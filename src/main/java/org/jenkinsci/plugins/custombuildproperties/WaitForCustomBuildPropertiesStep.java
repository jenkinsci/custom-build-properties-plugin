/*
 * The MIT License
 *
 * Copyright (c) 2019, Sebastian Hasait
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.custombuildproperties;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import jenkins.util.Timer;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class WaitForCustomBuildPropertiesStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(WaitForCustomBuildPropertiesStep.class.getName());

    private final List<String> keys;
    private final int timeoutTime;
    private TimeUnit timeoutUnit = TimeUnit.MINUTES;

    @DataBoundConstructor
    public WaitForCustomBuildPropertiesStep(List<String> keys, int timeoutTime) {
        super();

        this.keys = new ArrayList<>(keys);
        this.timeoutTime = timeoutTime;
    }

    public int getTimeoutTime() {
        return timeoutTime;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    @DataBoundSetter
    public void setTimeoutUnit(TimeUnit timeoutUnit) {
        this.timeoutUnit = timeoutUnit;
    }

    public List<String> getKeys() {
        return keys;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(this, context);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Wait until specified custom build properties are set";
        }

        @Override
        public String getFunctionName() {
            return "waitForCustomBuildProperties";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(Run.class);
        }

    }

    public static final class Execution extends StepExecution {

        private static final long serialVersionUID = 1L;

        private List<String> keys;
        private long timeoutTimeMillis;

        private boolean alreadyCompleted;

        private transient volatile CustomBuildPropertiesListener listener;
        private transient volatile ScheduledFuture<?> checkTask;
        private transient volatile ScheduledFuture<?> timeoutTask;

        public Execution(WaitForCustomBuildPropertiesStep step, StepContext context) {
            super(context);

            this.keys = step.getKeys();
            this.timeoutTimeMillis = step.getTimeoutUnit().toMillis(step.getTimeoutTime());
        }

        @Override
        public boolean start() {
            return init();
        }

        @Override
        public void onResume() {
            init();
        }

        @Override
        public void stop(@NonNull Throwable cause) {
            complete();
            getContext().onFailure(cause);
        }

        private boolean init() {
            if (!check()) {
                scheduleTimeout();
                installChangeEventListener();
                scheduleCheck();
                return false;
            }
            return true;
        }

        private void scheduleTimeout() {
            if (timeoutTimeMillis > 0) {
                timeoutTask = Timer.get().schedule(new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.log(Level.FINEST, "scheduleTimeout.run");
                        synchronized (this) {
                            if (alreadyCompleted) {
                                return;
                            }
                            getContext().onFailure(new RuntimeException("Timeout"));
                            complete();
                        }
                    }
                }, timeoutTimeMillis, TimeUnit.MILLISECONDS);
            }
        }

        private void installChangeEventListener() {
            Run relevantRun;
            try {
                relevantRun = getContext().get(Run.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            listener = new CustomBuildPropertiesListener() {
                @Override
                public void onCustomBuildPropertyChanged(Run run, String key, Object oldValue, Object newValue) {
                    if (run == relevantRun && keys.contains(key)) {
                        LOGGER.log(Level.FINEST, "onCustomBuildPropertyChanged");
                        check();
                    }
                }
            };
            CustomBuildPropertiesListener.all().add(listener);
        }

        private void scheduleCheck() {
            checkTask = Timer.get().schedule(new Runnable() {
                @Override
                public void run() {
                    LOGGER.log(Level.FINEST, "scheduleCheck.run");
                    if (!check()) {
                        scheduleCheck();
                    }
                }
            }, 10, TimeUnit.MINUTES);
        }

        private boolean check() {
            synchronized (this) {
                if (alreadyCompleted) {
                    return true;
                }
                boolean allCustomBuildPropertiesExist;
                try {
                    allCustomBuildPropertiesExist = allCustomBuildPropertiesExist();
                } catch (Exception e) {
                    getContext().onFailure(e);
                    complete();
                    return true;
                }
                if (allCustomBuildPropertiesExist) {
                    getContext().onSuccess(null);
                    complete();
                    return true;
                }
                return false;
            }
        }

        private void complete() {
            synchronized (this) {
                LOGGER.log(Level.FINEST, "complete: {0}", keys);
                if (!alreadyCompleted) {
                    alreadyCompleted = true;
                    LOGGER.log(Level.FINEST, "complete - alreadyCompleted = true");
                    if (listener != null) {
                        CustomBuildPropertiesListener.all().remove(listener);
                        LOGGER.log(Level.FINEST, "complete - removed listener");
                    }
                    if (checkTask != null) {
                        checkTask.cancel(false);
                        LOGGER.log(Level.FINEST, "complete - cancelled checkTask");
                    }
                    if (timeoutTask != null) {
                        timeoutTask.cancel(false);
                        LOGGER.log(Level.FINEST, "complete - cancelled timeoutTask");
                    }
                    LOGGER.log(Level.FINEST, "complete - done");
                } else {
                    LOGGER.log(Level.FINEST, "complete - alreadyCompleted: {0}", keys);
                }
            }
        }

        private boolean allCustomBuildPropertiesExist() throws Exception {
            if (keys.isEmpty()) {
                LOGGER.log(Level.FINEST, "customBuildPropertiesExists - keys empty");
                return true;
            }

            Run run = getContext().get(Run.class);

            final CustomBuildPropertiesAction action = run.getAction(CustomBuildPropertiesAction.class);
            if (action == null) {
                LOGGER.log(Level.FINEST, "customBuildPropertiesExists - no action yet: {0}", keys);
                return false;
            }

            for (String key : keys) {
                if (!action.containsProperty(key)) {
                    LOGGER.log(Level.FINEST, "customBuildPropertiesExists - key missing: {0} not in {1}", new Object[]{key, keys});
                    return false;
                }
            }

            LOGGER.log(Level.FINEST, "customBuildPropertiesExists - all keys found: {0}", keys);
            return true;
        }

    }

}

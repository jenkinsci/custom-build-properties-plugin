/*
 * The MIT License
 *
 * Copyright (c) 2017, Sebastian Hasait
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

import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public final class SetCustomBuildPropertyStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(SetCustomBuildPropertyStep.class.getName());

    protected static void runLogic(String key, Object value, boolean onlySetIfAbsent, Run<?, ?> run) throws Exception {
        synchronized (run) {
            final CustomBuildPropertiesAction action;
            final CustomBuildPropertiesAction actionMayBeNull = run.getAction(CustomBuildPropertiesAction.class);
            if (actionMayBeNull != null) {
                action = actionMayBeNull;
            } else {
                action = new CustomBuildPropertiesAction();
                run.addAction(action);
            }

            if (onlySetIfAbsent) {
                action.setPropertyIfAbsent(key, value);
            } else {
                action.setProperty(key, value);
            }

            run.save();
        }
    }

    private final String key;
    private final Object value;
    private boolean onlySetIfAbsent;

    @DataBoundConstructor
    public SetCustomBuildPropertyStep(String key, Object value) {
        super();

        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public boolean isOnlySetIfAbsent() {
        return onlySetIfAbsent;
    }

    @DataBoundSetter
    public void setOnlySetIfAbsent(final boolean onlySetIfAbsent) {
        this.onlySetIfAbsent = onlySetIfAbsent;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(this, context);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
            return "Set CustomBuildProperty";
        }

        @Override
        public String getFunctionName() {
            return "setCustomBuildProperty";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(Run.class);
        }

    }

    public static final class Execution extends SynchronousStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        private final String key;
        private final Object value;
        private final boolean onlySetIfAbsent;

        public Execution(SetCustomBuildPropertyStep step, StepContext context) {
            super(context);

            this.key = step.getKey();
            this.value = step.getValue();
            this.onlySetIfAbsent = step.isOnlySetIfAbsent();
        }

        @Override
        protected Void run() throws Exception {
            final Run run = getContext().get(Run.class);

            runLogic(key, value, onlySetIfAbsent, run);

            return null;
        }

    }

}

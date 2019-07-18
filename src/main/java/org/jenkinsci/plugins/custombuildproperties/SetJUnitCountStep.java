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
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 */
public final class SetJUnitCountStep extends Step {

    public static final String CBP_SUFFIX_PASSED_COUNT = "PassedCount";
    public static final String CBP_SUFFIX_FAILED_COUNT = "FailedCount";
    public static final String CBP_SUFFIX_FAILED_AGE = "FailedAge";

    private static final Logger LOGGER = Logger.getLogger(SetJUnitCountStep.class.getName());

    private final String keyPrefix;
    private final String include;
    private final String exclude;
    private boolean onlySetIfAbsent;

    @DataBoundConstructor
    public SetJUnitCountStep(String keyPrefix, String include, String exclude) {
        super();

        this.keyPrefix = keyPrefix;
        this.include = include;
        this.exclude = exclude;
    }

    public String getExclude() {
        return exclude;
    }

    public String getInclude() {
        return include;
    }

    public String getKeyPrefix() {
        return keyPrefix;
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
            return "Set junit test result counts as custom build properties";
        }

        @Override
        public String getFunctionName() {
            return "setJUnitCounts";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(Run.class);
        }

    }

    public static final class Execution extends SynchronousStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        private transient final SetJUnitCountStep step;

        public Execution(final SetJUnitCountStep step, final StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            final Run run = getContext().get(Run.class);

            final String keyPrefix = step.getKeyPrefix();
            final String include = step.getInclude();
            final String exclude = step.getExclude();
            final boolean onlySetIfAbsent = step.isOnlySetIfAbsent();

            final Pattern includePattern = include != null && include.trim().length() > 0 ? Pattern.compile(include) : null;
            final Pattern excludePattern = exclude != null && exclude.trim().length() > 0 ? Pattern.compile(exclude) : null;

            synchronized (run) {
                int[] passed = new int[]{
                        0,
                        0
                };
                int[] failed = new int[]{
                        0,
                        0
                };

                final TestResultAction testResultAction = run.getAction(TestResultAction.class);
                if (testResultAction != null) {
                    passed = count(testResultAction.getPassedTests(), includePattern, excludePattern);
                    failed = count(testResultAction.getFailedTests(), includePattern, excludePattern);
                }

                SetCustomBuildPropertyStep.runLogic(keyPrefix + CBP_SUFFIX_PASSED_COUNT, passed[0], onlySetIfAbsent, run);
                SetCustomBuildPropertyStep.runLogic(keyPrefix + CBP_SUFFIX_FAILED_COUNT, failed[0], onlySetIfAbsent, run);
                SetCustomBuildPropertyStep.runLogic(keyPrefix + CBP_SUFFIX_FAILED_AGE, failed[1], onlySetIfAbsent, run);
            }


            return null;
        }

        private int[] count(final List<CaseResult> caseResults, final Pattern includePattern, final Pattern excludePattern) {
            int count = 0;
            int age = 0;

            if (caseResults != null) {
                for (CaseResult caseResult : caseResults) {
                    if (caseResult == null) {
                        continue;
                    }

                    final String className = caseResult.getClassName();
                    if (className == null) {
                        continue;
                    }

                    if (includePattern != null && !includePattern.matcher(className).matches()) {
                        continue;
                    }

                    if (excludePattern != null && excludePattern.matcher(className).matches()) {
                        continue;
                    }

                    age = Math.max(age, caseResult.getAge());

                    count++;
                }
            }

            return new int[]{
                    count,
                    age
            };
        }

    }

}

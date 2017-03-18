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

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.google.inject.Inject;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

public class SetCustomBuildPropertyStepExecution extends AbstractStepExecutionImpl {
	private static final Logger LOGGER = Logger.getLogger(SetCustomBuildPropertyStepExecution.class.getName());

	// only used during the start() call, so no need to be persisted
	@Inject(optional = true)
	private transient SetCustomBuildPropertyStep step;

	@StepContextParameter
	private transient Run<?, ?> run;

	@Override
	public boolean start() throws Exception {
		final CustomBuildPropertiesAction action = ensureActionBoundToRun();
		if (step.isOnlySetIfAbsent()) {
			action.setPropertyIfAbsent(step.getKey(), step.getValue());
		} else {
			action.setProperty(step.getKey(), step.getValue());
		}
		getContext().onSuccess(null);

		return true;
	}

	@Override
	public void stop(@Nonnull final Throwable cause) throws Exception {
		// nop
	}

	private CustomBuildPropertiesAction ensureActionBoundToRun() {
		synchronized (this) {
			final CustomBuildPropertiesAction actionMayBeNull = run.getAction(CustomBuildPropertiesAction.class);
			if (actionMayBeNull != null) {
				return actionMayBeNull;
			} else {
				final CustomBuildPropertiesAction action = new CustomBuildPropertiesAction();
				run.addAction(action);
				return action;
			}
		}
	}

}

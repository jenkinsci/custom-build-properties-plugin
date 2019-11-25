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

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class CustomBuildPropertiesListener implements ExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(CustomBuildPropertiesListener.class.getName());

    /**
     * Return all the registered {@link CustomBuildPropertiesListener}s.
     */
    public static ExtensionList<CustomBuildPropertiesListener> all() {
        return ExtensionList.lookup(CustomBuildPropertiesListener.class);
    }

    /**
     * Notify listeners about change of CustomBuildProperty.
     */
    static void fireChanged(Run run, String key, Object oldValue, Object newValue) {
        for (CustomBuildPropertiesListener l : all()) {
            try {
                l.onCustomBuildPropertyChanged(run, key, oldValue, newValue);
            } catch (Throwable e) {
                report(e);
            }
        }
    }

    private static void report(Throwable e) {
        LOGGER.log(Level.WARNING, CustomBuildPropertiesListener.class.getSimpleName() + " failed", e);
    }

    public void onCustomBuildPropertyChanged(Run run, String key, Object oldValue, Object newValue) {
        // empty default implementation
    }

}

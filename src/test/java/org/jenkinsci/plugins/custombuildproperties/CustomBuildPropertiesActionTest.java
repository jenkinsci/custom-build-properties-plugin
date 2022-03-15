/*
 * The MIT License
 *
 * Copyright (c) 2022, Sebastian Hasait
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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CustomBuildPropertiesActionTest {

    private static final String SOME_KEY = "SomeKey";
    private static final Object SOME_VALUE = 42;
    private static final Object SOME_OTHER_VALUE = 21;

    private CustomBuildPropertiesAction testedAction;

    @Before
    public void setup() {
        testedAction = new CustomBuildPropertiesAction();
    }

    @Test
    public void test_constants() {
        assertNotNull(SOME_KEY);
        assertNotNull(SOME_VALUE);
        assertNotNull(SOME_OTHER_VALUE);
        assertNotEquals(SOME_VALUE, SOME_OTHER_VALUE);
    }

    @Test
    public void test_containsProperty_null() {
        assertFalse(testedAction.containsProperty(null));
    }

    @Test
    public void test_containsProperty_notExistingKey() {
        assertFalse(testedAction.containsProperty(SOME_KEY));
    }

    @Test
    public void test_containsProperty_existingKey() {
        testedAction.setProperty(SOME_KEY, SOME_VALUE);
        assertTrue(testedAction.containsProperty(SOME_KEY));
    }

    @Test
    public void test_getProperty_notExistingKey() {
        assertNull(testedAction.getProperty(SOME_KEY));
    }

    @Test
    public void test_setProperty_getProperty() {
        testedAction.setProperty(SOME_KEY, SOME_VALUE);
        assertEquals(SOME_VALUE, testedAction.getProperty(SOME_KEY));
    }

    @Test
    public void test_setPropertyIfAbsent_setInitialValue() {
        testedAction.setPropertyIfAbsent(SOME_KEY, SOME_VALUE);
        assertEquals(SOME_VALUE, testedAction.getProperty(SOME_KEY));
    }

    @Test
    public void test_setPropertyIfAbsent_notOverwritesAlreadySetValue() {
        testedAction.setProperty(SOME_KEY, SOME_VALUE);
        testedAction.setPropertyIfAbsent(SOME_KEY, SOME_OTHER_VALUE);
        assertEquals(SOME_VALUE, testedAction.getProperty(SOME_KEY));
    }

}

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomBuildPropertiesActionTest {

    private static final String SOME_KEY = "SomeKey";
    private static final Object SOME_VALUE = 42;
    private static final Object SOME_OTHER_VALUE = 21;

    private CustomBuildPropertiesAction testedAction;

    @BeforeEach
    void setup() {
        testedAction = new CustomBuildPropertiesAction();
    }

    @Test
    void test_constants() {
        assertNotNull(SOME_KEY);
        assertNotNull(SOME_VALUE);
        assertNotNull(SOME_OTHER_VALUE);
        assertNotEquals(SOME_VALUE, SOME_OTHER_VALUE);
    }

    @Test
    void test_containsProperty_null() {
        assertFalse(testedAction.containsProperty(null));
    }

    @Test
    void test_containsProperty_notExistingKey() {
        assertFalse(testedAction.containsProperty(SOME_KEY));
    }

    @Test
    void test_containsProperty_existingKey() {
        testedAction.setProperty(SOME_KEY, SOME_VALUE);
        assertTrue(testedAction.containsProperty(SOME_KEY));
    }

    @Test
    void test_getProperty_notExistingKey() {
        assertNull(testedAction.getProperty(SOME_KEY));
    }

    @Test
    void test_setProperty_getProperty() {
        testedAction.setProperty(SOME_KEY, SOME_VALUE);
        assertEquals(SOME_VALUE, testedAction.getProperty(SOME_KEY));
    }

    @Test
    void test_setPropertyIfAbsent_setInitialValue() {
        testedAction.setPropertyIfAbsent(SOME_KEY, SOME_VALUE);
        assertEquals(SOME_VALUE, testedAction.getProperty(SOME_KEY));
    }

    @Test
    void test_setPropertyIfAbsent_notOverwritesAlreadySetValue() {
        testedAction.setProperty(SOME_KEY, SOME_VALUE);
        testedAction.setPropertyIfAbsent(SOME_KEY, SOME_OTHER_VALUE);
        assertEquals(SOME_VALUE, testedAction.getProperty(SOME_KEY));
    }

    @Test
    void test_parseRemoteValue_true() {
        assertEquals(Boolean.TRUE, testedAction.parseRemoteValue("true", "java.lang.Boolean"));
    }

    @Test
    void test_parseRemoteValue_false() {
        assertEquals(Boolean.FALSE, testedAction.parseRemoteValue("false", "java.lang.Boolean"));
    }

    @Test
    void test_parseRemoteValue_yes() {
        assertEquals(Boolean.TRUE, testedAction.parseRemoteValue("yes", "java.lang.Boolean"));
    }

    @Test
    void test_parseRemoteValue_foo() {
        assertEquals(Boolean.FALSE, testedAction.parseRemoteValue("foo", "java.lang.Boolean"));
    }

    @Test
    void test_parseRemoteValue()throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
            assertEquals(Boolean.FALSE, testedAction.parseRemoteValue("foo", "not.existing.Type")));
    }

    @Test
    void test_parseRemoteValue_42() {
        assertEquals(42, testedAction.parseRemoteValue("42", "java.lang.Integer"));
    }

    @Test
    void test_parseRemoteValue_42L() {
        assertEquals(42L, testedAction.parseRemoteValue("42", "java.lang.Long"));
    }

    @Test
    void test_parseRemoteValue_date1() {
        TimeZone originalDefaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("CET"));
            assertEquals(new GregorianCalendar(2001, Calendar.OCTOBER, 26, 21, 32, 52).getTime(),
                    testedAction.parseRemoteValue("2001-10-26T21:32:52+02:00", "java.util.Date"));
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    @Test
    void test_parseRemoteValue_date2() {
        TimeZone originalDefaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("CET"));
            assertEquals(new GregorianCalendar(2001, Calendar.OCTOBER, 26, 18, 32, 52).getTime(),
                    testedAction.parseRemoteValue("2001-10-26T21:32:52+05:00", "java.util.Date"));
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    @Test
    void test_parseRemoteValue_date3() {
        TimeZone originalDefaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("CET"));
            assertEquals(new GregorianCalendar(2001, Calendar.OCTOBER, 26, 23, 32, 52).getTime(),
                    testedAction.parseRemoteValue("2001-10-26T21:32:52Z", "java.util.Date"));
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    @Test
    void test_parseRemoteValue_date4() {
        TimeZone originalDefaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("CET"));
            assertEquals(new GregorianCalendar(2001, Calendar.OCTOBER, 26, 21, 32, 52).getTime(),
                    testedAction.parseRemoteValue("2001-10-26T21:32:52", "java.util.Date"));
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

}

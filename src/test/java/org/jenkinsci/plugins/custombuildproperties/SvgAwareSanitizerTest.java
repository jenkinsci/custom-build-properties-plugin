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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SvgAwareSanitizerTest {

    @Test
    void test_01_svg() {
        test("01_svg");
    }

    @Test
    void test_02_script() {
        test("02_script");
    }

    @Test
    void test_03_onmouseover() {
        test("03_onmouseover");
    }

    @Test
    void test_04_img() {
        test("04_img");
    }

    private void test(String name) {
        String raw = readResource(getClass(), "/" + name + "-raw.txt");
        String exp = readResource(getClass(), "/" + name + "-san.txt");
        String san = SvgAwareSanitizer.sanitize(raw);
        assertEquals(exp, san);
    }

    private String readResource(Class baseClass, String suffix) {
        try {
            ByteArrayOutputStream contentBaos = new ByteArrayOutputStream();
            String resourcePath = "/" + baseClass.getName().replace('.', '/') + suffix;
            InputStream contentIn = baseClass.getResourceAsStream(resourcePath);
            if (contentIn != null) {
                byte[] buffer = new byte[1024];
                int n;
                while (-1 != (n = contentIn.read(buffer))) {
                    contentBaos.write(buffer, 0, n);
                }
                contentIn.close();
            }
            return contentBaos.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

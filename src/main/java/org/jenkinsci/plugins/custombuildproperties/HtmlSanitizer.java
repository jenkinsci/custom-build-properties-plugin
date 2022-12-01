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

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlSanitizer {

    private static final Pattern EMPTY_ELEMENT_SYNTAX = Pattern.compile("<(polygon|path)(\\s[^/>]+)/>");

    private static final PolicyFactory POLICY;

    static {
        PolicyFactory limitedSvg = new HtmlPolicyBuilder() //
                .allowElements("svg", "title", "g", "polygon", "path", "text") //
                .allowTextIn("title", "text") //
                .allowAttributes("width", "height", "viewBox", "xmlns").onElements("svg") //
                .allowAttributes("id", "class", "transform").onElements("g") //
                .allowAttributes("fill", "stroke", "stroke-dasharray").onElements("polygon", "path", "text") //
                .allowAttributes("x", "y").onElements("text") //
                .allowAttributes("points").onElements("polygon") //
                .allowAttributes("d").onElements("path") //
                .allowAttributes("text-anchor", "font-family", "font-size").onElements("text") //
                .requireRelNofollowOnLinks() //
                .toFactory();

        POLICY = Sanitizers.FORMATTING.and(limitedSvg);
    }

    public static String sanitize(String content) {
        // Workaround BEGIN for https://github.com/OWASP/java-html-sanitizer/issues/122
        StringBuffer sb = new StringBuffer();
        Matcher contentMatcher = EMPTY_ELEMENT_SYNTAX.matcher(content);
        while (contentMatcher.find()) {
            contentMatcher.appendReplacement(sb, "<$1$2></$1>");
        }
        contentMatcher.appendTail(sb);
        content = sb.toString();
        // Workaround END
        return POLICY.sanitize(content);
    }

}

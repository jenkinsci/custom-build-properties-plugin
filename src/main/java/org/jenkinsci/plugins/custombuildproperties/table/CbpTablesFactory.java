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

package org.jenkinsci.plugins.custombuildproperties.table;

import hudson.markup.MarkupFormatter;
import org.jenkinsci.plugins.custombuildproperties.CustomBuildPropertiesAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
public class CbpTablesFactory {

    private final Map<String, Object> properties;
    private final MarkupFormatter defaultMarkupFormatter;
    private final Set<String> internalSanitizer = new HashSet<>();
    private final List<CbpTable> tables = new ArrayList<>();

    public CbpTablesFactory(Map<String, Object> properties, MarkupFormatter defaultMarkupFormatter) {
        this.properties = properties;
        this.defaultMarkupFormatter = defaultMarkupFormatter;
    }

    public List<CbpTable> createTables() {
        createTablesAndPopulateInternalSanitizer();
        putTableValues();
        return tables;
    }

    private void createTablesAndPopulateInternalSanitizer() {
        Iterator<Map.Entry<String, Object>> propertiesI = properties.entrySet().iterator();
        while (propertiesI.hasNext()) {
            Map.Entry<String, Object> property = propertiesI.next();
            String key = property.getKey();
            Object value = property.getValue();
            if (key != null) {
                if (value instanceof String) {
                    if (key.startsWith(CustomBuildPropertiesAction.CBP_TABLE_PREFIX)) {
                        String name = key.substring(CustomBuildPropertiesAction.CBP_TABLE_PREFIX.length());
                        Pattern pattern;
                        try {
                            pattern = Pattern.compile((String) value);
                        } catch (PatternSyntaxException e) {
                            pattern = null;
                        }
                        if (pattern != null) {
                            CbpTable table = new CbpTable(name, pattern, defaultMarkupFormatter);
                            tables.add(table);
                            propertiesI.remove();
                        }
                    } else if (key.startsWith(CustomBuildPropertiesAction.CBP_SANITIZER_PREFIX)) {
                        if (CustomBuildPropertiesAction.CBP_INTERNAL_SANITIZER.equals(value)) {
                            internalSanitizer.add(key.substring(CustomBuildPropertiesAction.CBP_SANITIZER_PREFIX.length()));
                            propertiesI.remove();
                        }
                    }
                }
            }
        }
    }

    private void putTableValues() {
        Iterator<Map.Entry<String, Object>> propertiesI = properties.entrySet().iterator();
        while (propertiesI.hasNext()) {
            Map.Entry<String, Object> property = propertiesI.next();
            String key = property.getKey();
            Object value = property.getValue();
            if (key != null) {
                for (CbpTable table : tables) {
                    Matcher matcher = table.getPattern().matcher(key);
                    if (matcher.matches() && matcher.groupCount() >= 2) {
                        String rowName = matcher.group(1);
                        String columnName = matcher.group(2);
                        putTableValue(table, rowName, columnName, value);
                        propertiesI.remove();
                    }
                }
            }
        }

        if (!properties.isEmpty()) {
            CbpTable table = new CbpTable("Key", null, defaultMarkupFormatter);
            tables.add(table);

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                putTableValue(table, entry.getKey(), "Value", entry.getValue());
            }
        }
    }

    private void putTableValue(CbpTable table, String rowName, String columnName, Object value) {
        boolean sanitizeInternal = internalSanitizer.contains(table.getName() + '_' + rowName + '_' + columnName);
        table.putValue(rowName, columnName, value, sanitizeInternal);
    }

}

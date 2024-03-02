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

package org.jenkinsci.plugins.custombuildproperties.table;

import hudson.markup.MarkupFormatter;
import org.apache.commons.lang.time.FastDateFormat;
import org.jenkinsci.plugins.custombuildproperties.SvgAwareSanitizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 */
public class CbpTable {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss EEE");

    private final MarkupFormatter defaultMarkupFormatter;
    private final Pattern pattern;
    private final String name;
    private final String title;

    private final List<CbpTableColumn> columns = new ArrayList<>();
    private final Map<String, CbpTableColumn> columnsByName = new HashMap<>();
    private final List<CbpTableRow> rows = new ArrayList<>();
    private final Map<String, CbpTableRow> rowsByName = new HashMap<>();

    public CbpTable(String name, Pattern pattern, MarkupFormatter defaultMarkupFormatter) {
        this.defaultMarkupFormatter = defaultMarkupFormatter;
        this.pattern = pattern;
        this.name = name;

        this.title = sanitize(name, false);
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public List<CbpTableColumn> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public List<CbpTableRow> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public void putValue(String rowName, String columnName, Object value, boolean sanitizeInternal) {
        CbpTableColumn column = getOrCreateColumn(columnName);
        CbpTableRow row = getOrCreateRow(rowName);
        CbpTableCell cell = row.getOrCreateCell(column);
        cell.setValue(sanitize(rawFormat(value), sanitizeInternal));
    }

    private CbpTableColumn getOrCreateColumn(String columnName) {
        return columnsByName.computeIfAbsent(columnName, notUsed -> {
            CbpTableColumn column = new CbpTableColumn();
            column.setTitle(sanitize(columnName, false));
            columns.add(column);
            return column;
        });
    }

    private CbpTableRow getOrCreateRow(String rowName) {
        return rowsByName.computeIfAbsent(rowName, notUsed -> {
            CbpTableRow row = new CbpTableRow();
            row.setTitle(sanitize(rowName, false));
            rows.add(row);
            return row;
        });
    }

    String sanitize(String content, boolean internal) {
        if (internal) {
            return SvgAwareSanitizer.sanitize(content);
        }
        try {
            return defaultMarkupFormatter.translate(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String rawFormat(Object value) {
        if (value instanceof Date || value instanceof Calendar) {
            return DATE_FORMAT.format(value);
        }
        return value == null ? "" : value.toString();
    }

}

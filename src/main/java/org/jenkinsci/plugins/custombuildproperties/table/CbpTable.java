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

import org.apache.commons.lang.time.FastDateFormat;

import java.util.*;
import java.util.regex.Pattern;

/**
 *
 */
public class CbpTable {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss EEE");

    private final Map<String, Map<String, Object>> rawData = new TreeMap<>();
    private final Set<String> rawColumns = new TreeSet<>();

    private Pattern pattern;
    private String title;

    private List<CbpTableHeader> headers = new ArrayList<>();

    private List<CbpTableRow> rows = new ArrayList<>();

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CbpTableHeader createHeader() {
        CbpTableHeader header = new CbpTableHeader();
        headers.add(header);
        return header;
    }

    public CbpTableRow createRow() {
        CbpTableRow row = new CbpTableRow();
        rows.add(row);
        return row;
    }

    public List<CbpTableHeader> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public List<CbpTableRow> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public void addRawData(String rowName, String columnName, Object value) {
        if (!rawData.containsKey(rowName)) {
            rawData.put(rowName, new HashMap<String, Object>());
        }
        rawData.get(rowName).put(columnName, value);
        rawColumns.add(columnName);
    }

    public void processRaw() {
        pattern = null;

        for (String columnName : rawColumns) {
            CbpTableHeader header = createHeader();
            header.setTitle(columnName);
        }

        for (Map.Entry<String, Map<String, Object>> rawE : rawData.entrySet()) {
            String rowName = rawE.getKey();
            Map<String, Object> rawCells = rawE.getValue();
            CbpTableRow row = createRow();
            row.setTitle(rowName);
            for (String columnName : rawColumns) {
                CbpTableCell cell = row.createCell();
                cell.setValue(rawFormat(rawCells.get(columnName)));
            }
        }

        rawData.clear();
        rawColumns.clear();
    }

    private String rawFormat(Object value) {
        if (value instanceof Date || value instanceof Calendar) {
            return DATE_FORMAT.format(value);
        }
        return value == null ? "" : value.toString();
    }

}

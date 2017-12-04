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

import hudson.model.Action;
import hudson.model.Api;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.custombuildproperties.table.CbpTable;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ExportedBean
public class CustomBuildPropertiesAction implements Action {

    public static final String CBP_TABLE_PREFIX = "_cbp_table_";

    private final Map<String, Object> properties = new HashMap<>();

    public CustomBuildPropertiesAction() {
        super();
    }

    @Exported(visibility = 2)
    public Map<String, Object> getProperties() {
        synchronized (properties) {
            return new HashMap<>(properties);
        }
    }

    public boolean containsProperty(String key) {
        synchronized (properties) {
            return properties.containsKey(key);
        }
    }

    public Object getProperty(String key) {
        synchronized (properties) {
            return properties.get(key);
        }
    }

    public void setProperty(String key, Object value) {
        synchronized (properties) {
            properties.put(key, value);
        }
    }

    public void setPropertyIfAbsent(String key, Object value) {
        synchronized (properties) {
            if (!properties.containsKey(key)) {
                properties.put(key, value);
            }
        }
    }

    public List<CbpTable> getViewTables() {
        Map<String, Object> sortedProperties = new TreeMap<>();
        synchronized (properties) {
            sortedProperties.putAll(properties);
        }

        List<CbpTable> tables = createTables(sortedProperties);
        fillViewTables(sortedProperties, tables);

        return tables;
    }

    private List<CbpTable> createTables(Map<String, Object> workProperties) {
        List<CbpTable> result = new ArrayList<>();

        Iterator<Map.Entry<String, Object>> propertiesI = workProperties.entrySet().iterator();
        while (propertiesI.hasNext()) {
            Map.Entry<String, Object> property = propertiesI.next();
            String key = property.getKey();
            Object value = property.getValue();
            if (key != null && value instanceof String && key.startsWith(CBP_TABLE_PREFIX)) {
                String title = key.substring(CBP_TABLE_PREFIX.length());
                Pattern pattern;
                try {
                    pattern = Pattern.compile((String) value);
                } catch (PatternSyntaxException e) {
                    pattern = null;
                }
                if (pattern != null) {
                    CbpTable table = new CbpTable();
                    table.setTitle(title);
                    table.setPattern(pattern);
                    result.add(table);
                    propertiesI.remove();
                }
            }
        }
        return result;
    }

    private void fillViewTables(Map<String, Object> workProperties, List<CbpTable> tables) {

        Iterator<Map.Entry<String, Object>> propertiesI = workProperties.entrySet().iterator();
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
                        table.addRawData(rowName, columnName, value);
                        propertiesI.remove();
                    }
                }
            }
        }

        if (!workProperties.isEmpty()) {
            CbpTable table = new CbpTable();
            table.setTitle("Key");
            tables.add(table);

            for (Map.Entry<String, Object> entry : workProperties.entrySet()) {
                table.addRawData(entry.getKey(), "Value", entry.getValue());
            }
        }

        for (CbpTable table : tables) {
            table.processRaw();
        }

    }

    /**
     * Exposes this object to the remote API.
     */
    public Api getApi() {
        return new Api(this);
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "Custom Build Properties";
    }

    @Override
    public String getUrlName() {
        return "custombuildproperties";
    }


    public void doGet(StaplerRequest req, StaplerResponse rsp,
                      @QueryParameter(required = true) String key) throws IOException, ServletException {
        setHeaders(rsp);
        rsp.setContentType("text/plain");
        Object value = properties.get(key);
        rsp.getWriter().print(value);
        rsp.getWriter().close();
    }

    public void doSet(StaplerRequest req, StaplerResponse rsp,
                      @QueryParameter(required = true) String key, @QueryParameter(required = true) String value, @QueryParameter String valueType) throws Exception {
        Object newValue;
        if (valueType != null) {
            Class<?> valueClass = Thread.currentThread().getContextClassLoader().loadClass(valueType);
            newValue = valueClass.getConstructor(String.class).newInstance(value);
        } else {
            newValue = value;
        }

        Object oldValue;
        Run run = req.findAncestorObject(Run.class);
        synchronized (run) {
            oldValue = properties.put(key, newValue);
            run.save();
        }

        setHeaders(rsp);
        rsp.setContentType("text/plain");
        rsp.getWriter().print(oldValue);
        rsp.getWriter().close();
    }

    private void setHeaders(StaplerResponse rsp) {
        rsp.setHeader("X-Jenkins", Jenkins.VERSION);
        rsp.setHeader("X-Jenkins-Session", Jenkins.SESSION_HASH);
    }

}

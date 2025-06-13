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

import hudson.model.Api;
import hudson.model.Item;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import net.sf.json.JSONObject;
import org.apache.commons.lang.BooleanUtils;
import org.jenkinsci.plugins.custombuildproperties.table.CbpTable;
import org.jenkinsci.plugins.custombuildproperties.table.CbpTablesFactory;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@ExportedBean
public class CustomBuildPropertiesAction implements RunAction2 {

    public static final String CBP_TABLE_PREFIX = "_cbp_table_";
    public static final String CBP_SANITIZER_PREFIX = "_cbp_sanitizer_";
    public static final String CBP_INTERNAL_SANITIZER = "internal";

    private static final Map<String, Function<String, ?>> SUPPORTED_REMOTE_TYPES;

    private static <T> void addRemoteType(Map<String, Function<String, ?>> map, Class<T> clazz, Function<String, T> parser) {
        map.put(clazz.getName(), parser);
    }

    static {
        Map<String, Function<String, ?>> map = new LinkedHashMap<>();
        addRemoteType(map, String.class, string -> string);
        addRemoteType(map, Boolean.class, BooleanUtils::toBoolean);
        // numbers
        addRemoteType(map, Byte.class, Byte::parseByte);
        addRemoteType(map, Short.class, Short::parseShort);
        addRemoteType(map, Integer.class, Integer::parseInt);
        addRemoteType(map, Long.class, Long::parseLong);
        addRemoteType(map, Float.class, Float::parseFloat);
        addRemoteType(map, Double.class, Double::parseDouble);
        addRemoteType(map, BigInteger.class, BigInteger::new);
        addRemoteType(map, BigDecimal.class, BigDecimal::new);
        // dates and times via ISO-8601 format
        addRemoteType(map, Date.class, string -> {
            TemporalAccessor parseBestResult = DateTimeFormatter.ISO_DATE_TIME.parseBest(string, ZonedDateTime::from, OffsetDateTime::from, LocalDateTime::from);
            Instant instant;
            if (parseBestResult instanceof ZonedDateTime) {
                instant = ((ZonedDateTime) parseBestResult).toInstant();
            } else if (parseBestResult instanceof OffsetDateTime) {
                instant = ((OffsetDateTime) parseBestResult).toInstant();
            } else if (parseBestResult instanceof LocalDateTime) {
                instant = ((LocalDateTime) parseBestResult).atZone(ZoneId.systemDefault()).toInstant();
            } else {
                throw new RuntimeException("Unexpected parseBestResult=" + parseBestResult + " for " + string);
            }
            return Date.from(instant);
        });
        addRemoteType(map, LocalTime.class, LocalTime::parse);
        addRemoteType(map, LocalDate.class, LocalDate::parse);
        addRemoteType(map, LocalDateTime.class, LocalDateTime::parse);
        addRemoteType(map, Instant.class, Instant::parse);
        addRemoteType(map, OffsetDateTime.class, OffsetDateTime::parse);
        addRemoteType(map, ZonedDateTime.class, ZonedDateTime::parse);
        SUPPORTED_REMOTE_TYPES = Collections.unmodifiableMap(map);
    }

    private final Map<String, Object> properties = new HashMap<>();

    private transient Run<?, ?> run;

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

    public Object setProperty(String key, Object newValue) {
        return setPropertyInternal(key, newValue, false, true);
    }

    public Object setPropertyIfAbsent(String key, Object newValue) {
        return setPropertyInternal(key, newValue, true, true);
    }

    public List<CbpTable> getViewTables() {
        Map<String, Object> clonedProperties;
        synchronized (properties) {
            clonedProperties = new TreeMap<>(properties);
        }

        return new CbpTablesFactory(clonedProperties, Jenkins.get().getMarkupFormatter()).createTables();
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
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

    public void doGet(StaplerRequest2 req, StaplerResponse2 rsp,
                      @QueryParameter(required = true) String key) throws IOException, ServletException {
        run.checkPermission(Item.READ);

        Object value = getProperty(key);
        writeValue(rsp, value);
    }

    @RequirePOST
    public void doSet(StaplerRequest2 req, StaplerResponse2 rsp) throws Exception {
        run.checkPermission(Run.UPDATE);

        JSONObject submittedForm = req.getSubmittedForm();
        String key = submittedForm.getString("key");
        String value = submittedForm.getString("value");
        String valueType = submittedForm.optString("valueType", null);

        Object newValue = parseRemoteValue(value, valueType);

        Object oldValue;
        synchronized (run) {
            oldValue = setPropertyInternal(key, newValue, false, false);
            run.save();
        }
        CustomBuildPropertiesListener.fireChanged(run, key, oldValue, newValue);

        writeValue(rsp, oldValue);
    }

    /**
     * @deprecated Use {@link #doSet(StaplerRequest2, StaplerResponse2)} instead.
     */
    @Deprecated
    @RequirePOST
    public void doSetPost(StaplerRequest2 req, StaplerResponse2 rsp) throws Exception {
        // Permission check delegated to doSet
        doSet(req, rsp);
    }

    /**
     * Only visible for testing.
     */
    Object parseRemoteValue(String value, String valueType) {
        if (valueType == null) {
            return value;
        }

        Function<String, ?> parser = SUPPORTED_REMOTE_TYPES.get(valueType);
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported valueType: " + valueType);
        }

        return parser.apply(value);
    }

    private void writeValue(StaplerResponse2 rsp, Object value) throws IOException {
        setHeaders(rsp);
        rsp.setContentType("text/plain;charset=UTF-8");
        rsp.getWriter().print(value);
        rsp.getWriter().close();
    }

    private void setHeaders(StaplerResponse2 rsp) {
        rsp.setHeader("X-Jenkins", Jenkins.VERSION);
        rsp.setHeader("X-Jenkins-Session", Jenkins.SESSION_HASH);
    }

    private Object setPropertyInternal(String key, Object newValue, boolean onlyIfAbsent, boolean fireEvent) {
        Object oldValue;
        synchronized (properties) {
            if (onlyIfAbsent && properties.containsKey(key)) {
                return null;
            }
            oldValue = properties.put(key, newValue);
        }
        if (fireEvent) {
            CustomBuildPropertiesListener.fireChanged(run, key, oldValue, newValue);
        }
        return oldValue;
    }

}

package com.spinny.framework;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-test details consumed by the Excel execution report. */
public final class ScenarioReportData {
    // ThreadLocal prevents report values from leaking between parallel TestNG invocations.
    private static final ThreadLocal<Map<String, String>> DATA =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private ScenarioReportData() {}

    /** Start with empty metadata before a test invocation. */
    public static void clear() { DATA.get().clear(); }

    public static void put(String key, Object value) {
        if (value != null) DATA.get().put(key, String.valueOf(value));
    }

    /** Return a copy so later updates do not change a listener's captured result. */
    public static Map<String, String> snapshot() { return new LinkedHashMap<>(DATA.get()); }
}

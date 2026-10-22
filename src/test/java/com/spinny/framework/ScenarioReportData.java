package com.spinny.framework;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-test details consumed by the Excel execution report. */
public final class ScenarioReportData {
    private static final ThreadLocal<Map<String, String>> DATA =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private ScenarioReportData() {}

    public static void clear() { DATA.get().clear(); }

    public static void put(String key, Object value) {
        if (value != null) DATA.get().put(key, String.valueOf(value));
    }

    public static Map<String, String> snapshot() { return new LinkedHashMap<>(DATA.get()); }
}

package com.aacreatecompat;

import java.util.concurrent.ConcurrentHashMap;

/** Rate-limited diagnostic logging. On when config debug_log=true or the debug jar's marker file is present. */
public final class Debug {
    private static final boolean FORCED = Debug.class.getResource("/aacreatecompat_debug.flag") != null;
    private static final ConcurrentHashMap<String, Long> LAST = new ConcurrentHashMap<>();

    public static boolean on() {
        if (FORCED) return true;
        try { return CompatConfig.DEBUG.get(); } catch (Throwable t) { return false; }
    }

    /** Logs at most once per 2 s for the same key. */
    public static void log(String key, String msg, Object... args) {
        if (!on()) return;
        long now = System.currentTimeMillis();
        Long prev = LAST.get(key);
        if (prev != null && now - prev < 2000) return;
        LAST.put(key, now);
        AACreateCompat.LOGGER.info("[aacc-debug] " + msg, args);
    }

    public static boolean forced() { return FORCED; }
}

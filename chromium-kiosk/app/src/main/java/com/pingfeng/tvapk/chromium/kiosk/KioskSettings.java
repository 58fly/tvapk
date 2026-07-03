package com.pingfeng.tvapk.chromium.kiosk;

import java.util.Properties;

public final class KioskSettings {
    public static final String DEFAULT_TARGET_URL = "http://47.94.161.17:3000/wl.html";
    public static final int DEFAULT_ZOOM_PERCENT = 100;
    public static final int MIN_ZOOM_PERCENT = 10;
    public static final int MAX_ZOOM_PERCENT = 100;
    public static final int ZOOM_STEP_PERCENT = 10;

    public final String targetUrl;
    public final int zoomPercent;
    public final String versionName;
    public final int versionCode;
    public final long updatedAt;

    public KioskSettings(
            String targetUrl,
            int zoomPercent,
            String versionName,
            int versionCode,
            long updatedAt
    ) {
        this.targetUrl = normalizeUrl(targetUrl);
        this.zoomPercent = normalizeZoomPercent(zoomPercent);
        this.versionName = versionName == null ? "" : versionName;
        this.versionCode = versionCode;
        this.updatedAt = updatedAt;
    }

    public static KioskSettings defaults(String versionName, int versionCode) {
        return new KioskSettings(DEFAULT_TARGET_URL, DEFAULT_ZOOM_PERCENT, versionName, versionCode, 0L);
    }

    public KioskSettings withUrl(String targetUrl) {
        return new KioskSettings(targetUrl, zoomPercent, versionName, versionCode, updatedAt);
    }

    public KioskSettings withZoomPercent(int zoomPercent) {
        return new KioskSettings(targetUrl, zoomPercent, versionName, versionCode, updatedAt);
    }

    public KioskSettings withUpdatedAt(long updatedAt) {
        return new KioskSettings(targetUrl, zoomPercent, versionName, versionCode, updatedAt);
    }

    public float getScale() {
        return zoomPercent / 100f;
    }

    public String getVersionLabel() {
        return "v" + versionName + " / build " + versionCode;
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("targetUrl", targetUrl);
        properties.setProperty("zoomPercent", String.valueOf(zoomPercent));
        properties.setProperty("versionName", versionName);
        properties.setProperty("versionCode", String.valueOf(versionCode));
        properties.setProperty("updatedAt", String.valueOf(updatedAt));
        return properties;
    }

    public static int normalizeZoomPercent(int value) {
        int rounded = Math.round(value / (float) ZOOM_STEP_PERCENT) * ZOOM_STEP_PERCENT;
        if (rounded < MIN_ZOOM_PERCENT) return MIN_ZOOM_PERCENT;
        if (rounded > MAX_ZOOM_PERCENT) return MAX_ZOOM_PERCENT;
        return rounded;
    }

    private static String normalizeUrl(String value) {
        if (value == null || value.trim().isEmpty()) return DEFAULT_TARGET_URL;
        return value.trim();
    }
}

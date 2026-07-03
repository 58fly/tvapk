package com.pingfeng.tvapk.chromium.kiosk;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class KioskSettingsStore {
    public static final String FILE_NAME = "kiosk_settings.properties";

    private static final String TAG = "KioskSettingsStore";
    private final File file;
    private final KioskSettings defaults;

    public KioskSettingsStore(Context context, KioskSettings defaults) {
        this(new File(context.getFilesDir(), FILE_NAME), defaults);
    }

    public KioskSettingsStore(File file, KioskSettings defaults) {
        this.file = file;
        this.defaults = defaults;
    }

    public synchronized void save(KioskSettings settings) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create settings directory: " + parent.getAbsolutePath());
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            settings.toProperties().store(output, "TV kiosk settings");
        }
    }

    public synchronized KioskSettings load() {
        if (!file.exists()) return defaults;

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException e) {
            Log.w(TAG, "Failed to read kiosk settings file", e);
            return defaults;
        }

        return new KioskSettings(
                readString(properties, "targetUrl", defaults.targetUrl),
                readInt(properties, "zoomPercent", defaults.zoomPercent, true),
                readString(properties, "versionName", defaults.versionName),
                readInt(properties, "versionCode", defaults.versionCode, false),
                readLong(properties, "updatedAt", defaults.updatedAt)
        );
    }

    private static String readString(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private static int readInt(Properties properties, String key, int fallback, boolean rejectOutOfRange) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)).trim());
            if (rejectOutOfRange
                    && (value < KioskSettings.MIN_ZOOM_PERCENT || value > KioskSettings.MAX_ZOOM_PERCENT)) {
                return fallback;
            }
            return value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long readLong(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

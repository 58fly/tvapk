package com.threethan.browser.kiosk;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class KioskSettingsStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void saveAndLoadRoundTripsUrlAndZoom() throws Exception {
        File file = temporaryFolder.newFile("kiosk_settings.properties");
        KioskSettingsStore store = new KioskSettingsStore(file, KioskSettings.defaults("1.4.0", 1400));
        KioskSettings settings = new KioskSettings("http://example.com", 60, "1.3.2", 1302, 123L);

        store.save(settings);
        KioskSettings loaded = store.load();

        assertEquals("http://example.com", loaded.targetUrl);
        assertEquals(60, loaded.zoomPercent);
        assertEquals("1.3.2", loaded.versionName);
        assertEquals(1302, loaded.versionCode);
        assertEquals(123L, loaded.updatedAt);
    }

    @Test
    public void invalidZoomFallsBackToDefault() throws Exception {
        File file = writeProperties("targetUrl=http://example.com\nzoomPercent=125\n");
        KioskSettingsStore store = new KioskSettingsStore(file, KioskSettings.defaults("1.4.0", 1400));

        KioskSettings loaded = store.load();

        assertEquals("http://example.com", loaded.targetUrl);
        assertEquals(100, loaded.zoomPercent);
    }

    @Test
    public void missingFileLoadsDefaultSettings() throws Exception {
        File file = new File(temporaryFolder.getRoot(), "missing.properties");
        KioskSettingsStore store = new KioskSettingsStore(file, KioskSettings.defaults("1.4.0", 1400));

        KioskSettings loaded = store.load();

        assertEquals(KioskSettings.DEFAULT_TARGET_URL, loaded.targetUrl);
        assertEquals(100, loaded.zoomPercent);
        assertEquals("1.4.0", loaded.versionName);
        assertEquals(1400, loaded.versionCode);
    }

    private File writeProperties(String content) throws IOException {
        File file = temporaryFolder.newFile("kiosk_settings.properties");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}

package com.threethan.browser.kiosk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KioskSettingsControllerTest {
    @Test
    public void cancelRestoresBaselineAndReportsPreviewSettings() {
        KioskSettings baseline = new KioskSettings("http://old.example", 100, "1.4.0", 1400, 0L);
        KioskSettingsController controller = new KioskSettingsController(baseline);

        controller.beginEdit();
        controller.setDraftUrl("http://new.example");
        controller.setDraftZoomPercent(60);
        KioskSettings preview = controller.getPreviewSettings();

        assertEquals("http://new.example", preview.targetUrl);
        assertEquals(60, preview.zoomPercent);

        KioskSettings canceled = controller.cancel();

        assertEquals("http://old.example", canceled.targetUrl);
        assertEquals(100, canceled.zoomPercent);
        assertEquals("http://old.example", controller.getActiveSettings().targetUrl);
        assertEquals(100, controller.getActiveSettings().zoomPercent);
    }

    @Test
    public void commitPromotesDraftToActiveSettings() {
        KioskSettings baseline = new KioskSettings("http://old.example", 100, "1.4.0", 1400, 0L);
        KioskSettingsController controller = new KioskSettingsController(baseline);

        controller.beginEdit();
        controller.setDraftUrl("http://new.example");
        controller.setDraftZoomPercent(55);
        KioskSettings committed = controller.commit(456L);

        assertEquals("http://new.example", committed.targetUrl);
        assertEquals(60, committed.zoomPercent);
        assertEquals(456L, committed.updatedAt);
        assertEquals("http://new.example", controller.getActiveSettings().targetUrl);
        assertEquals(60, controller.getActiveSettings().zoomPercent);
    }
}

package com.threethan.browser.browser.GeckoView;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSessionSettings;

public class KioskGeckoSettingsFactoryTest {
    @Test
    public void runtimeSettingsEnableDownloadedWebFontsForIconFonts() {
        GeckoRuntimeSettings settings = KioskGeckoSettingsFactory.createRuntimeSettings();

        assertTrue(settings.getWebFontsEnabled());
    }

    @Test
    public void sessionSettingsKeepBusinessPageFontResourcesUnblocked() {
        GeckoSessionSettings settings = KioskGeckoSettingsFactory.createSessionSettings();

        assertFalse(settings.getUseTrackingProtection());
    }
}

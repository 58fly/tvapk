package com.threethan.browser.browser.GeckoView;

import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSessionSettings;

public final class KioskGeckoSettingsFactory {
    private KioskGeckoSettingsFactory() {
    }

    public static GeckoRuntimeSettings createRuntimeSettings() {
        return new GeckoRuntimeSettings.Builder()
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .consoleOutput(false)
                .loginAutofillEnabled(false)
                .extensionsProcessEnabled(true)
                .extensionsWebAPIEnabled(true)
                .webFontsEnabled(true)
                .glMsaaLevel(0)
                .debugLogging(false)
                .aboutConfigEnabled(true)
                .build();
    }

    public static GeckoSessionSettings createSessionSettings() {
        GeckoSessionSettings settings = new GeckoSessionSettings.Builder().build();
        settings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
        settings.setUseTrackingProtection(false);
        return settings;
    }
}

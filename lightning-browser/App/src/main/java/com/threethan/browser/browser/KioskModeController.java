package com.threethan.browser.browser;

public class KioskModeController {
    public boolean shouldForceHideTopBar(boolean isKiosk, boolean isTab) {
        return isKiosk || !isTab;
    }
}

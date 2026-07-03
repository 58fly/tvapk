package com.threethan.browser.browser;

public final class KioskKeyActionController {
    public enum Action {
        NO_OP,
        SHOW_SETTINGS,
        CLOSE_SETTINGS
    }

    public Action onBack(boolean settingsVisible) {
        return settingsVisible ? Action.CLOSE_SETTINGS : Action.SHOW_SETTINGS;
    }

    public Action onMenu(boolean settingsVisible) {
        return settingsVisible ? Action.CLOSE_SETTINGS : Action.SHOW_SETTINGS;
    }
}

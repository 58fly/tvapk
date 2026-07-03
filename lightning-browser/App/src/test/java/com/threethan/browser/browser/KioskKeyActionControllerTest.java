package com.threethan.browser.browser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KioskKeyActionControllerTest {
    @Test
    public void backOpensSettingsWhenOverlayHidden() {
        KioskKeyActionController controller = new KioskKeyActionController();

        assertEquals(KioskKeyActionController.Action.SHOW_SETTINGS, controller.onBack(false));
    }

    @Test
    public void backClosesSettingsWhenOverlayVisible() {
        KioskKeyActionController controller = new KioskKeyActionController();

        assertEquals(KioskKeyActionController.Action.CLOSE_SETTINGS, controller.onBack(true));
    }

    @Test
    public void menuOpensSettingsWhenOverlayHidden() {
        KioskKeyActionController controller = new KioskKeyActionController();

        assertEquals(KioskKeyActionController.Action.SHOW_SETTINGS, controller.onMenu(false));
    }
}

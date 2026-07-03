package com.threethan.browser.browser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KioskModeControllerTest {
    @Test
    public void kioskModeForcesTopBarHiddenEvenWhenOpenedAsTab() {
        KioskModeController controller = new KioskModeController();

        assertTrue(controller.shouldForceHideTopBar(true, true));
    }

    @Test
    public void nonTabModeKeepsLegacyFullscreenBehavior() {
        KioskModeController controller = new KioskModeController();

        assertTrue(controller.shouldForceHideTopBar(false, false));
    }

    @Test
    public void normalTabModeDoesNotForceTopBarHidden() {
        KioskModeController controller = new KioskModeController();

        assertFalse(controller.shouldForceHideTopBar(false, true));
    }
}

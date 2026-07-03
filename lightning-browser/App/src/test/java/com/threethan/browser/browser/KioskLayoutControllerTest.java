package com.threethan.browser.browser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KioskLayoutControllerTest {
    @Test
    public void kioskWebViewAlwaysUsesFullScreenLayout() {
        KioskLayoutController controller = new KioskLayoutController();

        assertEquals(0, controller.getWebViewTop(true));
        assertEquals(0, controller.getWebViewBottom(true));
    }

    @Test
    public void nonKioskWebViewAlsoStartsFullscreenUntilOverlayRequiresChange() {
        KioskLayoutController controller = new KioskLayoutController();

        assertEquals(0, controller.getWebViewTop(false));
        assertEquals(0, controller.getWebViewBottom(false));
    }
}

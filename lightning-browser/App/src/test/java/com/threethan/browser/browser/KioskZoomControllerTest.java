package com.threethan.browser.browser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KioskZoomControllerTest {
    @Test
    public void defaultZoomIsOneHundredPercent() {
        KioskZoomController controller = new KioskZoomController();

        assertEquals(100, controller.getPercent());
        assertEquals("100%", controller.getLabel());
        assertEquals(1.0f, controller.getScale(), 0.001f);
    }

    @Test
    public void zoomInStopsAtOneHundredPercent() {
        KioskZoomController controller = new KioskZoomController(90);

        assertEquals(100, controller.zoomIn());
        assertEquals(100, controller.zoomIn());
    }

    @Test
    public void zoomOutStopsAtTenPercent() {
        KioskZoomController controller = new KioskZoomController(20);

        assertEquals(10, controller.zoomOut());
        assertEquals(10, controller.zoomOut());
    }

    @Test
    public void constructorRoundsToNearestTenPercentWithinBounds() {
        KioskZoomController low = new KioskZoomController(7);
        KioskZoomController rounded = new KioskZoomController(56);
        KioskZoomController high = new KioskZoomController(101);

        assertEquals(10, low.getPercent());
        assertEquals(60, rounded.getPercent());
        assertEquals(100, high.getPercent());
    }
}

package com.threethan.browser.browser;

public class KioskZoomController {
    public static final int MIN_PERCENT = 10;
    public static final int MAX_PERCENT = 100;
    public static final int STEP_PERCENT = 10;

    private int percent;

    public KioskZoomController() {
        this(MAX_PERCENT);
    }

    public KioskZoomController(int percent) {
        this.percent = normalize(percent);
    }

    public int zoomIn() {
        percent = normalize(percent + STEP_PERCENT);
        return percent;
    }

    public int zoomOut() {
        percent = normalize(percent - STEP_PERCENT);
        return percent;
    }

    public int getPercent() {
        return percent;
    }

    public float getScale() {
        return percent / 100f;
    }

    public String getLabel() {
        return percent + "%";
    }

    private int normalize(int value) {
        int rounded = Math.round(value / (float) STEP_PERCENT) * STEP_PERCENT;
        if (rounded < MIN_PERCENT) return MIN_PERCENT;
        if (rounded > MAX_PERCENT) return MAX_PERCENT;
        return rounded;
    }
}

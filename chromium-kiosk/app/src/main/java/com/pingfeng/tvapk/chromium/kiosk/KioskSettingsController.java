package com.pingfeng.tvapk.chromium.kiosk;

public final class KioskSettingsController {
    private KioskSettings baseline;
    private KioskSettings draft;

    public KioskSettingsController(KioskSettings baseline) {
        this.baseline = baseline;
        this.draft = baseline;
    }

    public void beginEdit() {
        draft = baseline;
    }

    public void setDraftUrl(String url) {
        draft = draft.withUrl(url);
    }

    public void setDraftZoomPercent(int zoomPercent) {
        draft = draft.withZoomPercent(zoomPercent);
    }

    public KioskSettings getPreviewSettings() {
        return draft;
    }

    public KioskSettings getActiveSettings() {
        return baseline;
    }

    public KioskSettings commit(long updatedAt) {
        baseline = draft.withUpdatedAt(updatedAt);
        draft = baseline;
        return baseline;
    }

    public KioskSettings cancel() {
        draft = baseline;
        return baseline;
    }
}

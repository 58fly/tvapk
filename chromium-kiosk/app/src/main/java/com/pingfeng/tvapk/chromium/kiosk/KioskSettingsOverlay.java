package com.pingfeng.tvapk.chromium.kiosk;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pingfeng.tvapk.chromium.R;

public final class KioskSettingsOverlay {
    public interface Listener {
        void onPreviewZoomChanged(int zoomPercent);
        void onSaveRequested(String targetUrl, int zoomPercent);
        void onCancelRequested();
        void onRestoreDefaultRequested();
        void onExitRequested();
    }

    private final View root;
    private final EditText urlEdit;
    private final SeekBar zoomSeek;
    private final TextView zoomValue;
    private final TextView kernelInfo;
    private final TextView versionView;
    private final Listener listener;
    private boolean isBinding;

    public KioskSettingsOverlay(View parent, Listener listener) {
        this.root = parent.findViewById(R.id.kioskSettingsOverlay);
        this.urlEdit = parent.findViewById(R.id.kioskUrlEdit);
        this.zoomSeek = parent.findViewById(R.id.kioskZoomSeek);
        this.zoomValue = parent.findViewById(R.id.kioskZoomValue);
        this.kernelInfo = parent.findViewById(R.id.kioskKernelInfo);
        this.versionView = parent.findViewById(R.id.kioskSettingsVersion);
        this.listener = listener;

        Button save = parent.findViewById(R.id.kioskSave);
        Button cancel = parent.findViewById(R.id.kioskCancel);
        Button restoreDefault = parent.findViewById(R.id.kioskRestoreDefault);
        Button exit = parent.findViewById(R.id.kioskExit);

        zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int zoomPercent = progressToZoom(progress);
                updateZoomValue(zoomPercent);
                if (!isBinding) listener.onPreviewZoomChanged(zoomPercent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        save.setOnClickListener(view -> listener.onSaveRequested(
                urlEdit.getText().toString(),
                progressToZoom(zoomSeek.getProgress())
        ));
        cancel.setOnClickListener(view -> listener.onCancelRequested());
        restoreDefault.setOnClickListener(view -> listener.onRestoreDefaultRequested());
        exit.setOnClickListener(view -> listener.onExitRequested());
    }

    public void show(KioskSettings settings) {
        isBinding = true;
        urlEdit.setText(settings.targetUrl);
        zoomSeek.setProgress(zoomToProgress(settings.zoomPercent));
        updateZoomValue(settings.zoomPercent);
        versionView.setText(settings.getVersionLabel());
        isBinding = false;

        root.setVisibility(View.VISIBLE);
        root.bringToFront();
        urlEdit.requestFocus();
    }

    public void setKernelInfo(String info) {
        kernelInfo.setText(info == null || info.trim().isEmpty() ? "-" : info);
    }

    public void hide() {
        root.setVisibility(View.GONE);
    }

    public boolean isVisible() {
        return root.getVisibility() == View.VISIBLE;
    }

    public void setDraftSettings(KioskSettings settings) {
        isBinding = true;
        urlEdit.setText(settings.targetUrl);
        zoomSeek.setProgress(zoomToProgress(settings.zoomPercent));
        updateZoomValue(settings.zoomPercent);
        isBinding = false;
        listener.onPreviewZoomChanged(settings.zoomPercent);
    }

    private static int progressToZoom(int progress) {
        return KioskSettings.MIN_ZOOM_PERCENT + progress * KioskSettings.ZOOM_STEP_PERCENT;
    }

    private static int zoomToProgress(int zoomPercent) {
        return (KioskSettings.normalizeZoomPercent(zoomPercent) - KioskSettings.MIN_ZOOM_PERCENT)
                / KioskSettings.ZOOM_STEP_PERCENT;
    }

    private void updateZoomValue(int zoomPercent) {
        zoomValue.setText(zoomPercent + "%");
    }
}

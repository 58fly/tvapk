package com.threethan.browser.kiosk;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.threethan.browser.R;

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
    private final ScrollView diagnosticsScroll;
    private final TextView diagnosticsText;
    private final TextView versionView;
    private final Listener listener;
    private String fullDiagnosticsText = "-";
    private boolean isBinding;

    public KioskSettingsOverlay(View parent, Listener listener) {
        this.root = parent.findViewById(R.id.kioskSettingsOverlay);
        this.urlEdit = parent.findViewById(R.id.kioskUrlEdit);
        this.zoomSeek = parent.findViewById(R.id.kioskZoomSeek);
        this.zoomValue = parent.findViewById(R.id.kioskZoomValue);
        this.diagnosticsScroll = parent.findViewById(R.id.kioskDiagnosticsScroll);
        this.diagnosticsText = parent.findViewById(R.id.kioskDiagnosticsText);
        this.versionView = parent.findViewById(R.id.kioskSettingsVersion);
        this.listener = listener;

        Button save = parent.findViewById(R.id.kioskSave);
        Button cancel = parent.findViewById(R.id.kioskCancel);
        Button restoreDefault = parent.findViewById(R.id.kioskRestoreDefault);
        Button exit = parent.findViewById(R.id.kioskExit);
        Button diagnosticsFullScreen = parent.findViewById(R.id.kioskDiagnosticsFullScreen);

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
        diagnosticsFullScreen.setOnClickListener(view -> showDiagnosticsDialog(parent.getContext()));
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

    public void hide() {
        root.setVisibility(View.GONE);
    }

    public void setDiagnosticsText(String text) {
        fullDiagnosticsText = text == null || text.trim().isEmpty() ? "-" : text;
        diagnosticsText.setText(fullDiagnosticsText);
        diagnosticsScroll.post(() -> diagnosticsScroll.scrollTo(0, 0));
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

    private void showDiagnosticsDialog(Context context) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setFocusable(true);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setScrollbarFadingEnabled(false);

        TextView textView = new TextView(context);
        int padding = dp(context, 16);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(fullDiagnosticsText);
        textView.setTextColor(0xFF222222);
        textView.setTextSize(13);
        textView.setLineSpacing(dp(context, 2), 1.0f);
        scrollView.addView(textView);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.kiosk_settings_diagnostics_fullscreen_title)
                .setView(scrollView)
                .setPositiveButton(R.string.kiosk_settings_diagnostics_close, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> scrollView.requestFocus());
        scrollView.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                scrollView.smoothScrollBy(0, dp(context, 80));
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                scrollView.smoothScrollBy(0, -dp(context, 80));
                return true;
            }
            return false;
        });
        dialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

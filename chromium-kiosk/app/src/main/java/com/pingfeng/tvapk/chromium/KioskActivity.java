package com.pingfeng.tvapk.chromium;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingfeng.tvapk.chromium.kiosk.KioskSettings;
import com.pingfeng.tvapk.chromium.kiosk.KioskSettingsController;
import com.pingfeng.tvapk.chromium.kiosk.KioskSettingsOverlay;
import com.pingfeng.tvapk.chromium.kiosk.KioskSettingsStore;

import java.io.IOException;

public class KioskActivity extends Activity {
    public static final String TARGET_URL = KioskSettings.DEFAULT_TARGET_URL;
    private static final long KERNEL_WAIT_TIMEOUT_MS = 15000L;

    // Memory management
    private static final long MEMORY_CLEAN_INTERVAL_MS = 6L * 60L * 60L * 1000L; // 6 hours
    private static final long MEMORY_THRESHOLD_MB = 300L; // Threshold to trigger forced clean
    private static final long MEMORY_REFRESH_DELAY_MS = 3000L; // Wait before refreshing page

    private WebView webView;
    private TextView diagnosticView;
    private TextView errorView;
    private TextView urlText;
    private TextView zoomLevel;
    private View topBar;
    private KioskSettingsStore settingsStore;
    private KioskSettingsController settingsController;
    private KioskSettingsOverlay settingsOverlay;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean contentCreated;
    private final Runnable memoryCleanerRunnable = new Runnable() {
        @Override
        public void run() {
            performMemoryClean();
            mainHandler.postDelayed(this, MEMORY_CLEAN_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        configureFullscreen();
        initSettings();
        waitForKernelThenCreateContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureFullscreen();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(memoryCleanerRunnable);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            showSettingsOverlay();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            showTopBar();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (settingsOverlay != null && settingsOverlay.isVisible()) {
                cancelSettingsOverlay();
                return true;
            }
            if (diagnosticView != null && diagnosticView.getVisibility() == View.VISIBLE) {
                diagnosticView.setVisibility(View.GONE);
                return true;
            }
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initSettings() {
        KioskSettings defaults = KioskSettings.defaults(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        settingsStore = new KioskSettingsStore(this, defaults);
        settingsController = new KioskSettingsController(settingsStore.load());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createContent() {
        if (contentCreated) {
            return;
        }
        contentCreated = true;
        setContentView(R.layout.activity_kiosk);

        FrameLayout webContainer = findViewById(R.id.webContainer);
        topBar = findViewById(R.id.topBar);
        diagnosticView = findViewById(R.id.diagnosticView);
        errorView = findViewById(R.id.errorView);
        urlText = findViewById(R.id.urlText);
        zoomLevel = findViewById(R.id.zoomLevel);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFFFFFFF);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        configureWebSettings(webView.getSettings());
        webView.setWebViewClient(createWebViewClient(webContainer));
        webContainer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        initTopBarControls();
        initSettingsOverlay();
        updateUrlText(settingsController.getActiveSettings().targetUrl);
        updateZoomLabel(settingsController.getActiveSettings().zoomPercent);
        webView.requestFocus();
        webView.loadUrl(settingsController.getActiveSettings().targetUrl);

        // Start periodic memory cleaning for long-running kiosk mode
        mainHandler.postDelayed(memoryCleanerRunnable, MEMORY_CLEAN_INTERVAL_MS);
    }

    private void waitForKernelThenCreateContent() {
        showKernelWaitingView();
        mainHandler.postDelayed(() -> {
            if (!contentCreated) {
                createContent();
                showError("内置 Chromium 内核初始化超时，当前可能仍在使用系统 WebView。");
            }
        }, KERNEL_WAIT_TIMEOUT_MS);
        WebViewKernelManager.runWhenReady(success -> runOnUiThread(() -> {
            createContent();
            if (!success) {
                showError("内置 Chromium 内核替换失败，当前可能仍在使用系统 WebView。");
            }
        }));
    }

    private void showKernelWaitingView() {
        TextView waitingView = new TextView(this);
        waitingView.setText("正在初始化 Chromium119 内核...");
        waitingView.setTextColor(0xFFFFFFFF);
        waitingView.setTextSize(22f);
        waitingView.setGravity(android.view.Gravity.CENTER);
        waitingView.setBackgroundColor(0xFF000000);
        setContentView(waitingView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private WebViewClient createWebViewClient(FrameLayout webContainer) {
        return new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                updateUrlText(url);
                applyPageScale(settingsController.getActiveSettings().getScale());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    showError("页面加载失败: " + error.getDescription());
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                showError("WebView 渲染进程已退出, didCrash=" + detail.didCrash());
                if (webView != null) {
                    webContainer.removeView(webView);
                    webView.destroy();
                    webView = null;
                }
                return true;
            }
        };
    }

    private void initTopBarControls() {
        findViewById(R.id.refresh).setOnClickListener(view -> {
            if (webView != null) webView.reload();
        });
        findViewById(R.id.zoomOut).setOnClickListener(view -> adjustZoom(-KioskSettings.ZOOM_STEP_PERCENT));
        findViewById(R.id.zoomIn).setOnClickListener(view -> adjustZoom(KioskSettings.ZOOM_STEP_PERCENT));
        findViewById(R.id.urlText).setOnClickListener(view -> showSettingsOverlay());
        findViewById(R.id.settings).setOnClickListener(view -> showSettingsOverlay());
        findViewById(R.id.exit).setOnClickListener(view -> finish());
        findViewById(R.id.refresh).setOnLongClickListener(view -> {
            toggleDiagnostics();
            return true;
        });
    }

    private void initSettingsOverlay() {
        settingsOverlay = new KioskSettingsOverlay(findViewById(R.id.rootContainer), new KioskSettingsOverlay.Listener() {
            @Override
            public void onPreviewZoomChanged(int zoomPercent) {
                settingsController.setDraftZoomPercent(zoomPercent);
                updateZoomLabel(zoomPercent);
                applyPageScale(zoomPercent / 100f);
            }

            @Override
            public void onSaveRequested(String targetUrl, int zoomPercent) {
                saveSettings(targetUrl, zoomPercent);
            }

            @Override
            public void onCancelRequested() {
                cancelSettingsOverlay();
            }

            @Override
            public void onRestoreDefaultRequested() {
                KioskSettings defaults = KioskSettings.defaults(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
                settingsController.beginEdit();
                settingsController.setDraftUrl(defaults.targetUrl);
                settingsController.setDraftZoomPercent(defaults.zoomPercent);
                settingsOverlay.setDraftSettings(settingsController.getPreviewSettings());
            }

            @Override
            public void onExitRequested() {
                finish();
            }
        });
    }

    private void adjustZoom(int delta) {
        KioskSettings active = settingsController.getActiveSettings();
        int nextZoom = KioskSettings.normalizeZoomPercent(active.zoomPercent + delta);
        saveSettings(active.targetUrl, nextZoom);
    }

    private void showSettingsOverlay() {
        if (settingsOverlay == null || settingsController == null) return;

        settingsController.beginEdit();
        settingsOverlay.setKernelInfo(buildSettingsKernelInfo());
        settingsOverlay.show(settingsController.getPreviewSettings());
        if (diagnosticView != null) diagnosticView.setVisibility(View.GONE);
        showTopBar();
    }

    private void cancelSettingsOverlay() {
        KioskSettings restored = settingsController.cancel();
        updateZoomLabel(restored.zoomPercent);
        applyPageScale(restored.getScale());
        if (settingsOverlay != null) settingsOverlay.hide();
        configureFullscreen();
    }

    private void saveSettings(String targetUrl, int zoomPercent) {
        settingsController.setDraftUrl(toValidUrl(targetUrl));
        settingsController.setDraftZoomPercent(zoomPercent);
        KioskSettings pending = settingsController.getPreviewSettings().withUpdatedAt(System.currentTimeMillis());

        try {
            settingsStore.save(pending);
        } catch (IOException e) {
            Toast.makeText(this, "设置保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        KioskSettings saved = settingsController.commit(pending.updatedAt);
        updateZoomLabel(saved.zoomPercent);
        updateUrlText(saved.targetUrl);
        applyPageScale(saved.getScale());
        if (settingsOverlay != null) settingsOverlay.hide();
        if (webView != null && !saved.targetUrl.equals(webView.getUrl())) {
            webView.loadUrl(saved.targetUrl);
        }
        configureFullscreen();
    }

    private void applyPageScale(float scale) {
        if (webView == null) return;

        String script = "(function(){"
                + "var scale=" + scale + ";"
                + "var root=document.documentElement;"
                + "var body=document.body;"
                + "if(!root||!body){return;}"
                + "root.style.width=(100/scale)+'%';"
                + "root.style.minWidth=(100/scale)+'%';"
                + "root.style.height=(100/scale)+'%';"
                + "root.style.transformOrigin='0 0';"
                + "root.style.transform='scale('+scale+')';"
                + "body.style.width=(100/scale)+'%';"
                + "body.style.minWidth=(100/scale)+'%';"
                + "body.style.transformOrigin='0 0';"
                + "body.style.overflow='hidden';"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private void configureFullscreen() {
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void showTopBar() {
        if (topBar != null) {
            topBar.setVisibility(View.VISIBLE);
            topBar.bringToFront();
        }
    }

    private void toggleDiagnostics() {
        if (diagnosticView == null) {
            return;
        }
        if (diagnosticView.getVisibility() == View.VISIBLE) {
            diagnosticView.setVisibility(View.GONE);
            return;
        }
        String userAgent = webView == null ? "" : webView.getSettings().getUserAgentString();
        diagnosticView.setText(buildDiagnosticStatus(userAgent));
        diagnosticView.setVisibility(View.VISIBLE);
        diagnosticView.bringToFront();
    }

    private void showError(String message) {
        if (errorView == null) {
            return;
        }
        String userAgent = webView == null ? "" : webView.getSettings().getUserAgentString();
        errorView.setText(message + "\n\n" + buildDiagnosticStatus(userAgent));
        errorView.setVisibility(View.VISIBLE);
        errorView.bringToFront();
    }

    private void updateUrlText(String url) {
        if (urlText != null) {
            urlText.setText(url == null ? TARGET_URL : url);
        }
    }

    private void updateZoomLabel(int zoomPercent) {
        if (zoomLevel != null) {
            zoomLevel.setText(KioskSettings.normalizeZoomPercent(zoomPercent) + "%");
        }
    }

    private static String toValidUrl(String value) {
        if (value == null || value.trim().isEmpty()) return TARGET_URL;

        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        return "http://" + trimmed;
    }

    private String buildDiagnosticStatus(String userAgent) {
        String currentUrl = webView == null ? "" : webView.getUrl();
        return WebViewKernelManager.buildStatus(this, userAgent)
                + "\nURL: "
                + (currentUrl == null || currentUrl.isEmpty() ? settingsController.getActiveSettings().targetUrl : currentUrl);
    }

    private String buildSettingsKernelInfo() {
        String userAgent = webView == null ? "" : webView.getSettings().getUserAgentString();
        WebViewKernelManager.KernelStatus kernelStatus = WebViewKernelManager.getKernelStatus(userAgent);
        StringBuilder builder = new StringBuilder();
        builder.append("系统内核：")
                .append(kernelStatus.systemPackageName)
                .append(" / ")
                .append(kernelStatus.systemVersion)
                .append(" / Chromium ")
                .append(formatMajor(kernelStatus.systemChromiumMajor))
                .append("\n");
        builder.append("APK 内置：")
                .append(kernelStatus.embeddedPackageName)
                .append(" / ")
                .append(kernelStatus.embeddedVersion)
                .append(" / Chromium ")
                .append(formatMajor(kernelStatus.embeddedChromiumMajor))
                .append("\n");
        builder.append("当前实际：")
                .append(kernelStatus.activePackageName)
                .append(" / ")
                .append(kernelStatus.activeVersion)
                .append(" / Chromium ")
                .append(formatMajor(kernelStatus.activeChromiumMajor))
                .append("\n");
        builder.append("替换状态：")
                .append(kernelStatus.upgradeStatus)
                .append(" ")
                .append(kernelStatus.upgradeProgress)
                .append("%");
        if (!kernelStatus.upgradeError.isEmpty()) {
            builder.append("\n错误信息：").append(kernelStatus.upgradeError);
        }
        return builder.toString();
    }

    private static String formatMajor(int major) {
        return major > 0 ? String.valueOf(major) : "-";
    }

    // Memory management for long-running kiosk mode
    private void performMemoryClean() {
        if (webView == null) return;

        long usedMb = getMemoryUsedMb();
        if (usedMb < MEMORY_THRESHOLD_MB) {
            return; // Memory usage is acceptable, skip cleaning
        }

        android.util.Log.i("MemoryCleaner", "Memory threshold exceeded: " + usedMb + "MB, performing clean");

        // 1. Clear caches
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        // 2. Trigger garbage collection
        System.gc();

        // 3. Reload page after delay to release accumulated memory
        if (webView.getUrl() != null) {
            final String url = webView.getUrl();
            mainHandler.postDelayed(() -> {
                if (webView != null) {
                    webView.loadUrl(url);
                }
            }, MEMORY_REFRESH_DELAY_MS);
        }
    }

    private long getMemoryUsedMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
    }
}

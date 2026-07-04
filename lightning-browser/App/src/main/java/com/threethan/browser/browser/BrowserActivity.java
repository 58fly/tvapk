package com.threethan.browser.browser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;

import com.threethan.browser.BuildConfig;
import com.threethan.browser.R;
import com.threethan.browser.browser.GeckoView.BrowserWebView;
import com.threethan.browser.browser.GeckoView.Delegate.CustomNavigationDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomPromptDelegate;
import com.threethan.browser.diagnostics.AppDiagnostics;
import com.threethan.browser.helper.BookmarkManager;
import com.threethan.browser.helper.CustomDialog;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.helper.FaviconLoader;
import com.threethan.browser.helper.Keyboard;
import com.threethan.browser.helper.PermissionManager;
import com.threethan.browser.helper.TabManager;
import com.threethan.browser.kiosk.KioskSettings;
import com.threethan.browser.kiosk.KioskSettingsController;
import com.threethan.browser.kiosk.KioskSettingsOverlay;
import com.threethan.browser.kiosk.KioskSettingsStore;
import com.threethan.browser.lib.StringLib;
import com.threethan.browser.updater.BrowserUpdater;
import com.threethan.browser.wrapper.BoundActivity;
import com.threethan.browser.wrapper.EditTextWatched;

import org.mozilla.geckoview.GeckoSession;

import java.io.IOException;

public class BrowserActivity extends BoundActivity {
    private BrowserWebView w;
    TextView urlPre;
    TextView urlMid;
    TextView urlEnd;
    ImageView favicon;
    public String tabId = null;
    View back;
    View forward;
    View background;
    ProgressBar loading;
    View bookmarkAdd;
    View bookmarkRem;
    View permissionButton;
    TextView zoomLevel;
    protected final BookmarkManager bookmarkManager = new BookmarkManager(this);
    private final KioskZoomController kioskZoomController = new KioskZoomController();
    private final KioskLayoutController kioskLayoutController = new KioskLayoutController();
    private final KioskModeController kioskModeController = new KioskModeController();
    private final KioskKeyActionController kioskKeyActionController = new KioskKeyActionController();
    private KioskSettingsStore kioskSettingsStore;
    private KioskSettingsController kioskSettingsController;
    private KioskSettingsOverlay kioskSettingsOverlay;
    private boolean isTopBarForciblyHidden;
    private static final long KIOSK_CONTROLS_TIMEOUT_MS = 10000L;
    private final Handler kioskControlsHandler = new Handler();
    private final Runnable hideKioskControlsRunnable = () -> {
        if (isTopBarForciblyHidden) {
            hideKioskControls();
        }
    };
    private boolean isEphemeral;
    private boolean isTab;
    private boolean isKiosk;
    private String defaultUrl = KioskSettings.DEFAULT_TARGET_URL;
    private final PermissionManager permissionManager = new PermissionManager(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bind();

        Log.v("Lightning Browser", "Starting Browser Activity");
        AppDiagnostics.record(this, "ACTIVITY_CREATE", "intent=" + getIntent().getAction());

        setContentView(R.layout.activity_browser);
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        enableKioskFullscreen();

        background = findViewById(R.id.container);
        loading = findViewById(R.id.loading);
        initKioskSettings();
        initKioskSettingsOverlay();

        urlPre = findViewById(R.id.urlPre);
        urlMid = findViewById(R.id.urlMid);
        urlEnd = findViewById(R.id.urlEnd);

        favicon = findViewById(R.id.favicon);
        favicon.setClipToOutline(true);

        Uri uri = getIntent().getData();
        if (uri != null && !uri.toString().isEmpty()) currentUrl = uri.toString();
        else currentUrl = defaultUrl;

        isTab = getIntent().getBooleanExtra("isTab", false);
        isKiosk = getIntent().getBooleanExtra("isKiosk", false);

        if (getIntent().getExtras() != null) {
            isEphemeral = getIntent().getBooleanExtra("isEphemeral", false);
            if (isTab) {
                if (currentUrl.startsWith(BrowserService.TAB_PREFIX)) {
                    tabId = currentUrl;
                    currentUrl = BrowserService.getUrl(tabId);
                } else tabId = BrowserService.getNewTabId();
            }
        }
        if (tabId == null) tabId = BrowserService.TAB_PREFIX + "ext::" + currentUrl;
        Log.v("Lightning Browser", "... with url " + currentUrl
                + (isTab ? ", is a tab" : ", not a tab")
                + (isKiosk ? ", kiosk mode" : "")
                + ", assigned id " + tabId);

        if (kioskModeController.shouldForceHideTopBar(isKiosk, isTab)) {
            isTopBarForciblyHidden = true;
            hideTopBar();
        }

        // Back/Forward Buttons
        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

        back.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoBack()) w.goBack();
            updateButtonsAndUrl();
        });
        forward.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoForward()) w.goForward();
            updateButtonsAndUrl();
        });

        back.setOnLongClickListener((view -> {
            if (w == null) return false;
            w.forwardFull();
            updateButtonsAndUrl();
            return true;
        }));
        forward.setOnLongClickListener((view -> {
            if (w == null) return false;
            w.backFull();
            updateButtonsAndUrl();
            return true;
        }));

        // Refresh Button
        View refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener((view) -> reload());
        refresh.setOnLongClickListener((view) -> {
            w.loadUrl(currentUrl);
            w.clearQueued = true;
            updateButtonsAndUrl(currentUrl);
            return true;
        });

        View exit = findViewById(R.id.exit);
        exit.setOnClickListener((View) -> finish());

        View zoomOut = findViewById(R.id.zoomOut);
        View zoomIn = findViewById(R.id.zoomIn);
        zoomLevel = findViewById(R.id.zoomLevel);
        updateZoomLabel();
        zoomOut.setOnClickListener((view) -> {
            kioskZoomController.zoomOut();
            applyKioskZoom();
            scheduleKioskControlsAutoHide();
        });
        zoomIn.setOnClickListener((view) -> {
            kioskZoomController.zoomIn();
            applyKioskZoom();
            scheduleKioskControlsAutoHide();
        });

        // Bookmark Button
        bookmarkAdd = findViewById(R.id.addBookmark);
        bookmarkRem = findViewById(R.id.removeBookmark);
        bookmarkAdd.setOnClickListener((view) -> {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.VISIBLE);
            bookmarkManager.addBookmark(currentUrl, BrowserService.getTitle(tabId));
        });
        bookmarkRem.setOnClickListener((view) -> {
            bookmarkRem.setVisibility(View.GONE);
            bookmarkAdd.setVisibility(View.VISIBLE);
            bookmarkManager.removeBookmark(currentUrl);
        });

        // Permission Button
        permissionButton = findViewById(R.id.permissionButton);
        permissionButton.setOnClickListener((view) -> {
            String origin = "";
            if (w.getSession() != null
                    && w.getSession().getNavigationDelegate()
                    instanceof CustomNavigationDelegate navDelegate) {
                origin = navDelegate.getOrigin();
            }

            String[] permissions = permissionManager.getPermissionsForOrigin(origin);

            String finalOrigin = origin;
            new CustomDialog.Builder(this)
                    .setTitle(R.string.permission_manage)
                    .setMessage(getString(R.string.permission_manage_message, origin,
                            StringLib.buildPermissionNamesList(permissions, this)))
                    .setPositiveButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(R.string.permission_revoke, (dialog, which) -> {
                        for (String permission : PermissionManager.KNOWN_PERMISSIONS) {
                            permissionManager.setPermission(finalOrigin, permission, false);
                        }
                        permissionButton.setVisibility(View.GONE);
                        // Refresh page to apply permission changes
                        reload();
                        dialog.dismiss();
                    })
                    .show();
        });


        // Edit URL
        View urlLayout = findViewById(R.id.urlLayout);
        EditTextWatched urlEdit = findViewById(R.id.urlEdit);
        View clearUrl = findViewById(R.id.clear);
        View confirm = findViewById(R.id.confirm);
        urlEdit.setOnEdited((string) -> {
            clearUrl.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
            confirm.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
        });
        clearUrl.setOnClickListener((view) -> urlEdit.setText(""));

        View topBar = findViewById(R.id.topBar);
        View topBarEdit = findViewById(R.id.topBarEdit);
        urlLayout.setOnClickListener((view) -> {
            if (isKiosk) {
                showKioskSettingsOverlay();
                return;
            }
            topBar.setVisibility(View.GONE);
            topBarEdit.setVisibility(View.VISIBLE);
            urlEdit.setText(currentUrl);
            urlEdit.post(urlEdit::requestFocus);
            urlEdit.postDelayed(() -> Keyboard.show(this), 100);
        });
        urlEdit.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on Enter key press
                topBarEdit.findViewById(R.id.confirm).callOnClick();
                return true;
            }
            return false;
        });
        confirm.setOnClickListener((view) -> {
            String nUrl = urlEdit.getText().toString();

            nUrl = StringLib.toValidUrl(nUrl);

            w.loadUrl(nUrl);
            updateButtonsAndUrl(nUrl);
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });
        topBarEdit.findViewById(R.id.cancel).setOnClickListener((view) -> {
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });

        findViewById(R.id.extensions).setOnClickListener(v -> BrowserService.ManageExtensions());

        new BrowserUpdater(this).checkAppUpdateInteractive();
    }

    private void initKioskSettings() {
        KioskSettings defaults = KioskSettings.defaults(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        kioskSettingsStore = new KioskSettingsStore(this, defaults);
        kioskSettingsController = new KioskSettingsController(kioskSettingsStore.load());
        defaultUrl = kioskSettingsController.getActiveSettings().targetUrl;
    }

    private void initKioskSettingsOverlay() {
        kioskSettingsOverlay = new KioskSettingsOverlay(background, new KioskSettingsOverlay.Listener() {
            @Override
            public void onPreviewZoomChanged(int zoomPercent) {
                kioskSettingsController.setDraftZoomPercent(zoomPercent);
                applyKioskSettingsPreview(kioskSettingsController.getPreviewSettings());
            }

            @Override
            public void onSaveRequested(String targetUrl, int zoomPercent) {
                saveKioskSettings(targetUrl, zoomPercent);
            }

            @Override
            public void onCancelRequested() {
                cancelKioskSettingsOverlay();
            }

            @Override
            public void onRestoreDefaultRequested() {
                KioskSettings defaults = KioskSettings.defaults(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
                kioskSettingsController.beginEdit();
                kioskSettingsController.setDraftUrl(defaults.targetUrl);
                kioskSettingsController.setDraftZoomPercent(defaults.zoomPercent);
                kioskSettingsOverlay.setDraftSettings(kioskSettingsController.getPreviewSettings());
            }

            @Override
            public void onExitRequested() {
                finish();
            }
        });
    }

    private void showKioskSettingsOverlay() {
        if (kioskSettingsOverlay == null || kioskSettingsController == null) return;

        AppDiagnostics.record(this, "SETTINGS_OPEN", "url=" + currentUrl);
        kioskControlsHandler.removeCallbacks(hideKioskControlsRunnable);
        hideKioskControls();
        kioskSettingsController.beginEdit();
        kioskSettingsOverlay.setDiagnosticsText(AppDiagnostics.readRecentText(this, 40));
        kioskSettingsOverlay.show(kioskSettingsController.getPreviewSettings());
    }

    private void hideKioskSettingsOverlay() {
        if (kioskSettingsOverlay == null) return;

        kioskSettingsOverlay.hide();
        enableKioskFullscreen();
    }

    private void cancelKioskSettingsOverlay() {
        if (kioskSettingsController == null) return;

        KioskSettings restored = kioskSettingsController.cancel();
        applyKioskSettingsPreview(restored);
        hideKioskSettingsOverlay();
    }

    private void saveKioskSettings(String targetUrl, int zoomPercent) {
        if (kioskSettingsController == null || kioskSettingsStore == null) return;

        kioskSettingsController.setDraftUrl(StringLib.toValidUrl(targetUrl));
        kioskSettingsController.setDraftZoomPercent(zoomPercent);
        long updatedAt = System.currentTimeMillis();
        KioskSettings pending = kioskSettingsController.getPreviewSettings().withUpdatedAt(updatedAt);

        try {
            kioskSettingsStore.save(pending);
        } catch (IOException e) {
            Log.e("Lightning Browser", "Failed to save kiosk settings", e);
            Dialog.toast(getString(R.string.kiosk_settings_save_failed), e.getMessage(), true);
            return;
        }

        KioskSettings saved = kioskSettingsController.commit(updatedAt);
        defaultUrl = saved.targetUrl;
        applyKioskSettingsPreview(saved);
        hideKioskSettingsOverlay();

        if (!saved.targetUrl.equals(currentUrl) && w != null) {
            w.loadUrl(saved.targetUrl);
            updateButtonsAndUrl(saved.targetUrl);
        }

        Dialog.toast(getString(R.string.kiosk_settings_saved), saved.targetUrl, false);
    }

    private void applyKioskSettingsPreview(KioskSettings settings) {
        if (w != null) w.applyPageScale(settings.getScale());
    }

    private void enableKioskFullscreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableKioskFullscreen();
    }

    public boolean shouldKeepTopBarHidden() {
        return isTopBarForciblyHidden;
    }

    public void applyKioskZoom() {
        updateZoomLabel();
        if (w == null) return;

        if (kioskSettingsController != null) {
            w.applyPageScale(kioskSettingsController.getActiveSettings().getScale());
        } else {
            w.applyPageScale(kioskZoomController.getScale());
        }
    }

    private void updateZoomLabel() {
        if (zoomLevel == null) return;

        if (kioskSettingsController != null) {
            zoomLevel.setText(kioskSettingsController.getActiveSettings().zoomPercent + "%");
        } else {
            zoomLevel.setText(kioskZoomController.getLabel());
        }
    }

    private void scheduleKioskControlsAutoHide() {
        if (kioskSettingsOverlay != null && kioskSettingsOverlay.isVisible()) return;
        if (!isTopBarForciblyHidden || findViewById(R.id.topBar).getVisibility() != View.VISIBLE) return;

        kioskControlsHandler.removeCallbacks(hideKioskControlsRunnable);
        kioskControlsHandler.postDelayed(hideKioskControlsRunnable, KIOSK_CONTROLS_TIMEOUT_MS);
        Log.v("Lightning Browser", "Kiosk controls auto-hide scheduled");
    }

    private void updateButtonsAndUrl() {
        updateButtonsAndUrl(w.getUrl());
    }

    public void updateButtonsAndUrl(String url) {
        if (w == null) return;
        updateUrl(url);
    }

    private void reload() {
        AppDiagnostics.record(this, "PAGE_RELOAD", "url=" + currentUrl);
        w.reload();
    }

    public void startLoading() {
        AppDiagnostics.record(this, "PAGE_START", "url=" + currentUrl);
        loading.setVisibility(View.VISIBLE);
        loading.setIndeterminate(true);
        back.setAlpha(0.5f);
        forward.setAlpha(0.5f);
    }


    public void setLoadingProgress(int progress) {
        loading.setVisibility(View.VISIBLE);
        if (progress <= 0 || progress >= 100) {
            loading.setIndeterminate(true);
        } else {
            loading.setIndeterminate(false);
            loading.setProgress(progress);
        }
    }

    public void stopLoading() {
        AppDiagnostics.record(this, "PAGE_STOP", "url=" + currentUrl);
        loading.setIndeterminate(true);
        loading.setVisibility(View.GONE);
        // Update navigation
        back.setAlpha(1f);
        forward.setAlpha(1f);
        back.setVisibility(w.canGoBack() && !w.clearQueued ? View.VISIBLE : View.GONE);
        forward.setVisibility(w.canGoForward() && !w.clearQueued ? View.VISIBLE : View.GONE);
    }

    protected void setGeckoViewTop(int top) {
        if (w != null) {
            if (w.getLayoutParams() instanceof FrameLayout.LayoutParams flp) {
                flp.topMargin = kioskLayoutController.getWebViewTop(isTopBarForciblyHidden);
                flp.bottomMargin = kioskLayoutController.getWebViewBottom(isTopBarForciblyHidden);
                w.setLayoutParams(flp);
            }
        } else {
            new Handler().postDelayed(() -> setGeckoViewTop(top), 100);
        }
    }

    public void showTopBar() {
        View topBar = findViewById(R.id.topBar);
        View topBarEdit = findViewById(R.id.topBarEdit);
        topBar.setVisibility(View.VISIBLE);
        topBarEdit.setVisibility(View.GONE);
        topBar.bringToFront();

        setGeckoViewTop(0);
        scheduleKioskControlsAutoHide();
    }

    public void hideTopBar() {
        kioskControlsHandler.removeCallbacks(hideKioskControlsRunnable);
        hideKioskControls();
    }

    private void hideKioskControls() {
        findViewById(R.id.topBar).setVisibility(View.GONE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);

        setGeckoViewTop(0);
        enableKioskFullscreen();
        Log.v("Lightning Browser", "Kiosk controls hidden");
    }

    public String currentUrl = "";

    // Splits the URL into parts and updates the URL display
    @SuppressLint("SetTextI18n") // It wants me to use a string resource to add a dot
    private void updateUrl(String url) {
        if (url == null) url = defaultUrl;

        StringLib.ParititionedUrl pUrl = new StringLib.ParititionedUrl(url);

        urlPre.setText(pUrl.prefix);
        urlMid.setText(pUrl.middle);
        urlEnd.setText(pUrl.suffix);

        currentUrl = url;
        BrowserService.putUrl(tabId, url);

        if (bookmarkManager.getBookmarks().contains(currentUrl)) {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.VISIBLE);
        } else {
            bookmarkRem.setVisibility(View.GONE);
            bookmarkAdd.setVisibility(View.VISIBLE);
        }

        FaviconLoader.loadFavicon(this, currentUrl, favicon::setImageDrawable);
        permissionButton.setVisibility(permissionManager.hasPermissionsForOrigin(StringLib.getOrigin(url)) ? View.VISIBLE : View.GONE);
        startLoading();
    }

    @NonNull
    @Override
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        OnBackInvokedDispatcher dispatcher = super.getOnBackInvokedDispatcher();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            dispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBackPressed
            );
        }
        return dispatcher;
    }
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            handleBackPressed();
        }
    }
    public void handleBackPressed() {
        if (handleKioskKeyAction(kioskKeyActionController.onBack(isKioskSettingsVisible()))) return;

        if (isFullScreen()) {
            fullScreenSession.exitFullScreen();
            setFullScreenSession(null);
        } else if (findViewById(R.id.topBarEdit).getVisibility() == View.VISIBLE) {
            findViewById(R.id.cancel).callOnClick();
        } else if (w.canGoBack()) {
            w.goBack();
            updateButtonsAndUrl();
        } else {
            showKioskSettingsOverlay();
        }
    }

    @Override
    protected void
    onDestroy() {
        kioskControlsHandler.removeCallbacks(hideKioskControlsRunnable);
        AppDiagnostics.record(this, "ACTIVITY_DESTROY", "finishing=" + isFinishing() + ", tab=" + tabId);
        try {
            if (wService != null) {
                if (isFinishing()) {
                    // Don't keep search views in background
                    if (isEphemeral) {
                        AppDiagnostics.record(this, "WEBVIEW_KILL", "ephemeral tab=" + tabId);
                        wService.killWebView(tabId);
                    } else {
                        TabManager tabManager = new TabManager(this);

                        if (tabManager.shouldUseSuspendedTabs(this)) {
                            String title = BrowserService.getTitle(tabId);
                            String url = BrowserService.getUrl(tabId);
                            AppDiagnostics.record(this, "WEBVIEW_SUSPEND", "tab=" + tabId + ", url=" + url);
                            wService.killWebView(tabId);
                            tabManager.addSuspendedTab(tabId, url, title);
                        } else {
                            wService.setWebViewActive(tabId, false);
                        }
                    }
                } else {
                    // System is reclaiming this activity (e.g. low memory).
                    // Do NOT keep WebView in memory to prevent accumulation leak.
                    AppDiagnostics.record(this, "WEBVIEW_KILL", "system_reclaim tab=" + tabId);
                    wService.killWebView(tabId);
                }
                wService.removeActivity(this);
            }
        } catch (Throwable throwable) {
            AppDiagnostics.record(this, "DESTROY_ERROR", AppDiagnostics.throwableSummary(throwable));
        }
        if (isFinishing()) {
            AppDiagnostics.markCleanExit(this, "Activity onDestroy finishing=true, tab=" + tabId);
        } else {
            AppDiagnostics.markRuntimeState(this, "ACTIVITY_DESTROY finishing=false, tab=" + tabId);
        }
        super.onDestroy();
    }

    public void loadUrl(String url) {
        if (url == null) {
            Log.w("Lightning Browser", "Tried to load null URL");
            return;
        }
        AppDiagnostics.record(this, "LOAD_URL", "url=" + url);
        w.loadUrl(url);
        updateButtonsAndUrl(url);
    }

    // Sets the WebView when the service is bound
    @Override
    protected void onBound() {
        assert wService != null;
        if (tabId == null) tabId = defaultUrl;
        AppDiagnostics.record(this, "SERVICE_BOUND", "tab=" + tabId + ", url=" + currentUrl);

        // Show conditional buttons
        View kill = findViewById(R.id.kill);
        if (isTab) {
            kill.setVisibility(View.GONE);
        } else {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.GONE);
            kill.setVisibility(View.VISIBLE);
            kill.setOnClickListener((view) -> wService.killWebView(tabId));
        }

        w = wService.getWebView(this);
        w.setOnScrollInterceptor(this::handleScrollChanged);
        CursorLayout container = findViewById(R.id.container);
        setGeckoViewTop(0);
        container.addView(w, 0);
        container.targetView = w;
        findViewById(R.id.topBar).bringToFront();
        findViewById(R.id.topBarEdit).bringToFront();
        if (kioskSettingsOverlay != null) kioskSettingsOverlay.hide();
        findViewById(R.id.kioskSettingsOverlay).bringToFront();
        findViewById(R.id.loading).bringToFront();
        applyKioskZoom();

        updateButtonsAndUrl();
    }

    @Override
    protected void onResume() {
        Dialog.setActivityContext(this);
        AppDiagnostics.markRuntimeState(this, "FOREGROUND tab=" + tabId + ", url=" + currentUrl);
        AppDiagnostics.record(this, "ACTIVITY_RESUME", "tab=" + tabId + ", url=" + currentUrl);
        super.onResume();
    }

    @Override
    protected void onPause() {
        AppDiagnostics.markRuntimeState(this, "BACKGROUND tab=" + tabId + ", url=" + currentUrl);
        AppDiagnostics.record(this, "ACTIVITY_PAUSE", "tab=" + tabId + ", url=" + currentUrl);
        super.onPause();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            return handleKioskKeyAction(kioskKeyActionController.onMenu(isKioskSettingsVisible()));
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) onBackPressed();
        return true;
    }

    private boolean isKioskSettingsVisible() {
        return kioskSettingsOverlay != null && kioskSettingsOverlay.isVisible();
    }

    private boolean handleKioskKeyAction(KioskKeyActionController.Action action) {
        if (action == KioskKeyActionController.Action.SHOW_SETTINGS) {
            showKioskSettingsOverlay();
            return true;
        }
        if (action == KioskKeyActionController.Action.CLOSE_SETTINGS) {
            cancelKioskSettingsOverlay();
            return true;
        }
        return false;
    }

    private float accumulatedScrollY = 0;

    public boolean handleScrollChanged(int deltaX, int deltaY) {
        if (isTopBarForciblyHidden || isFullScreen()) return false;

        float density = getResources().getDisplayMetrics().density;


        float topLayoutHeight = getTopLayoutHeight();

        if (accumulatedScrollY > topLayoutHeight * 2) accumulatedScrollY = topLayoutHeight * 2;
        if (accumulatedScrollY < -topLayoutHeight) accumulatedScrollY = -topLayoutHeight;

        float prevTop = -accumulatedScrollY + topLayoutHeight;
        if (prevTop < 0) prevTop = 0;
        if (prevTop > topLayoutHeight) prevTop = topLayoutHeight;

        accumulatedScrollY += deltaY * density;

        float top = -accumulatedScrollY + topLayoutHeight;
        if (top < 0) top = 0;
        if (top > topLayoutHeight) top = topLayoutHeight;

        if (top != prevTop) {
            setGeckoViewTop((int) top);
            return true;
        } else {
            return false;
        }
    }


    private int getTopLayoutHeight() {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (42 * scale + 0.5f);
    }

    GeckoSession fullScreenSession = null;

    protected boolean isFullScreen() {
        return fullScreenSession != null;
    }

    public void setFullScreenSession(GeckoSession geckoSession) {
        this.fullScreenSession = geckoSession;
        if (isFullScreen()) hideTopBar();
        else showTopBar();
    }

    private GeckoSession.PermissionDelegate.Callback pendingPermissionCallback = null;
    private String pendingPermissionOrigin = null;

    public void requestPermissions(String[] permissions, GeckoSession
            session, GeckoSession.PermissionDelegate.Callback callback) {

        String origin = "";
        if (session.getNavigationDelegate() instanceof CustomNavigationDelegate navDelegate) {
            origin = navDelegate.getOrigin();
        }

        boolean siteGranted = true;
        for (String permission : permissions) {
            if (!permissionManager.getPermission(origin, permission)) {
                siteGranted = false;
                break;
            }
        }
        boolean androidGranted = true;
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                androidGranted = false;
                break;
            }
        }

        if (siteGranted) {
            callback.grant();
            if (!androidGranted) {
                // Need to request Android permissions as well
                requestPermissions(permissions, 1);
            }
            return;
        }
        String finalOrigin = origin;
        new CustomDialog.Builder(this)
                .setTitle(R.string.permission_manage)
                .setMessage(getString(R.string.permission_request_message, origin,
                        StringLib.buildPermissionNamesList(permissions, this)))
                .setPositiveButton(R.string.permission_allow, (dialog, which) -> {
                    for (String permission : permissions) {
                        permissionManager.setPermission(finalOrigin, permission, true);
                        if (checkSelfPermission(permission)
                                != PackageManager.PERMISSION_GRANTED) {
                            pendingPermissionCallback = callback;
                            pendingPermissionOrigin = finalOrigin;
                            requestPermissions(permissions, 1);
                            return;
                        }
                    }
                    permissionButton.setVisibility(View.VISIBLE);
                    callback.grant();
                })
                .setNegativeButton(R.string.permission_deny, (dialog, which)
                        -> callback.reject())
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (pendingPermissionCallback != null) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    for (String permission : permissions)
                        permissionManager.setPermission(pendingPermissionOrigin, permission, true);
                    pendingPermissionCallback.grant();
                    permissionButton.setVisibility(View.VISIBLE);
                    if (wService != null) wService.restartForeground();
                } else {
                    pendingPermissionCallback.reject();
                }
                pendingPermissionCallback = null;
            }
        } catch (Exception e) {
            Log.e("BrowserActivity", "Error handling permission result", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (w.getPromptDelegate() instanceof CustomPromptDelegate promptDelegate) {
            if (promptDelegate.filePickerRequestCode == requestCode) {
                promptDelegate.handleFilePickerResult(resultCode, data);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

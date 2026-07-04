package com.threethan.browser.browser.GeckoView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;

import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.browser.GeckoView.Delegate.CustomContentDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomHistoryDelgate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomNavigationDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomPermissionDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomProgressDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomPromptDelegate;

import org.mozilla.geckoview.GeckoSession;

import java.util.Objects;

/*
    BrowserWebView

    A customized version of GeckoView which keeps media playing in the background.
 */
@SuppressLint("ViewConstructor")
public class BrowserWebView extends ScrollHandlingGeckoView {
    // Delegates
    private final CustomNavigationDelegate navigationDelegate;
    private final CustomHistoryDelgate historyDelegate;
    private final CustomProgressDelegate progressDelegate;
    private final CustomPromptDelegate promptDelegate;
    private final CustomContentDelegate contentDelegate;
    private final CustomPermissionDelegate permissionDelegate;
    private final CustomMediaSessionDelegate mediaSessionDelegate;

    // Functions
    public void goBack() {
        if (getSession() == null) return;
        getSession().goBack();
    }
    public void goForward() {
        if (getSession() == null) return;
        getSession().goForward();
    }

    public boolean canGoBack() {
        return navigationDelegate.canGoBack;
    }
    public boolean canGoForward() {
        return navigationDelegate.canGoForward;
    }
    public boolean clearQueued = false;
    public void backFull() {
        if (getSession() == null) return;
        getSession().gotoHistoryIndex(0);
    }
    public void forwardFull() {
        if (getSession() == null) return;
        getSession().gotoHistoryIndex(historyDelegate.historyList.size()-1);
    }
    public String getUrl() {
        return navigationDelegate.currentUrl;
    }

    public void loadUrl(String url) {
        if (getSession() == null) return;
        getSession().load(new GeckoSession.Loader().uri(url).flags(GeckoSession.LOAD_FLAGS_BYPASS_CACHE | GeckoSession.LOAD_FLAGS_FORCE_ALLOW_DATA_URI | GeckoSession.LOAD_FLAGS_BYPASS_CACHE | GeckoSession.LOAD_FLAGS_ALLOW_POPUPS));
    }
    public void reload() {
        if (getSession() == null) return;
        getSession().reload();
    }
    public void applyPageScale(float scale) {
        if (getSession() == null) return;

        String script = "(() => {"
                + "const scale = " + scale + ";"
                + "const root = document.documentElement;"
                + "const body = document.body;"
                + "const expandedWidth = (100 / scale) + '%';"
                + "const targetWidth = '100vw';"
                + "const targetMinWidth = '100vw';"
                + "const fixedSelectors = '.bottom-bar,.cp-toggle,.recovery-overlay,.loading-mask';"
                + "const wrapperId = '__tvapk_scaled_content__';"
                + "root.style.zoom = '';"
                + "root.style.width = targetWidth;"
                + "root.style.minWidth = targetMinWidth;"
                + "root.style.maxWidth = 'none';"
                + "root.style.overflowX = 'clip';"
                + "if (body) {"
                + "body.style.zoom = '';"
                + "body.style.transform = '';"
                + "body.style.transformOrigin = '';"
                + "body.style.width = targetWidth;"
                + "body.style.minWidth = targetMinWidth;"
                + "body.style.maxWidth = 'none';"
                + "body.style.margin = '0';"
                + "body.style.overflowX = 'clip';"
                + "let wrapper = document.getElementById(wrapperId);"
                + "if (!wrapper) {"
                + "wrapper = document.createElement('div');"
                + "wrapper.id = wrapperId;"
                + "const children = Array.from(body.children);"
                + "children.forEach(child => {"
                + "if (child.id !== wrapperId && !child.matches(fixedSelectors)) wrapper.appendChild(child);"
                + "});"
                + "body.insertBefore(wrapper, body.firstChild);"
                + "}"
                + "Array.from(body.children).forEach(child => {"
                + "if (child.id !== wrapperId && !child.matches(fixedSelectors)) wrapper.appendChild(child);"
                + "});"
                + "wrapper.style.transform = 'scale(' + scale + ')';"
                + "wrapper.style.transformOrigin = '0 0';"
                + "wrapper.style.width = expandedWidth;"
                + "wrapper.style.minWidth = expandedWidth;"
                + "wrapper.style.maxWidth = 'none';"
                + "wrapper.style.margin = '0';"
                + "wrapper.style.overflowX = 'clip';"
                + "document.querySelectorAll(fixedSelectors).forEach(el => {"
                + "el.style.transform = el.classList.contains('cp-toggle') ? '' : 'translateZ(0)';"
                + "el.style.left = el.classList.contains('bottom-bar') ? '0' : el.style.left;"
                + "el.style.right = el.classList.contains('bottom-bar') ? '0' : el.style.right;"
                + "el.style.bottom = el.classList.contains('bottom-bar') ? '0' : el.style.bottom;"
                + "});"
                + "}"
                + "}"
                + "})()";
        getSession().load(new GeckoSession.Loader()
                .uri("javascript:" + Uri.encode(script))
                .flags(GeckoSession.LOAD_FLAGS_BYPASS_CACHE));
    }
    public void kill() {
        GeckoSession session = getSession();
        if (session != null) {
            session.setHistoryDelegate(null);
            session.setPermissionDelegate(null);
            session.setProgressDelegate(null);
            session.setContentDelegate(null);
            session.setPromptDelegate(null);
            session.setNavigationDelegate(null);
            session.setMediaSessionDelegate(null);
            session.close();
        }
        releaseSession();
        clearActivityReferences();
    }

    private void clearActivityReferences() {
        navigationDelegate.mActivity = null;
        progressDelegate.mActivity = null;
        promptDelegate.mActivity = null;
        contentDelegate.mActivity = null;
        permissionDelegate.mActivity = null;
        mediaSessionDelegate.mActivity = null;
        // historyDelegate doesn't hold activity
    }

    // Startups

    public BrowserWebView(Context context, BrowserActivity mActivity) {
        super(context);

        GeckoSession session = new GeckoSession(KioskGeckoSettingsFactory.createSessionSettings());
        session.open(BrowserService.getRuntime());

        session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        historyDelegate = new CustomHistoryDelgate(mActivity);
        progressDelegate = new CustomProgressDelegate(mActivity);
        promptDelegate = new CustomPromptDelegate(mActivity);
        contentDelegate = new CustomContentDelegate(mActivity);
        permissionDelegate = new CustomPermissionDelegate(mActivity);
        mediaSessionDelegate = new CustomMediaSessionDelegate(mActivity);

        navigationDelegate = new CustomNavigationDelegate(mActivity);

        session.setHistoryDelegate(historyDelegate);
        session.setPermissionDelegate(permissionDelegate);
        session.setProgressDelegate(progressDelegate);
        session.setContentDelegate(contentDelegate);
        session.setPromptDelegate(promptDelegate);
        session.setNavigationDelegate(navigationDelegate);
        session.setMediaSessionDelegate(mediaSessionDelegate);
        setSession(session);
        Objects.requireNonNull(mSession).getCompositorController().setClearColor(0xFF2A2A2E);
        coverUntilFirstPaint(0xFF2A2A2E);
    }
    
    public void updateActivity(BrowserActivity mActivity) {
        navigationDelegate.mActivity = mActivity;
        progressDelegate.mActivity = mActivity;
        promptDelegate.mActivity = mActivity;
        contentDelegate.mActivity = mActivity;
        permissionDelegate.mActivity = mActivity;
        mediaSessionDelegate.mActivity = mActivity;
    }

    public void setActive(boolean active) {
        if (getSession() != null) getSession().setActive(active);
    }

    public GeckoSession.PromptDelegate getPromptDelegate() {
        return promptDelegate;
    }
}

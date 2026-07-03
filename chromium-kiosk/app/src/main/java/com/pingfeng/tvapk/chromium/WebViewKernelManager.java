package com.pingfeng.tvapk.chromium;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.WebView;

import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradeAssetSource;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class WebViewKernelManager {
    public static final String KERNEL_ASSET_NAME = "119.0.6045.53_min24_arm32.apk";
    public static final String KERNEL_ASSET_VERSION = "119.0.6045.53-min24-arm32";

    private static volatile String status = "未开始";
    private static volatile String errorMessage = "";
    private static volatile float progress = 0f;
    private static final Set<ReadyCallback> READY_CALLBACKS = new CopyOnWriteArraySet<>();

    private WebViewKernelManager() {
    }

    public interface ReadyCallback {
        void onReady(boolean success);
    }

    public static final class KernelStatus {
        public final String systemPackageName;
        public final String systemVersion;
        public final int systemChromiumMajor;
        public final String embeddedPackageName;
        public final String embeddedVersion;
        public final int embeddedChromiumMajor;
        public final String activePackageName;
        public final String activeVersion;
        public final int activeChromiumMajor;
        public final String upgradeStatus;
        public final int upgradeProgress;
        public final String upgradeError;
        public final String userAgent;

        private KernelStatus(
                String systemPackageName,
                String systemVersion,
                int systemChromiumMajor,
                String embeddedPackageName,
                String embeddedVersion,
                int embeddedChromiumMajor,
                String activePackageName,
                String activeVersion,
                int activeChromiumMajor,
                String upgradeStatus,
                int upgradeProgress,
                String upgradeError,
                String userAgent
        ) {
            this.systemPackageName = nullToDash(systemPackageName);
            this.systemVersion = nullToDash(systemVersion);
            this.systemChromiumMajor = systemChromiumMajor;
            this.embeddedPackageName = nullToDash(embeddedPackageName);
            this.embeddedVersion = nullToDash(embeddedVersion);
            this.embeddedChromiumMajor = embeddedChromiumMajor;
            this.activePackageName = nullToDash(activePackageName);
            this.activeVersion = nullToDash(activeVersion);
            this.activeChromiumMajor = activeChromiumMajor;
            this.upgradeStatus = nullToDash(upgradeStatus);
            this.upgradeProgress = upgradeProgress;
            this.upgradeError = upgradeError == null ? "" : upgradeError;
            this.userAgent = nullToDash(userAgent);
        }
    }

    public static void install(Application application) {
        if (application == null) {
            status = "失败";
            errorMessage = "Application 为空";
            return;
        }
        if (!isMainProcess(application)) {
            status = "跳过子进程";
            return;
        }
        status = "准备替换内核";
        WebViewUpgrade.addUpgradeCallback(new UpgradeCallback() {
            @Override
            public void onUpgradeProcess(float percent) {
                progress = percent;
                status = "替换中 " + Math.round(percent * 100f) + "%";
            }

            @Override
            public void onUpgradeComplete() {
                progress = 1f;
                status = "替换成功";
                errorMessage = "";
                notifyReady(true);
            }

            @Override
            public void onUpgradeError(Throwable throwable) {
                status = "替换失败";
                errorMessage = throwable == null ? "未知错误" : String.valueOf(throwable.getMessage());
                notifyReady(false);
            }
        });
        try {
            WebViewUpgrade.upgrade(new UpgradeAssetSource(application, KERNEL_ASSET_NAME, KERNEL_ASSET_VERSION));
        } catch (Throwable throwable) {
            status = "替换失败";
            errorMessage = throwable.getMessage() == null ? throwable.toString() : throwable.getMessage();
            notifyReady(false);
        }
    }

    public static void runWhenReady(ReadyCallback callback) {
        if (callback == null) {
            return;
        }
        if (safeIsCompleted()) {
            callback.onReady(true);
            return;
        }
        if (safeIsFailed()) {
            callback.onReady(false);
            return;
        }
        READY_CALLBACKS.add(callback);
    }

    public static boolean isCompleted() {
        return safeIsCompleted();
    }

    public static boolean isFailed() {
        return safeIsFailed();
    }

    public static KernelStatus getKernelStatus(String userAgent) {
        String systemPackageName = safeGetSystemWebViewPackageName();
        String systemVersion = safeGetSystemWebViewPackageVersion();
        String activePackageName = resolveCurrentPackageName();
        String activeVersion = resolveCurrentPackageVersion();
        return new KernelStatus(
                systemPackageName,
                systemVersion,
                KernelInfoParser.extractChromiumMajor(systemVersion),
                "com.google.android.webview",
                "119.0.6045.53",
                119,
                activePackageName,
                activeVersion,
                resolveActiveChromiumMajor(activeVersion, userAgent),
                status,
                Math.round(progress * 100f),
                errorMessage,
                userAgent
        );
    }

    public static String buildStatus(Context context, String userAgent) {
        KernelStatus kernelStatus = getKernelStatus(userAgent);
        StringBuilder builder = new StringBuilder();
        builder.append("App: tvapk-chromium119\n");
        builder.append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        builder.append("ABI: ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown").append("\n");
        builder.append("Kernel asset: ").append(KERNEL_ASSET_NAME).append("\n");
        builder.append("Upgrade status: ").append(kernelStatus.upgradeStatus).append("\n");
        builder.append("Upgrade progress: ").append(kernelStatus.upgradeProgress).append("%\n");
        if (!kernelStatus.upgradeError.isEmpty()) {
            builder.append("Upgrade error: ").append(kernelStatus.upgradeError).append("\n");
        }
        builder.append("System WebView: ")
                .append(kernelStatus.systemPackageName)
                .append(" / ")
                .append(kernelStatus.systemVersion)
                .append(" / Chromium ")
                .append(formatMajor(kernelStatus.systemChromiumMajor))
                .append("\n");
        builder.append("Embedded WebView: ")
                .append(kernelStatus.embeddedPackageName)
                .append(" / ")
                .append(kernelStatus.embeddedVersion)
                .append(" / Chromium ")
                .append(formatMajor(kernelStatus.embeddedChromiumMajor))
                .append("\n");
        builder.append("Active WebView: ")
                .append(kernelStatus.activePackageName)
                .append(" / ")
                .append(kernelStatus.activeVersion)
                .append(" / Chromium ")
                .append(formatMajor(kernelStatus.activeChromiumMajor))
                .append("\n");
        builder.append("UserAgent: ").append(kernelStatus.userAgent).append("\n");
        return builder.toString();
    }

    private static void notifyReady(boolean success) {
        for (ReadyCallback callback : READY_CALLBACKS) {
            if (callback == null) continue;
            callback.onReady(success);
        }
        READY_CALLBACKS.clear();
    }

    private static String resolveCurrentPackageName() {
        String upgradeName = safeGetUpgradeWebViewPackageName();
        if (upgradeName != null && !upgradeName.isEmpty()) return upgradeName;

        PackageInfo packageInfo = currentWebViewPackage();
        return packageInfo == null ? null : packageInfo.packageName;
    }

    private static String resolveCurrentPackageVersion() {
        String upgradeVersion = safeGetUpgradeWebViewVersion();
        if (upgradeVersion != null && !upgradeVersion.isEmpty()) return upgradeVersion;

        PackageInfo packageInfo = currentWebViewPackage();
        return packageInfo == null ? null : packageInfo.versionName;
    }

    private static PackageInfo currentWebViewPackage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        try {
            return WebView.getCurrentWebViewPackage();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isMainProcess(Context context) {
        String packageName = context.getPackageName();
        int pid = android.os.Process.myPid();
        android.app.ActivityManager manager =
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null || manager.getRunningAppProcesses() == null) {
            return true;
        }
        for (android.app.ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                return packageName.equals(process.processName);
            }
        }
        return true;
    }

    private static String nullToDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private static int resolveActiveChromiumMajor(String activeVersion, String userAgent) {
        int versionMajor = KernelInfoParser.extractChromiumMajor(activeVersion);
        if (versionMajor > 0) {
            return versionMajor;
        }
        return KernelInfoParser.extractChromiumMajor(userAgent);
    }

    private static String formatMajor(int major) {
        return major > 0 ? String.valueOf(major) : "-";
    }

    private static boolean safeIsCompleted() {
        try {
            return WebViewUpgrade.isCompleted();
        } catch (Throwable throwable) {
            recordDiagnosticError("读取替换完成状态失败", throwable);
            return false;
        }
    }

    private static boolean safeIsFailed() {
        try {
            return WebViewUpgrade.isFailed();
        } catch (Throwable throwable) {
            recordDiagnosticError("读取替换失败状态失败", throwable);
            return true;
        }
    }

    private static String safeGetSystemWebViewPackageName() {
        try {
            return WebViewUpgrade.getSystemWebViewPackageName();
        } catch (Throwable throwable) {
            recordDiagnosticError("读取系统 WebView 包名失败", throwable);
            return null;
        }
    }

    private static String safeGetSystemWebViewPackageVersion() {
        try {
            return WebViewUpgrade.getSystemWebViewPackageVersion();
        } catch (Throwable throwable) {
            recordDiagnosticError("读取系统 WebView 版本失败", throwable);
            return null;
        }
    }

    private static String safeGetUpgradeWebViewPackageName() {
        try {
            return WebViewUpgrade.getUpgradeWebViewPackageName();
        } catch (Throwable throwable) {
            recordDiagnosticError("读取替换 WebView 包名失败", throwable);
            return null;
        }
    }

    private static String safeGetUpgradeWebViewVersion() {
        try {
            return WebViewUpgrade.getUpgradeWebViewVersion();
        } catch (Throwable throwable) {
            recordDiagnosticError("读取替换 WebView 版本失败", throwable);
            return null;
        }
    }

    private static void recordDiagnosticError(String prefix, Throwable throwable) {
        String message = throwable == null || throwable.getMessage() == null
                ? String.valueOf(throwable)
                : throwable.getMessage();
        errorMessage = prefix + ": " + message;
        if (status == null || status.isEmpty() || "未开始".equals(status)) {
            status = "诊断异常";
        }
    }
}

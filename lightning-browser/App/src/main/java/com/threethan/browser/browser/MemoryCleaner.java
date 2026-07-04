package com.threethan.browser.browser;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.threethan.browser.diagnostics.AppDiagnostics;

import org.mozilla.geckoview.GeckoRuntime;

import java.lang.ref.WeakReference;

/**
 * 定时清理 GeckoView 内存，防止长时间运行后 OOM 崩溃。
 * 在 kiosk（长时间挂机）场景下尤为重要。
 */
public final class MemoryCleaner {
    private static final String TAG = "MemoryCleaner";

    /** 每隔 6 小时触发一次内存清理 */
    private static final long CLEAN_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    /** 内存占用阈值（MB），超过则立即触发清理 */
    private static final long MEMORY_THRESHOLD_MB = 400L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WeakReference<BrowserService> serviceRef;
    private final Runnable cleanerRunnable;

    public MemoryCleaner(BrowserService service) {
        this.serviceRef = new WeakReference<>(service);
        this.cleanerRunnable = new Runnable() {
            @Override
            public void run() {
                BrowserService s = serviceRef.get();
                if (s == null) return;
                performMemoryClean();
                handler.postDelayed(this, CLEAN_INTERVAL_MS);
            }
        };
    }

    public void start() {
        handler.postDelayed(cleanerRunnable, CLEAN_INTERVAL_MS);
    }

    public void stop() {
        handler.removeCallbacks(cleanerRunnable);
    }

    public void checkAndCleanIfNeeded() {
        long usedMb = getMemoryUsedMb();
        if (usedMb > MEMORY_THRESHOLD_MB) {
            Log.w(TAG, "Memory threshold exceeded: " + usedMb + "MB > " + MEMORY_THRESHOLD_MB + "MB, triggering clean");
            performMemoryClean();
        }
    }

    private long getMemoryUsedMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
    }

    private void performMemoryClean() {
        BrowserService service = serviceRef.get();
        if (service == null) return;

        Log.i(TAG, "Performing periodic memory clean");
        AppDiagnostics.record(service, "MEMORY_CLEAN", "triggered");

        // 1. 释放未使用的内存
        System.gc();

        // 2. 尝试清理 GeckoRuntime 存储
        try {
            GeckoRuntime runtime = BrowserService.getRuntime();
            if (runtime != null) {
                // GeckoView 144: 使用 0xFFFF (ALL) 清理所有存储数据
                runtime.getStorageController().clearData(0xFFFF);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear GeckoRuntime storage", e);
        }

        // 3. 再次触发垃圾回收
        System.gc();

        // 4. 记录清理后的内存状态
        long afterClean = getMemoryUsedMb();
        AppDiagnostics.record(service, "MEMORY_CLEAN_DONE", "after=" + afterClean + "MB");
    }
}

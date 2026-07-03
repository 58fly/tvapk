package com.threethan.browser.diagnostics;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

public class DiagnosticsApplication extends Application {
    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            AppDiagnostics.markRuntimeState(
                    this,
                    "CRASH thread=" + thread.getName() + ", " + AppDiagnostics.throwableSummary(throwable)
            );
            AppDiagnostics.record(
                    this,
                    "CRASH",
                    "thread=" + thread.getName() + ", " + AppDiagnostics.throwableSummary(throwable)
            );
            if (defaultExceptionHandler != null) {
                defaultExceptionHandler.uncaughtException(thread, throwable);
            }
        });
        AppDiagnostics.recordProcessStart(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        AppDiagnostics.markRuntimeState(this, "LOW_MEMORY");
        AppDiagnostics.record(this, "LOW_MEMORY", "系统触发 onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            AppDiagnostics.markRuntimeState(this, "TRIM_MEMORY level=" + level);
            AppDiagnostics.record(this, "TRIM_MEMORY", "level=" + level);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppDiagnostics.record(this, "CONFIG_CHANGED", "orientation=" + newConfig.orientation);
    }
}

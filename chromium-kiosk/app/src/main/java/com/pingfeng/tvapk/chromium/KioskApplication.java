package com.pingfeng.tvapk.chromium;

import android.app.Application;

public class KioskApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        WebViewKernelManager.install(this);
    }
}

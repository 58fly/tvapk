# Chromium Kiosk Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建独立极简 Android TV APK，内置 Chromium 119 WebView 内核，启动后全屏加载 `http://47.94.161.17:3000/wl.html`。

**Architecture:** 新建独立 Gradle Android 项目 `/Volumes/data/tvapk/chromium-kiosk`，不复用 `LightningBrowser` 的 GeckoView 浏览器主体。启动时在 `Application` 中调用 `WebViewUpgrade`，再由单 Activity 创建 `android.webkit.WebView` 全屏展示业务页面，并提供最小诊断层。

**Tech Stack:** Android Gradle Plugin 8.3.1、Java、Android WebView、WebViewUpgrade core、内置 Google WebView `119.0.6045.53_min24_arm32.apk`。

## Global Constraints

- 固定 URL：`http://47.94.161.17:3000/wl.html`。
- 最低系统：Android 7.0，对应 `minSdkVersion 24`。
- 内核 APK：`google/119.0.6045.53_min24_arm32.apk`。
- 输出产物：`/Volumes/data/tvapk/dist/tvapk-chromium119.apk`。
- 第一版只做 POC，不做多内核热切换。
- WebViewUpgrade 必须早于任何 `android.webkit.WebView` 创建。
- 不修改现有 `/Volumes/data/tvapk/lightning-browser` GeckoView 主线代码。

---

## File Structure

- Create: `/Volumes/data/tvapk/chromium-kiosk/settings.gradle`
- Create: `/Volumes/data/tvapk/chromium-kiosk/build.gradle`
- Create: `/Volumes/data/tvapk/chromium-kiosk/gradle.properties`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/build.gradle`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/AndroidManifest.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/assets/119.0.6045.53_min24_arm32.apk`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/KioskApplication.java`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/KioskActivity.java`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/KernelInfoParser.java`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/WebViewKernelManager.java`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/values/strings.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/README.md`
- Create: `/Volumes/data/tvapk/chromium-kiosk/CHANGELOG.md`

## Task 1: Scaffold Android Project

**Files:**
- Create Gradle project files and manifest.

**Interfaces:**
- Produces: Android app package `com.pingfeng.tvapk.chromium`.

- [ ] **Step 1: Create project structure**

Run:

```bash
mkdir -p /Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium
mkdir -p /Volumes/data/tvapk/chromium-kiosk/app/src/main/res/values
mkdir -p /Volumes/data/tvapk/chromium-kiosk/app/src/main/assets
```

- [ ] **Step 2: Write Gradle files**

Create `settings.gradle`, root `build.gradle`, `gradle.properties`, and `app/build.gradle` with Android plugin, Java 17, `minSdkVersion 24`, `targetSdkVersion 35`, and `implementation 'io.github.jonanorman.android.webviewup:core:0.1.0'`.

- [ ] **Step 3: Write manifest**

Create `AndroidManifest.xml` with `KioskApplication`, `KioskActivity`, internet permission, cleartext traffic enabled, launcher and leanback launcher entries.

- [ ] **Step 4: Verify Gradle model**

Run:

```bash
cd /Volumes/data/tvapk/chromium-kiosk
JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home \
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
gradle -q projects
```

Expected: project contains `:app`.

## Task 2: Add Kernel Asset

**Files:**
- Create: `app/src/main/assets/119.0.6045.53_min24_arm32.apk`

**Interfaces:**
- Produces: asset name constant `119.0.6045.53_min24_arm32.apk`.

- [ ] **Step 1: Download kernel APK**

Run:

```bash
curl -L \
  -o /Volumes/data/tvapk/chromium-kiosk/app/src/main/assets/119.0.6045.53_min24_arm32.apk \
  https://github.com/JonaNorman/WebViewPackage/releases/download/google/119.0.6045.53_min24_arm32.apk
```

- [ ] **Step 2: Verify asset**

Run:

```bash
ls -lh /Volumes/data/tvapk/chromium-kiosk/app/src/main/assets/119.0.6045.53_min24_arm32.apk
shasum -a 256 /Volumes/data/tvapk/chromium-kiosk/app/src/main/assets/119.0.6045.53_min24_arm32.apk
```

Expected: file size about `80MB`.

## Task 3: Implement Kernel Manager and Parser

**Files:**
- Create: `KernelInfoParser.java`
- Create: `WebViewKernelManager.java`

**Interfaces:**
- Produces: `KernelInfoParser.extractChromiumMajor(String userAgent): int`
- Produces: `WebViewKernelManager.install(Application application): void`
- Produces: `WebViewKernelManager.buildStatus(Context context, String userAgent): String`

- [ ] **Step 1: Create parser**

Implement Chromium major version extraction from UserAgent.

- [ ] **Step 2: Create kernel manager**

Use `UpgradeAssetSource(context, ASSET_NAME, ASSET_VERSION)` and `WebViewUpgrade.upgrade(source)`. Record callback status in static fields.

## Task 4: Implement Application and Activity

**Files:**
- Create: `KioskApplication.java`
- Create: `KioskActivity.java`

**Interfaces:**
- Consumes: `WebViewKernelManager.install(...)`
- Consumes: `WebViewKernelManager.buildStatus(...)`

- [ ] **Step 1: Initialize kernel early**

`KioskApplication.onCreate()` calls `WebViewKernelManager.install(this)` before any WebView.

- [ ] **Step 2: Build fullscreen WebView activity**

`KioskActivity` creates `WebView` programmatically, configures JavaScript, DOM storage, mixed content, viewport, focus, and loads fixed URL.

- [ ] **Step 3: Add hidden diagnostics**

Menu key or long press center toggles a small diagnostic overlay with UserAgent and kernel status.

## Task 5: Build and Publish APK

**Files:**
- Output: `/Volumes/data/tvapk/dist/tvapk-chromium119.apk`
- Create: `README.md`
- Create: `CHANGELOG.md`

- [ ] **Step 1: Build debug APK**

Run:

```bash
cd /Volumes/data/tvapk/chromium-kiosk
JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home \
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
gradle :app:assembleDebug
```

- [ ] **Step 2: Copy artifact**

Run:

```bash
mkdir -p /Volumes/data/tvapk/dist
cp /Volumes/data/tvapk/chromium-kiosk/app/build/outputs/apk/debug/app-debug.apk \
  /Volumes/data/tvapk/dist/tvapk-chromium119.apk
```

- [ ] **Step 3: Verify APK**

Run:

```bash
shasum -a 256 /Volumes/data/tvapk/dist/tvapk-chromium119.apk
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
PATH=/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/37.0.0:$PATH \
apksigner verify --verbose /Volumes/data/tvapk/dist/tvapk-chromium119.apk
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
PATH=/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/37.0.0:$PATH \
aapt dump badging /Volumes/data/tvapk/dist/tvapk-chromium119.apk | sed -n '1,80p'
```

- [ ] **Step 4: Update docs**

Record artifact path, SHA256, kernel asset version, build command, and known risks in `README.md` and `CHANGELOG.md`.

## Self-Review

- Spec coverage: fixed URL, Android 7.0, Chromium 119 asset, early WebViewUpgrade, diagnostics, artifact name, docs and verification are covered.
- Placeholder scan: no TBD/TODO placeholders.
- Type consistency: parser and manager method names are defined before activity consumption.

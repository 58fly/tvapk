# Chromium UI Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将旧版 TV 页面设计、缩放控制和设置弹层移植到 `tvapk-chromium119`，同时保留 Chromium119 内核方案。

**Architecture:** 新版继续使用 `android.webkit.WebView` 和 `WebViewUpgrade`。旧版 UI 只迁移外层 kiosk 控件、设置数据模型和可保存配置，不迁移 GeckoView、书签、扩展、权限管理和多标签逻辑。

**Tech Stack:** Android Java、XML layout、Android WebView、WebViewUpgrade、Gradle。

## Global Constraints

- 产物名称固定为 `/Volumes/data/tvapk/dist/tvapk-chromium119.apk`。
- 包名保持 `com.pingfeng.tvapk.chromium`。
- 默认地址保持 `http://47.94.161.17:3000/wl.html`。
- 内置内核保持 `119.0.6045.53_min24_arm32.apk`。
- 最低系统保持 Android 7.0，`minSdkVersion=24`。
- 不引入 GeckoView、扩展管理、书签管理和旧版权限管理。
- 所有可保存配置写入应用私有目录，失败时给出界面提示并保留当前页面。

---

### Task 1: 迁移旧版 kiosk 设置模型

**Files:**
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/kiosk/KioskSettings.java`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/kiosk/KioskSettingsController.java`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/kiosk/KioskSettingsStore.java`

**Interfaces:**
- Produces: `KioskSettings.defaults(String versionName, int versionCode)`、`KioskSettings.getScale()`、`KioskSettingsStore.load()`、`KioskSettingsStore.save(KioskSettings)`、`KioskSettingsController` 草稿编辑流程。

- [ ] 复制旧版设置模型并改包名为 `com.pingfeng.tvapk.chromium.kiosk`。
- [ ] 默认 URL 固定为 `http://47.94.161.17:3000/wl.html`。
- [ ] 保持缩放范围 `10% - 100%`，步进 `10%`。
- [ ] 读取配置失败时返回默认配置，不中断启动。

### Task 2: 迁移 UI 资源

**Files:**
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/layout/activity_kiosk.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/layout/kiosk_settings_overlay.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/drawable/bkg_button_web.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/drawable/bkg_favicon.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/drawable/bkg_kiosk_field.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/drawable/bkg_kiosk_overlay_button.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/drawable/bkg_kiosk_overlay_button_exit.xml`
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/drawable/bkg_kiosk_settings_panel.xml`
- Modify: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Android resource ids used by `KioskActivity` and `KioskSettingsOverlay`。
- Produces: 顶栏、WebView 容器、错误栏、诊断栏、设置弹层。

- [ ] 新建 `activity_kiosk.xml`，包含顶部控制条、WebView 容器、错误栏、诊断栏和设置弹层 include。
- [ ] 顶栏保留刷新、缩小、缩放比例、放大、URL 展示、设置、退出。
- [ ] 使用文字按钮替代旧版问题图标，规避 Android TV 内置图标显示异常。
- [ ] 新增中文字符串：刷新、设置、退出、页面缩放、保存、取消、恢复默认。

### Task 3: 迁移设置弹层控制器

**Files:**
- Create: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/kiosk/KioskSettingsOverlay.java`

**Interfaces:**
- Consumes: `KioskSettings`。
- Produces: `KioskSettingsOverlay.Listener` 回调，包括预览缩放、保存、取消、恢复默认、退出。

- [ ] 复制旧版设置弹层控制器并改包名和 `R` 引用。
- [ ] 保存按钮读取 URL 和 SeekBar 缩放值。
- [ ] 取消按钮恢复当前已保存配置。
- [ ] 恢复默认按钮回填默认 URL 和 `100%`。

### Task 4: 改造 Chromium KioskActivity

**Files:**
- Modify: `/Volumes/data/tvapk/chromium-kiosk/app/src/main/java/com/pingfeng/tvapk/chromium/KioskActivity.java`

**Interfaces:**
- Consumes: `KioskSettingsStore`、`KioskSettingsController`、`KioskSettingsOverlay`。
- Produces: WebView 全屏展示、顶栏交互、缩放注入、设置保存。

- [ ] 从程序化 UI 改为加载 `R.layout.activity_kiosk`。
- [ ] WebView 加入 `R.id.webContainer`。
- [ ] 初始化顶部按钮：刷新、缩小、放大、URL 点击打开设置、设置、退出。
- [ ] 页面完成后调用 `applyPageScale(settings.getScale())`。
- [ ] 缩放使用 `evaluateJavascript` 注入 `document.documentElement/body` 的 `transform: scale(...)`。
- [ ] 菜单键和设置键打开设置弹层，返回键优先关闭弹层或诊断层。
- [ ] 保存 URL 后立即加载新地址，保存缩放后立即预览并持久化。

### Task 5: 构建、校验、文档

**Files:**
- Modify: `/Volumes/data/tvapk/chromium-kiosk/README.md`
- Modify: `/Volumes/data/tvapk/chromium-kiosk/CHANGELOG.md`
- Output: `/Volumes/data/tvapk/dist/tvapk-chromium119.apk`

**Interfaces:**
- Produces: 可安装 APK 和静态校验记录。

- [ ] 运行 `JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk ./gradlew :app:assembleDebug`。
- [ ] 复制 `app/build/outputs/apk/debug/app-debug.apk` 到 `/Volumes/data/tvapk/dist/tvapk-chromium119.apk`。
- [ ] 运行 `apksigner verify --verbose`。
- [ ] 运行 `aapt dump badging`。
- [ ] 运行 `shasum -a 256`。
- [ ] 更新 README 和 CHANGELOG 的 UI、缩放和校验记录。

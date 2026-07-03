# TV Kiosk Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前顶部悬浮控制条改成电视直播风格半透明设置菜单，支持实时预览缩放、编辑网页地址、保存到配置文件，并保留版本号。

**Architecture:** 保留 GeckoView 主渲染链路和现有缩放实现，但把控制入口从“顶部栏”改为“半透明设置层”。设置层打开时读取当前配置，修改时即时应用到页面，保存后写回本地存储，下次启动从配置启动。页面顶部不再承担设置职责，只保留内容展示。

**Tech Stack:** Android View 系统、GeckoView、Java、JUnit4、Gradle、app 私有目录 `kiosk_settings.properties` 配置文件

---

### Task 1: 建配置模型

**Files:**
- Create: `App/src/main/java/com/threethan/browser/kiosk/KioskSettings.java`
- Create: `App/src/main/java/com/threethan/browser/kiosk/KioskSettingsStore.java`
- Test: `App/src/test/java/com/threethan/browser/kiosk/KioskSettingsStoreTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
public void saveAndLoadRoundTripsUrlAndZoom() {
    KioskSettingsStore store = new KioskSettingsStore(tempFile);
    KioskSettings settings = new KioskSettings("http://example.com", 60, "1.3.2", 1302, 123L);
    store.save(settings);

    KioskSettings loaded = store.load();

    assertEquals("http://example.com", loaded.targetUrl);
    assertEquals(60, loaded.zoomPercent);
    assertEquals("1.3.2", loaded.versionName);
    assertEquals(1302, loaded.versionCode);
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :App:testDebugUnitTest --tests com.threethan.browser.kiosk.KioskSettingsStoreTest`
Expected: FAIL，因为类未实现。

- [ ] **Step 3: 写最小实现**

```java
public final class KioskSettings {
    public final String targetUrl;
    public final int zoomPercent;
    public final String versionName;
    public final int versionCode;
    public final long updatedAt;

    public KioskSettings(String targetUrl, int zoomPercent, String versionName, int versionCode, long updatedAt) {
        this.targetUrl = targetUrl;
        this.zoomPercent = zoomPercent;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.updatedAt = updatedAt;
    }
}
```

```java
public final class KioskSettingsStore {
    public static final String FILE_NAME = "kiosk_settings.properties";
    private final File file;

    public KioskSettingsStore(Context context) {
        this(new File(context.getFilesDir(), FILE_NAME));
    }

    public KioskSettingsStore(File file) {
        this.file = file;
    }

    public void save(KioskSettings settings) throws IOException {
        Properties properties = settings.toProperties();
        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, "TV kiosk settings");
        }
    }

    public KioskSettings load() {
        // 读取失败或字段非法时回退默认配置。
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :App:testDebugUnitTest --tests com.threethan.browser.kiosk.KioskSettingsStoreTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add App/src/main/java/com/threethan/browser/kiosk/KioskSettings.java App/src/main/java/com/threethan/browser/kiosk/KioskSettingsStore.java App/src/test/java/com/threethan/browser/kiosk/KioskSettingsStoreTest.java
git commit -m "feat: add kiosk settings store"
```

### Task 2: 做设置控制器

**Files:**
- Create: `App/src/main/java/com/threethan/browser/kiosk/KioskSettingsController.java`
- Test: `App/src/test/java/com/threethan/browser/kiosk/KioskSettingsControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
public void cancelRestoresDraftValues() {
    KioskSettingsController controller = new KioskSettingsController(defaultSettings);
    controller.beginEdit();
    controller.setDraftUrl("http://new.example");
    controller.setDraftZoomPercent(60);
    controller.cancel();

    assertEquals("http://old.example", controller.getActiveSettings().targetUrl);
    assertEquals(100, controller.getActiveSettings().zoomPercent);
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :App:testDebugUnitTest --tests com.threethan.browser.kiosk.KioskSettingsControllerTest`
Expected: FAIL。

- [ ] **Step 3: 写最小实现**

```java
public final class KioskSettingsController {
    private final KioskSettings baseline;
    private KioskSettings draft;

    public KioskSettingsController(KioskSettings baseline) {
        this.baseline = baseline;
        this.draft = baseline;
    }

    public void beginEdit() { draft = baseline; }
    public void setDraftUrl(String url) { draft = new KioskSettings(url, draft.zoomPercent, draft.versionName, draft.versionCode, draft.updatedAt); }
    public void setDraftZoomPercent(int zoomPercent) { draft = new KioskSettings(draft.targetUrl, zoomPercent, draft.versionName, draft.versionCode, draft.updatedAt); }
    public KioskSettings getActiveSettings() { return draft; }
    public KioskSettings commit(long updatedAt) { draft = new KioskSettings(draft.targetUrl, draft.zoomPercent, draft.versionName, draft.versionCode, updatedAt); return draft; }
    public void cancel() { draft = baseline; }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :App:testDebugUnitTest --tests com.threethan.browser.kiosk.KioskSettingsControllerTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add App/src/main/java/com/threethan/browser/kiosk/KioskSettingsController.java App/src/test/java/com/threethan/browser/kiosk/KioskSettingsControllerTest.java
git commit -m "feat: add kiosk settings controller"
```

### Task 3: 做半透明设置面板

**Files:**
- Create: `App/src/main/res/layout/dialog_kiosk_settings.xml`
- Create: `App/src/main/res/drawable/bkg_kiosk_overlay.xml`
- Create: `App/src/main/java/com/threethan/browser/kiosk/KioskSettingsDialog.java`
- Modify: `App/src/main/res/layout/activity_browser.xml`

- [ ] **Step 1: 先写布局测试/截图验证点**

不写自动化 UI 测试，先用布局 XML 验证以下项：

```xml
<FrameLayout
    android:background="@drawable/bkg_kiosk_overlay"
    android:clickable="true"
    android:focusable="true">
```

- [ ] **Step 2: 写最小布局**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#88000000"
    android:clickable="true"
    android:focusable="true">

    <LinearLayout
        android:layout_width="720dp"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:orientation="vertical"
        android:padding="24dp"
        android:background="#E6000000">

        <!-- URL row -->
        <!-- Zoom row -->
        <!-- Save / Cancel / Default -->
        <!-- Version footer -->
    </LinearLayout>
</FrameLayout>
```

- [ ] **Step 3: 接入打开/关闭逻辑**

```java
public void showSettingsOverlay();
public void hideSettingsOverlay();
public void applyPreviewSettings();
```

- [ ] **Step 4: 验证半透明效果**

Run: `./gradlew :App:assembleDebug`
Expected: build pass, layout on device shows page still visible under overlay.

- [ ] **Step 5: 提交**

```bash
git add App/src/main/res/layout/dialog_kiosk_settings.xml App/src/main/res/drawable/bkg_kiosk_overlay.xml App/src/main/java/com/threethan/browser/kiosk/KioskSettingsDialog.java App/src/main/res/layout/activity_browser.xml
git commit -m "feat: add kiosk settings overlay"
```

### Task 4: 接实时预览和保存

**Files:**
- Modify: `App/src/main/java/com/threethan/browser/browser/BrowserActivity.java`
- Modify: `App/src/main/java/com/threethan/browser/browser/GeckoView/BrowserWebView.java`
- Modify: `App/src/main/java/com/threethan/browser/browser/GeckoView/Delegate/CustomNavigationDelegate.java`

- [ ] **Step 1: 写实时预览测试**

```java
@Test
public void previewZoomAppliesImmediately() {
    // 断言草稿 zoom 变化会触发 applyPageScale
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :App:testDebugUnitTest --tests com.threethan.browser.kiosk.KioskSettingsControllerTest`
Expected: FAIL until preview hook is implemented.

- [ ] **Step 3: 写最小实现**

```java
public void onZoomChanged(int zoomPercent) {
    settingsController.setDraftZoomPercent(zoomPercent);
    browserWebView.applyPageScale(zoomPercent / 100f);
}

public void onUrlEdited(String url) {
    settingsController.setDraftUrl(url);
}

public void onSaveClicked() {
    KioskSettings committed = settingsController.commit(System.currentTimeMillis());
    settingsStore.save(committed);
    browserWebView.loadUrl(committed.targetUrl);
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :App:testDebugUnitTest :App:assembleDebug`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add App/src/main/java/com/threethan/browser/browser/BrowserActivity.java App/src/main/java/com/threethan/browser/browser/GeckoView/BrowserWebView.java App/src/main/java/com/threethan/browser/browser/GeckoView/Delegate/CustomNavigationDelegate.java
git commit -m "feat: wire kiosk settings preview"
```

### Task 5: 顶层入口和版本显示

**Files:**
- Modify: `App/src/main/java/com/threethan/browser/wrapper/WrapperActivity.java`
- Modify: `App/src/main/java/com/threethan/browser/browser/BrowserActivity.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: 写入口测试**

```java
@Test
public void wrapperLaunchesKioskWithPersistedUrl() {
    // 断言启动读取配置后打开保存地址
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :App:testDebugUnitTest`
Expected: FAIL until persisted start path is wired.

- [ ] **Step 3: 写最小实现**

```java
Intent intent = new Intent(this, BrowserActivity.class);
intent.putExtra("isKiosk", true);
intent.putExtra("targetUrl", settings.targetUrl);
intent.putExtra("zoomPercent", settings.zoomPercent);
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :App:testDebugUnitTest :App:assembleDebug`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add App/src/main/java/com/threethan/browser/wrapper/WrapperActivity.java App/src/main/java/com/threethan/browser/browser/BrowserActivity.java README.md CHANGELOG.md
git commit -m "feat: persist kiosk launch settings"
```

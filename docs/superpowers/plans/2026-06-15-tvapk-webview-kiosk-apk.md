# Tvapk Webview Kiosk APK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 `nktnet1/webview-kiosk` 最小改动生成一个可安装 APK，启动后默认访问 `http://47.94.161.17:3000/wl.html`。

**Architecture:** 直接克隆上游仓库，保留原 WebView/Kiosk 架构，只修改默认首页配置与必要品牌/包名配置。优先使用 debug APK 交付可安装版本；如需要长期分发，再加本地 release keystore 签名。

**Tech Stack:** Android Gradle Plugin、Kotlin、Jetpack Compose、Android WebView、Gradle Wrapper。

---

## 难度评估

难度：中低。

主要难点不在功能实现。上游已支持 WebView、Kiosk、Launcher、Android 7.0。风险集中在四点：

- 构建环境需要可用 JDK 与 Android SDK。
- 首次 Gradle 构建需要下载依赖，国内网络可能慢。
- 目标页面是 `http://` 明文地址，需要确认 WebView 网络安全配置允许访问。
- 生成 APK 若用 debug 签名可直接安装测试；正式分发需 release 签名。

## 最小改动策略

只做必要改动：

- 改默认首页：`app/src/main/java/uk/nktnet/webviewkiosk/config/Constants.kt` 或 `UserSettings.kt` 默认 `homeUrl`。
- 如上游已有 `https://webviewkiosk.nktnet.uk` 默认值，则替换为 `http://47.94.161.17:3000/wl.html`。
- 检查 `app/src/main/res/xml/network_security_config.xml`，确保允许该 IP 明文 HTTP。
- 生成 APK：优先 `./gradlew assembleDebug`。
- 输出产物复制到 `dist/tvapk-webview-kiosk-debug.apk`。

## 文件结构

**拉取后项目目录：** `/Volumes/data/tvapk/webview-kiosk`

**修改文件：**

- `app/src/main/java/uk/nktnet/webviewkiosk/config/Constants.kt`  
  统一默认 URL 常量，便于后续维护。

- `app/src/main/java/uk/nktnet/webviewkiosk/config/UserSettings.kt`  
  如默认首页直接写在 `homeUrl` 初始化处，则改这里。

- `app/src/main/res/xml/network_security_config.xml`  
  检查或增加对 `47.94.161.17` 的 cleartext 放行。

- `app/build.gradle.kts`  
  只在需要区分原应用时修改 `applicationId`、`versionName`、`versionCode`。第一版可不改。

- `dist/`  
  存放最终 APK。

## 实施步骤

### Task 1: 拉取上游源码

**Files:**
- Create: `/Volumes/data/tvapk/webview-kiosk`

- [ ] **Step 1: 克隆仓库**

```bash
cd /Volumes/data/tvapk
git clone https://github.com/nktnet1/webview-kiosk.git
cd /Volumes/data/tvapk/webview-kiosk
```

Expected: 出现 `app/build.gradle.kts`、`settings.gradle.kts`、`gradlew`。

- [ ] **Step 2: 记录上游版本**

```bash
git rev-parse --short HEAD
git status --short
```

Expected: 工作区干净。

### Task 2: 定位默认首页配置

**Files:**
- Inspect: `/Volumes/data/tvapk/webview-kiosk/app/src/main/java/uk/nktnet/webviewkiosk/config/UserSettings.kt`
- Inspect: `/Volumes/data/tvapk/webview-kiosk/app/src/main/java/uk/nktnet/webviewkiosk/config/Constants.kt`

- [ ] **Step 1: 搜索首页配置**

```bash
cd /Volumes/data/tvapk/webview-kiosk
rg -n "homeUrl|HOME_URL|web_content.home_url|webviewkiosk.nktnet.uk" app/src/main
```

Expected: 找到 `homeUrl` 默认值或默认 URL 常量。

- [ ] **Step 2: 只改默认首页**

目标值：

```text
http://47.94.161.17:3000/wl.html
```

实施原则：只替换默认首页，不改导航逻辑、不删设置页、不改 WebView 行为。

### Task 3: 放行 HTTP 明文访问

**Files:**
- Modify or inspect: `/Volumes/data/tvapk/webview-kiosk/app/src/main/res/xml/network_security_config.xml`
- Inspect: `/Volumes/data/tvapk/webview-kiosk/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 检查网络安全配置**

```bash
cd /Volumes/data/tvapk/webview-kiosk
sed -n '1,220p' app/src/main/res/xml/network_security_config.xml
rg -n "networkSecurityConfig|usesCleartextTraffic" app/src/main/AndroidManifest.xml
```

Expected: Manifest 已引用 `@xml/network_security_config`。

- [ ] **Step 2: 若未放行 HTTP，增加 IP 白名单**

建议配置：

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">47.94.161.17</domain>
    </domain-config>
</network-security-config>
```

如果原文件已有其他配置，则保留原配置，只追加对应 `domain-config`。

### Task 4: 构建 debug APK

**Files:**
- Output: `/Volumes/data/tvapk/webview-kiosk/app/build/outputs/apk/debug/app-debug.apk`
- Create: `/Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk`

- [ ] **Step 1: 检查构建环境**

```bash
cd /Volumes/data/tvapk/webview-kiosk
java -version
./gradlew --version
```

Expected: JDK 可用，Gradle Wrapper 可启动。

- [ ] **Step 2: 构建 APK**

```bash
cd /Volumes/data/tvapk/webview-kiosk
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 复制产物**

```bash
mkdir -p /Volumes/data/tvapk/dist
cp app/build/outputs/apk/debug/app-debug.apk /Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk
ls -lh /Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk
```

Expected: APK 文件存在且大小非 0。

### Task 5: APK 静态验收

**Files:**
- Verify: `/Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk`

- [ ] **Step 1: 检查 APK 基本信息**

```bash
aapt dump badging /Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk | sed -n '1,80p'
```

Expected:

```text
sdkVersion:'21'
```

说明：`sdkVersion 21` 覆盖 Android 7.0 API 24。

- [ ] **Step 2: 检查默认 URL 已进入 APK**

```bash
unzip -p /Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk classes.dex | strings | rg "47\.94\.161\.17|wl\.html"
```

Expected: 能看到 `http://47.94.161.17:3000/wl.html`。

### Task 6: 真机或模拟器验收

**Files:**
- Verify: Android 7.0+ TV 盒子或模拟器

- [ ] **Step 1: 安装 APK**

```bash
adb install -r /Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk
```

Expected: `Success`。

- [ ] **Step 2: 启动应用**

```bash
adb shell monkey -p uk.nktnet.webviewkiosk 1
```

Expected: 应用打开并加载 `http://47.94.161.17:3000/wl.html`。

- [ ] **Step 3: 查看日志**

```bash
adb logcat -d | rg -i "webview|cleartext|47\.94\.161\.17|wl\.html|ERR_CLEARTEXT|net::ERR"
```

Expected: 没有 `ERR_CLEARTEXT_NOT_PERMITTED`、`net::ERR_CONNECTION_REFUSED`、`net::ERR_NAME_NOT_RESOLVED`。

## 验收标准

- APK 可安装。
- Android 7.0 设备可启动。
- 首次启动默认访问 `http://47.94.161.17:3000/wl.html`。
- 页面能显示，方向键/遥控器基础操作可用。
- APK 最终产物位于 `/Volumes/data/tvapk/dist/tvapk-webview-kiosk-debug.apk`。

## 风险与处理

- 若构建依赖下载失败：配置 Gradle 国内镜像或多次重试。
- 若页面打不开：先在设备浏览器或 `adb shell curl` 验证网络连通。
- 若出现 cleartext 错误：补 `network_security_config.xml` 明文白名单。
- 若 TV 遥控体验不理想：先用上游控制面板/地址栏设置解决，后续再做 TV 专项 UI 优化。
- 若要正式分发：生成 release keystore，执行 `assembleRelease`，保存签名文件与密码到私密位置，不能写入仓库。

## 是否建议立即执行

建议立即执行。该需求属于“套壳固定网页显示”，不需要重写 App。第一版目标是可安装、可打开目标页面、Android 7.0 兼容。

# 构建规范

定义两个子项目的编译命令和验证步骤。

---

## 1. 环境准备

```bash
export JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home
export ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

---

## 2. 编译命令

### 2.1 lightning-browser

```bash
cd /Volumes/data/tvapk/lightning-browser
chmod +x gradlew
./gradlew :App:assembleDebug
```

**输出**：`App/build/outputs/apk/debug/app-debug.apk`

### 2.2 chromium-kiosk

```bash
cd /Volumes/data/tvapk/chromium-kiosk
chmod +x gradlew
./gradlew :app:assembleDebug
```

**输出**：`app/build/outputs/apk/debug/app-debug.apk`

---

## 3. 验证步骤（必须执行）

### 3.1 使用 aapt 验证

```bash
AAPT="/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/36.0.0/aapt"

# lightning-browser
$AAPT dump badging lightning-browser/App/build/outputs/apk/debug/app-debug.apk

# chromium-kiosk
$AAPT dump badging chromium-kiosk/app/build/outputs/apk/debug/app-debug.apk
```

### 3.2 确认项

| 检查项 | lightning-browser | chromium-kiosk |
|--------|-------------------|----------------|
| `versionCode` | 如 `1405` | 如 `119002` |
| `versionName` | 如 `1.4.5` | 如 `1.0.1-chromium119` |
| `package` | `com.threethan.browser` | `com.pingfeng.tvapk.chromium` |
| `sdkVersion` | `24` | `24` |

### 3.3 计算 SHA256

```bash
sha256sum /path/to/apk
```

---

## 4. 常见编译问题

### `aapt: command not found`

```bash
# aapt 路径
AAPT="/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/36.0.0/aapt"
```

### `JAVA_HOME not set`

```bash
export JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home
```

### `SDK location not found`

```bash
export ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk
```

### `Permission denied: ./gradlew`

```bash
chmod +x gradlew
```

---

## 5. 产物文件位置

| 项目 | 构建后 APK 路径 |
|------|----------------|
| lightning-browser | `/Volumes/data/tvapk/lightning-browser/App/build/outputs/apk/debug/app-debug.apk` |
| chromium-kiosk | `/Volumes/data/tvapk/chromium-kiosk/app/build/outputs/apk/debug/app-debug.apk` |

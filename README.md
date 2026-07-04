# 电视网页展示 APK

本项目仓库包含两种 Android TV 网页全屏展示 APK 方案，用于在老旧 Android TV/盒子设备上固定展示指定网页。

---

## 项目结构

| 目录 | 说明 |
|------|------|
| `chromium-kiosk/` | 基于 Chromium 内核的 WebView 方案（内置 WebView APK 119） |
| `lightning-browser/` | 基于 GeckoView 内核的浏览器方案（移植 LightningBrowser） |
| `dist/` | 构建产物输出目录 |
| `temp/` | 临时文件与工具链目录 |
| `docs/` | 项目文档与计划 |

---

## 方案对比

| 特性 | `chromium-kiosk` | `lightning-browser` |
|------|------------------|-------------------|
| **内核** | 内置 Chromium WebView 119 | 内置 GeckoView |
| **兼容性** | Android 7.0 (API 24) 及以上 | Android 7.0 (API 24) 及以上 |
| **目标** | 老设备 WebView 升级替代 | 老设备 WebView 兼容替代 |
| **大小** | ~83 MB | ~87 MB |
| **版本** | 1.0.1-chromium119 | 1.4.5 |

---

## 快速开始

### 1. Chromium Kiosk（推荐用于 WebView 升级场景）

```bash
cd chromium-kiosk
# 构建 APK
./gradlew assembleDebug
# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Lightning Browser（推荐用于 GeckoView 兼容场景）

```bash
cd lightning-browser
# 构建 APK
./gradlew assembleDebug
# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 默认目标页面

两个应用启动后均默认加载：

```
http://47.94.161.17:3000/wl.html
```

可在应用内通过设置面板修改。

---

## 系统要求

- **最低系统版本**: Android 7.0 (API 24)
- **目标系统版本**: Android 14+ (targetSdkVersion 35/36)
- **设备类型**: Android TV / 电视盒子 / 大屏展示终端
- **构建环境**: JDK 21 + Android SDK

---

## 目录说明

### `chromium-kiosk/`

- 应用包名: `com.pingfeng.tvapk.chromium`
- 版本: `1.0.0-chromium119`
- 通过 `WebViewUpgrade` 技术将系统 WebView 替换为内置 Chromium 119
- 支持顶部控制条、页面缩放、设置弹层

### `lightning-browser/`

- 应用包名: `com.threethan.browser`
- 版本: `1.4.1`
- 基于开源项目 `threethan/LightningBrowser`
- 使用 GeckoView 内核，不依赖系统 WebView
- 支持沉浸式全屏、遥控器交互、页面缩放设置

---

## 文档

- [chromium-kiosk/README.md](chromium-kiosk/README.md)
- [lightning-browser/README.md](lightning-browser/README.md)

---

## 许可证

各子项目保留其原有许可证。请查看子项目目录内的 LICENSE 文件。

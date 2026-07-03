# 变更日志

## [1.0.0-chromium119] - 2026-06-29

### 新增

- 设置弹层新增内核信息区。
- 内核信息区显示当前系统内核包名、版本号和 Chromium 主版本。
- 内核信息区显示 APK 内置内核包名、版本号和 Chromium 主版本。
- 内核信息区显示当前程序实际绑定内核包名、版本号和 Chromium 主版本。
- 内核信息区显示替换状态、进度和失败错误信息。

### 修复

- 修复内核诊断读取隐藏接口异常时可能导致应用闪退的问题。
- 新增内核初始化 15 秒超时回退，避免 WebViewUpgrade 回调异常时一直停留在初始化页。
- 修复 Activity 可能在内核替换完成前创建 WebView，导致仍绑定系统 WebView 的问题。
- 将 `WebViewUpgrade` 从 Maven 旧 AAR 切换为本地新版源码模块。
- 合并新版 `WebViewUpgrade` 的 sandbox 替身服务，支持 Chromium 多进程服务替换链路。
- 诊断信息增加当前实际 URL，便于真机确认加载状态。

### 验证

- `./gradlew :app:assembleDebug` 构建通过。
- `apksigner verify --verbose /Volumes/data/tvapk/dist/tvapk-chromium119.apk` 通过。
- `aapt dump badging` 确认 `versionCode='119001'`、`versionName='1.0.0-chromium119'`、`sdkVersion='24'`、`targetSdkVersion='35'`。
- `aapt dump xmltree` 确认 Manifest 包含 `StubSandboxedProcessService0-4`。
- APK 内确认包含 `res/layout/activity_kiosk.xml`、`res/layout/kiosk_settings_overlay.xml` 和内置 WebView APK。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-chromium119.apk
APK 大小：约 83MB
APK SHA256：c220a982f37333b8b25accb588c72b347d67dfda3a907e0135ebd45acc7e2c1f
内核 SHA256：f6bfbc3d93fb737d86a0cbc9b75d863edf1d739db79d5ab2ea8eab9b57f86dc7
签名类型：debug 签名
```

## [1.0.0-chromium119-ui] - 2026-06-28

### 新增

- 移植旧版 TV 页面外层设计到 Chromium POC。
- 新增顶部控制条：刷新、缩小、缩放比例、放大、设置、退出。
- 新增设置弹层：页面地址、页面缩放、恢复默认、取消、保存、退出应用。
- 新增配置持久化，保存目标 URL 和缩放比例。
- 新增 Chromium WebView 页面缩放注入，支持 `10% - 100%`，步进 `10%`。
- 新增长按刷新按钮打开诊断信息层。

### 调整

- 保留 Chromium119 内核方案，不迁移 GeckoView、扩展、书签和旧版权限管理。
- 顶栏按钮使用文字方案，规避部分 Android TV 内置图标无法显示的问题。

### 验证

- `./gradlew :app:assembleDebug` 构建通过。
- `apksigner verify --verbose /Volumes/data/tvapk/dist/tvapk-chromium119.apk` 通过。
- `aapt dump badging` 确认 `versionCode='119001'`、`versionName='1.0.0-chromium119'`、`sdkVersion='24'`、`targetSdkVersion='35'`。
- APK 内确认包含 `res/layout/activity_kiosk.xml`、`res/layout/kiosk_settings_overlay.xml` 和内置 WebView APK。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-chromium119.apk
APK 大小：约 83MB
APK SHA256：e6f2cc99ab14a15e9919d4f07b5f81038ac4bf46c45ac7a16b9f39128c9e06b5
内核 SHA256：f6bfbc3d93fb737d86a0cbc9b75d863edf1d739db79d5ab2ea8eab9b57f86dc7
签名类型：debug 签名
```

## [1.0.0-chromium119-poc] - 2026-06-28

### 新增

- 新增独立 Android TV Chromium kiosk POC 项目。
- 新增固定 URL 全屏展示能力。
- 新增内置 Google WebView `119.0.6045.53_min24_arm32.apk`。
- 新增 `WebViewUpgrade` 启动前内核替换流程。
- 新增遥控器菜单键/设置键诊断层。

### 验证

- `./gradlew :app:assembleDebug` 构建通过。
- `apksigner verify --verbose /Volumes/data/tvapk/dist/tvapk-chromium119.apk` 通过。
- `aapt dump badging` 确认 `versionCode='119001'`、`versionName='1.0.0-chromium119'`、`sdkVersion='24'`、`targetSdkVersion='35'`。
- 内置内核 APK 确认 `versionName='119.0.6045.53'`、`sdkVersion='24'`、`maxSdkVersion='28'`。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-chromium119.apk
APK 大小：约 83MB
APK SHA256：b50fa9cf2e13f125ffcd90bdec29a8e2f2a2e136aab445ff9e46960ab3458586
内核 SHA256：f6bfbc3d93fb737d86a0cbc9b75d863edf1d739db79d5ab2ea8eab9b57f86dc7
签名类型：debug 签名
```

### 已知风险

- 当前为 POC 包，必须在目标 Android 7.0 TV 真机验证后再决定是否生产使用。
- WebViewUpgrade 依赖隐藏接口和系统服务 hook，厂商 ROM 兼容性不确定。
- 内置 WebView APK 存在 `maxSdkVersion=28`，本包不适合作为高版本 Android 通用包。

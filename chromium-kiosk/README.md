# tvapk-chromium119

## 用途

本项目是 Android TV 固定网页全屏展示 APK 的 Chromium 内核 POC 版本。
当前版本已移植旧版 TV 页面外层设计，包括顶部控制条、页面缩放和设置弹层。

应用启动后加载固定页面：

```text
http://47.94.161.17:3000/wl.html
```

本版本不使用 GeckoView，也不依赖目标电视系统自带 WebView 版本。应用内置 Google WebView `119.0.6045.53_min24_arm32.apk`，启动时通过 `WebViewUpgrade` 尝试让本应用的 `android.webkit.WebView` 使用该内核。

## 依赖

```text
JDK：/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home
Android SDK：/Volumes/data/tvapk/temp/toolchains/android-sdk
WebViewUpgrade：本地源码模块 /Volumes/data/tvapk/temp/research/WebViewUpgrade/core
内置内核：119.0.6045.53_min24_arm32.apk
内核 SHA256：f6bfbc3d93fb737d86a0cbc9b75d863edf1d739db79d5ab2ea8eab9b57f86dc7
```

## 部署

当前产物：

```text
/Volumes/data/tvapk/dist/tvapk-chromium119.apk
```

安装命令：

```bash
adb install -r /Volumes/data/tvapk/dist/tvapk-chromium119.apk
```

启动命令：

```bash
adb shell monkey -p com.pingfeng.tvapk.chromium 1
```

## 配置

当前第一版为固定配置：

```text
packageName=com.pingfeng.tvapk.chromium
versionCode=119001
versionName=1.0.0-chromium119
targetUrl=http://47.94.161.17:3000/wl.html
minSdkVersion=24
targetSdkVersion=35
```

## 启动

启动流程：

1. `KioskApplication` 在主进程启动。
2. `WebViewKernelManager` 调用 `WebViewUpgrade.upgrade(...)`。
3. 内置 WebView APK 从 assets 复制到应用私有目录。
4. `KioskActivity` 等待内核替换完成或失败。
5. 替换流程结束后创建全屏 `android.webkit.WebView`。
6. 加载已保存的业务页面地址，首次启动使用默认地址。

## 交互

- 顶部控制条支持刷新、缩小、放大、打开设置和退出应用。
- 地址区域点击后打开设置弹层。
- 设置弹层支持修改页面地址、调整页面缩放、恢复默认、保存和退出。
- 设置弹层显示系统内核、APK 内置内核、当前实际内核和替换状态。
- 页面缩放范围为 `10% - 100%`，步进 `10%`。
- 遥控器菜单键或设置键打开设置弹层。
- 刷新按钮长按可打开诊断信息层，显示当前 WebView 包名、版本、UserAgent 和 Chromium 主版本。
- 内核诊断读取失败时会显示错误信息，不再因为厂商 ROM 隐藏接口异常直接闪退。

## 常见问题

### 启动后仍然不是 Chromium 119

临时方案：完全退出应用后冷启动，再打开诊断层确认 UserAgent。

根治方案：检查目标设备是否允许 `WebViewUpgrade` hook WebView provider，并查看诊断层中的升级错误。

预防方案：上线前必须在目标 Android TV 真机上确认 `Chromium major: 119`。

### 页面无法加载

临时方案：确认电视网络能访问 `http://47.94.161.17:3000/wl.html`。

根治方案：检查业务页面服务、网络、防火墙、DNS 和 HTTP 明文策略。

预防方案：发布前做断网、慢网络、服务不可用三类异常验收。

### 高版本 Android 无法安装或内核异常

原因：内置内核 APK 标记 `sdkVersion=24`、`maxSdkVersion=28`，本 POC 主要面向 Android 7.0 老电视。

处理建议：高版本系统不要使用本 POC 作为正式包，应另行选择无 `maxSdkVersion` 限制的内核包或继续使用 GeckoView 版本。

## 验证记录

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-chromium119.apk
APK 大小：约 83MB
APK SHA256：c220a982f37333b8b25accb588c72b347d67dfda3a907e0135ebd45acc7e2c1f
签名验证：apksigner verify --verbose 通过，v2 签名有效
aapt badging：versionCode='119001'、versionName='1.0.0-chromium119'、sdkVersion='24'、targetSdkVersion='35'
资源验证：APK 内包含 res/layout/activity_kiosk.xml、res/layout/kiosk_settings_overlay.xml 和 assets/119.0.6045.53_min24_arm32.apk
Manifest 验证：APK 内包含 WebViewUpgrade sandbox 替身服务 StubSandboxedProcessService0-4
```

## 风险说明

- 当前为 POC 验证包，不是正式生产包。
- `WebViewUpgrade` 使用系统服务 hook 和隐藏接口，不同厂商 ROM 兼容性不确定。
- 内置 WebView APK 只覆盖 arm32 和 Android 7.0 起步设备。
- 内核切换不能热替换，必须在 WebView 创建前完成。
- 真机验收前不能承诺解决所有页面兼容问题。

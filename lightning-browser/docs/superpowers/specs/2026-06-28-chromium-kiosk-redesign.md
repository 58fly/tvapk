# Chromium Kiosk Redesign

## 用途

重做一个独立的极简 Android TV 网页展示 APK，用内置高版本 Chromium WebView APK 替代系统老旧 WebView，并避免继续承载 `LightningBrowser` 的完整浏览器复杂度。

## 背景

当前 GeckoView 版本已不低，继续升级 GeckoView 不一定能解决业务页面显示兼容问题。目标业务只需要固定网页全屏展示，不需要标签页、书签、扩展、下载管理、完整浏览器导航等能力。

Android 7.0 老电视的系统 WebView 可能过旧，因此不能简单退回系统 WebView。`WebViewUpgrade` 提供一种可预研路线：在宿主 App 内让 `android.webkit.WebView` 使用随 App 内置或下载的 WebView/Chrome APK 作为内核，只影响本 App，不替换系统全局 WebView。

## 目标

- 新建独立极简 APK 方向，名称暂定 `PingfengChromiumKiosk`。
- 固定加载业务页面：`http://47.94.161.17:3000/wl.html`。
- 启动后直接进入沉浸式全屏展示。
- 内置尽量新的 Chromium WebView APK，同时必须兼容 Android 7.0；内核 APK 的 `minSdkVersion` 不得高于 `24`，第一候选为 `min24`。
- WebView 内核替换必须早于任何 `android.webkit.WebView` 创建。
- 记录并显示实际使用的内核信息，便于现场判断是否替换成功。
- 构建产物独立于现有 GeckoView 包，便于并行测试和回滚。

## 非目标

- 不复刻 `LightningBrowser` 的完整浏览器功能。
- 不做标签页、书签、扩展、下载管理。
- 不做多个内核运行时热切换。
- 不在同一个 APK 内同时保留 GeckoView 和 Chromium WebView 两套展示链路。
- 不依赖用户手动安装系统 WebView 或 Chrome。
- 第一版不做复杂设置面板，只保留必要的诊断入口。

## 内核选择

优先候选：

```text
google/119.0.6045.53_min24_arm32.apk
```

选择原因：

- `min24` 可覆盖 Android 7.0。
- `arm32` 匹配当前项目主要面向的 `armeabi-v7a` 老电视设备。
- HTTP 检测显示包体约 `80MB`，比 Chrome `116.0.5845.93_min24_arm32.apk` 约 `126MB` 更适合内置。
- Chromium 119 已显著高于 Android 7 常见系统 WebView 版本。

备选候选：

```text
chrome/116.0.5845.93_min24_arm32.apk
```

仅当 Google WebView 119 在目标设备上解析失败、加载失败或业务页面兼容不达标时再验证。

## 架构

```text
PingfengChromiumKiosk
├── config
│   └── kiosk.properties
├── core
│   ├── KioskApplication
│   ├── WebViewKernelManager
│   ├── KioskActivity
│   └── KioskWebViewController
├── utils
│   ├── KernelInfoParser
│   └── LogWriter
├── docs
├── logs
├── temp
├── static
│   └── webview-kernel/google-119-min24-arm32.apk
├── test
├── src
├── dist
└── build
```

实际 Android 工程可以使用标准 Gradle 目录，但文档和产物仍按项目规范归档。

## 启动流程

1. `KioskApplication.onCreate()` 判断主进程。
2. 读取内置 WebView APK 资产版本。
3. 调用 `WebViewUpgrade.upgrade(new UpgradeAssetSource(...))`。
4. 监听升级结果并写入本地日志。
5. `KioskActivity` 创建全屏窗口。
6. 创建 `android.webkit.WebView`。
7. 配置 WebView：
   - JavaScript enabled
   - DOM storage enabled
   - Database storage enabled
   - Mixed content allowed
   - Media playback no user gesture when needed
   - Wide viewport / overview mode按业务页面实际验证决定
8. 加载固定 URL。
9. 页面异常时显示简洁错误层，并保留内核信息。

## 数据流

```text
内置 WebView APK
  -> WebViewUpgrade 解包与替换
  -> Android WebView Provider 解析被宿主 App hook
  -> KioskActivity 创建 WebView
  -> WebView 使用替换后的 Chromium 内核
  -> 加载固定业务 URL
```

## 错误处理

- 内核 APK 不存在：记录错误，回退系统 WebView 并显示状态。
- APK 解析失败：记录 `getPackageArchiveInfo` 失败原因，阻止误判为业务页面问题。
- 系统版本低于内核 `minSdkVersion`：阻止替换，回退系统 WebView。
- ABI 不匹配：记录 native lib 提取或加载失败，回退系统 WebView。
- WebView 已提前初始化：提示需要重启，第一版通过启动顺序避免。
- 渲染进程崩溃：销毁当前 WebView，显示错误层，避免主进程崩溃。
- 页面加载失败：显示目标 URL、错误码、内核信息和重试入口。

## 诊断信息

第一版至少输出：

- App 版本。
- Android SDK 版本。
- CPU ABI。
- 系统 WebView 包名和版本。
- 替换后 WebView 包名和版本。
- WebView UserAgent。
- Chromium 主版本。
- WebViewUpgrade 成功或失败原因。
- 当前 URL。

诊断信息可通过隐藏入口查看，例如遥控器菜单键或连续按返回键打开，不作为主要界面。

## 测试

单元测试：

- 内核候选选择规则。
- UserAgent 中 Chromium 版本解析。
- Android SDK 与内核 `minSdkVersion` 兼容判断。
- ABI 候选选择。
- 配置读取和默认 URL。

构建验证：

- `assembleDebug`。
- APK 签名验证。
- `aapt dump badging` 确认 `minSdkVersion=24`、TV launcher。
- SHA256 记录。

真机验证：

- Android 7.0 目标电视启动。
- 内核替换成功，UserAgent 显示 Chromium 119。
- 固定业务页面全屏显示。
- 页面图标、字体、布局、表格、颜色正常。
- 遥控器方向键和确认键可用。
- 断网、页面 404、服务器慢响应时有明确错误状态。
- 冷启动后仍使用内置内核。
- 连续运行至少 2 小时无明显崩溃。

## 发布策略

第一阶段只发布内部 POC 包：

```text
/Volumes/data/tvapk/dist/tvapk-chromium119.apk
```

命名中必须体现：

- `tvapk`
- Chromium 内核主版本号，例如 `chromium119`

通过真机验证后，再决定是否升级为正式用户版本。

## 风险

- `WebViewUpgrade` 依赖系统服务 hook 和隐藏接口，厂商 ROM 兼容性不确定。
- 内置 WebView APK 会显著增大安装包。
- Google WebView APK 来源需要固定版本、固定校验值，避免供应链漂移。
- 不同电视 CPU ABI 可能不一致，第一版只覆盖 arm32。
- 运行时切换内核不可热替换，需要冷启动。
- 该方向是 Chromium 兼容验证包，不应替代现有 GeckoView 稳定包，直到真机矩阵通过。

## 验收标准

- 能在 Android 7.0 arm32 电视安装并启动。
- 不依赖系统 WebView 版本即可使用内置 Chromium 内核。
- 业务页面 `http://47.94.161.17:3000/wl.html` 全屏正常显示。
- 页面中当前 GeckoView 版本异常的图标、字体或布局问题不再出现。
- 设置或诊断入口能明确显示实际 Chromium 版本。
- 内核替换失败时不闪退，能回退或给出明确原因。
- 产物、SHA256、版本号、内核来源和验证结果写入 README 与 CHANGELOG。

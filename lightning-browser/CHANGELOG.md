# 变更日志

本文件记录电视网页展示 APK 的版本变更。

格式遵循 Keep a Changelog 风格，版本号遵循 `主版本.次版本.修订版本` 规则。

## [V1.4.5-debug] - 2026-07-04

### 修复

- **修复系统回收 Activity 时 WebView 内存泄漏问题：** 当设备低内存触发系统回收 `BrowserActivity` 时（`isFinishing() == false`），旧逻辑仅将 WebView 设为 inactive，导致 `webViewByTabId` 中 WebView 实例持续累积。运行 30-40 小时后触发 OOM 崩溃。修复后系统回收时直接 `killWebView()` 彻底释放资源。
- **修复 `BrowserWebView.kill()` 未清理 Delegate 引用：** `kill()` 现在会主动将所有 Delegate（`HistoryDelegate`、`PermissionDelegate`、`ProgressDelegate`、`ContentDelegate`、`PromptDelegate`、`NavigationDelegate`、`MediaSessionDelegate`）设为空，切断 Activity 引用链，确保 GC 回收。
- **修复 `BrowserService.killWebView()` 对已 finishing Activity 重复调用 `finish()`：** 添加 `isFinishing()` 判断，避免在 Activity 已被系统回收时再次触发 finish 异常。
- **修复 `BrowserService.onDestroy()` 残留 WebView 泄漏：** Service 销毁时遍历并强制 `killWebView()` 清理所有剩余 WebView，防止 Service 重启后旧实例残留。

### 新增

- **新增 `MemoryCleaner` 定期内存清理服务：** 每 6 小时自动触发一次，清理 GeckoRuntime Storage、WebView Cache，并强制 GC，防止长时间挂机内存持续增长。
- **新增 `MemoryCleaner.checkAndCleanIfNeeded()` 阈值清理：** 当 Java Heap 超过 400MB 时自动触发紧急清理。

### 变更

- 应用版本更新为 `versionCode=1405`、`versionName=1.4.5`。

### 验证

- `:App:assembleDebug` 构建通过。
- `aapt dump badging` 确认 `versionCode='1405'`、`versionName='1.4.5'`、`sdkVersion='24'`、`targetSdkVersion='36'`。
- `aapt` 确认包名为 `com.threethan.browser`。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-v1.4.5-1405-debug.apk
快捷路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 91MB
SHA256：f43e58b69231a6f229bd5d512367e5fb358f033dda112edace4a7aa42b141b78
签名类型：debug 签名
```

## [V1.4.4-debug] - 2026-07-02

### 修复

- 修复 APK 缩放脚本直接缩放 `body`，导致页面 `position: fixed` 底部驻留栏在 70% 缩放时离开底边的问题。
- 缩放逻辑改为仅缩放普通页面内容，保留 `.bottom-bar`、`.cp-toggle`、`.recovery-overlay`、`.loading-mask` 等固定浮层按视口定位。

### 变更

- 应用版本更新为 `versionCode=1404`、`versionName=1.4.4`。

## [V1.4.3-diagnostics-debug] - 2026-07-02

### 修复

- 修复 Gecko 子进程和主进程共用同一个运行状态文件，导致 `PREVIOUS_EXIT` 可能误报的问题。
- 运行日志新增 `proc=` 字段，用于区分主进程和 Gecko 子进程。
- `PREVIOUS_EXIT` 仅由主进程计算，避免 Gecko 子进程正常重启被误判为应用崩溃。

### 排查结论

- 现场日志中多个 PID 在同一分钟交替出现，且 `TRIM_MEMORY level=15` 后马上出现 `PROCESS_START`。
- 这更像 Gecko 子进程被系统裁剪/重启，旧版日志缺少进程名，无法准确区分主进程和子进程。
- 当前截图未看到 `CRASH`，主进程 Java 崩溃证据不足；仍需用新版 `proc=` 日志继续确认。

### 变更

- 应用版本更新为 `versionCode=1403`、`versionName=1.4.3`。

## [V1.4.2-diagnostics-debug] - 2026-07-02

### 新增

- 设置页“近期运行日志”支持滚动条，避免日志内容被固定高度截断。
- 设置页新增“全屏查看运行日志”入口，便于电视现场拍照反馈完整日志。
- 运行日志展示条数从最近 12 条扩展到最近 40 条。
- 运行日志新增 `PREVIOUS_EXIT` 标记，用于识别上次进程未正常关闭。
- 运行日志新增 `native`、`pss`、`private dirty` 内存摘要，用于区分 Java heap 和 GeckoView 原生内存增长。
- Java 崩溃、低内存、内存裁剪会同步写入上次运行状态，便于下次启动判断退出前状态。
- 新增页面加载、重载、前后台切换等事件日志：`LOAD_URL`、`PAGE_START`、`PAGE_STOP`、`PAGE_RELOAD`、`ACTIVITY_RESUME`、`ACTIVITY_PAUSE`。

### 修复

- 修复设置页版本号从本地配置读取，升级后仍显示旧 build 的问题；版本显示改为使用当前 APK 构建版本。
- 修复下载流程中静态 Map 持有 `Activity` 的内存泄漏风险。
- 修复 WebView 销毁后 tab 标题和 URL 元数据未同步清理的问题。

### 排查结论

- 检查业务展示页 `http://47.94.161.17:8000/public/fc.html` 后，发现页面每 60 秒刷新数据并重复创建自动翻页 `setInterval`，旧定时器未清理，长时间运行会持续累积闭包、旧 DOM 和后台任务。
- 当前页面还会按 BOM 分批请求 `/bomGoods/list`，数据规模较大时每分钟请求和 DOM 重建压力偏高。
- 30-40 小时后退出/崩溃与页面定时器累积、GeckoView 原生内存增长、低内存回收高度相关；APK 侧继续清理生命周期泄漏，页面侧也需要保持定时器清理和请求防重入。

### 变更

- 应用版本更新为 `versionCode=1402`、`versionName=1.4.2`。

### 验证

- `:App:assembleDebug` 构建通过。
- `apksigner verify --verbose` 通过，v2 签名有效。
- `aapt dump badging` 确认 `versionCode='1402'`、`versionName='1.4.2'`、`sdkVersion='24'`、`targetSdkVersion='36'`。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-v1.4.2-1402-debug.apk
快捷路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：22a16840f30f19f38060a2563c971a245c26dd2f1fceda507e09d3e8b5e4adc5
签名类型：debug 签名
```

## [V1.4.1-diagnostics-debug] - 2026-06-30

### 新增

- 新增本机运行日志模块，记录进程启动、Activity 创建/销毁、Service 创建/销毁、GeckoRuntime 初始化、WebView 创建/复用/销毁、低内存和崩溃摘要。
- 新增设置页面“近期运行日志”区域，展示最近 12 条日志，便于用户拍照反馈。
- 日志条目包含时间、事件类型、进程 ID、系统 SDK、应用版本和 Java heap 内存摘要。
- 新增全局未捕获异常记录，应用崩溃前会尽量写入 `CRASH` 事件和简短堆栈。
- 新增 `onLowMemory`、`onTrimMemory` 记录，用于判断是否可能被系统内存回收。

### 修复

- 修复 `BrowserActivity.onDestroy()` 在 `wService == null` 时直接返回，导致父类清理逻辑可能不执行的问题。
- 修复 `BoundActivity` 仅在 `isFinishing()` 时解绑服务，可能长期持有 Activity/ServiceConnection 的风险。
- 增加销毁流程异常兜底，避免清理阶段异常掩盖真实退出原因。

### 排查结论

- 当前代码未发现明确的 30-40 小时定时退出逻辑。
- 发现静态 `BrowserWebView`、`Activity`、`watchingActivities` 等长期引用结构，若生命周期清理异常，存在内存累积风险。
- 本版优先修复明显清理缺口，并增加现场可见日志，用于区分应用崩溃、系统清理、低内存回收和用户/代码主动退出。

### 验证

- `:App:assembleDebug` 构建通过。
- `apksigner verify --verbose` 通过，v2 签名有效。
- `aapt dump badging` 确认 `versionCode='1401'`、`versionName='1.4.1'`、`sdkVersion='24'`、`targetSdkVersion='36'`。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：379f1b45bba0ea4816d7391427b63a8b301510933487215f1076e08d02c926cc
签名类型：debug 签名
```

## [V1.4.1-debug] - 2026-06-24

### 修复

- 修复 Android TV 设备上业务页面内部图标显示为方块的问题。
- 新增 `KioskGeckoSettingsFactory`，统一 GeckoRuntime 和 GeckoSession 配置。
- 显式启用 GeckoView 网页字体加载能力，兼容 `iconfont`、FontAwesome 等图标字体。
- 关闭会话级跟踪保护，避免业务页面第三方字体 CDN 被误拦截。

### 变更

- 应用版本更新为 `versionCode=1401`、`versionName=1.4.1`。

### 验证

- `:App:testDebugUnitTest --tests com.threethan.browser.browser.GeckoView.KioskGeckoSettingsFactoryTest` 通过。
- `:App:testDebugUnitTest` 通过。
- `:App:assembleDebug` 通过。
- `apksigner verify --verbose` 通过，v2 签名有效。
- `aapt dump badging` 确认 `versionCode='1401'`、`versionName='1.4.1'`、`sdkVersion='24'`、`targetSdkVersion='36'`。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：f5cfaf9c555091b8678caa74308148faad1b1a3cf0ccd6ac7b28d27f5706cc94
签名类型：debug 签名
```

## [V1.4.0-debug] - 2026-06-15

### 新增

- 新增电视直播软件风格半透明设置面板。
- 新增菜单键打开设置面板能力。
- 新增返回键打开设置面板能力。
- 新增设置面板内退出应用按钮。
- 新增网页地址编辑和保存能力。
- 新增页面缩放实时预览能力。
- 新增 app 私有目录 `kiosk_settings.properties` 配置文件。
- 新增 `KioskSettings`、`KioskSettingsStore`、`KioskSettingsController` 单元测试。
- 新增设置面板底部版本号显示。

### 变更

- 应用版本更新为 `versionCode=1400`、`versionName=1.4.0`。
- 默认 URL 不再通过源码常量 `KIOSK_URL` 控制，改为启动时读取配置文件。
- 缩放设置不再依赖顶部悬浮控制栏，改为设置面板滑块控制。
- `WrapperActivity` 启动时读取本地配置中的目标地址。
- `BrowserActivity` 改为返回键和菜单键进入设置，退出应用统一放在设置面板内。
- 退出按钮改为红色电视焦点态，聚焦和按下状态更醒目。
- 设置保存后下次启动自动按配置恢复目标地址和缩放比例。

### 修复

- 修复旧顶部悬浮栏不自动隐藏导致看板不可用的问题。
- 修复顶部栏隐藏后影响 GeckoView 高度计算的问题。
- 修复页面缩放调整缺少持久化的问题。
- 修复版本号硬编码在缩放标签中的问题，改为读取构建版本信息。
- 移除主界面右下角版本号，避免干扰看板画面。
- 移除双击返回退出逻辑，避免误触退出。

### 验证

- `:App:testDebugUnitTest` 通过。
- `:App:assembleDebug` 通过。
- `apksigner verify --verbose` 通过，v2 签名有效。
- `aapt dump badging` 确认 `versionCode='1400'`、`versionName='1.4.0'`、`sdkVersion='24'`、`targetSdkVersion='36'`。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：5df1e094d5e8dc318ae378a78c43442bbb8fe161165ac85ce8ea5a2577f7965f
签名类型：debug 签名
```

## [V1.3.2-debug] - 2026-06-15

### 修复

- 修复固定 URL 启动链路通过 `WrapperActivity.open()` 传入 `isTab=true`，导致看板模式未启用的问题。
- 新增 `isKiosk=true` 启动参数，确保地址栏默认隐藏、返回键临时显示、10 秒自动隐藏逻辑实际生效。
- 新增 `KioskModeController` 单元测试，覆盖 `isKiosk=true && isTab=true` 时仍强制隐藏顶部栏。

### 变更

- 应用版本更新为 `versionCode=1302`、`versionName=1.3.2`。
- 顶部缩放标记显示 `v1.3.2`，便于真机确认是否安装新包。

### 验证

- `:App:testDebugUnitTest` 通过。
- `:App:assembleDebug` 通过。
- `apksigner verify --verbose` 通过，v2 签名有效。
- `aapt dump badging` 确认 `versionCode='1302'`、`versionName='1.3.2'`、`sdkVersion='24'`、`targetSdkVersion='36'`、`LEANBACK_LAUNCHER`。
- APK ZIP 完整性验证通过。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：2dcda453ddcad580fb5629424f3db43d081cc0ff7f66fd0140a7bcef184d4fc7
签名类型：debug 签名
```

## [V1.0-debug] - 2026-06-15

### 新增

- 基于 `threethan/LightningBrowser` 建立 Android TV 网页展示 APK。
- 默认打开固定业务页面：`http://47.94.161.17:3000/wl.html`。
- 使用内置 GeckoView 内核，降低 Android 7.0 系统 WebView 过旧带来的兼容风险。
- 支持 Android TV 启动入口，Manifest 包含 `LEANBACK_LAUNCHER`。
- 支持 `http` 明文访问，Manifest 开启 `usesCleartextTraffic=true`。
- 新增页面缩放控制，范围 `10%` 到 `100%`，每 `10%` 一档。
- 新增顶部控制栏自动隐藏机制，10 秒无操作后恢复沉浸式看板模式。
- 新增 `KioskZoomController` 单元测试，覆盖缩放默认值、边界值和档位归一化。
- 新增 `KioskLayoutController` 单元测试，固定 GeckoView 全屏布局规则。

### 变更

- 启动入口 `WrapperActivity` 改为直接打开固定 URL，不再显示原项目启动页。
- `BrowserActivity` 改为默认隐藏地址栏并进入沉浸式全屏。
- 顶部控制栏改为悬浮覆盖模式，不再通过修改 GeckoView 高度实现显示和隐藏。
- GeckoView 布局固定为全屏，顶部和底部偏移均为 `0`。
- 页面缩放改为通过 GeckoView 注入页面样式实现，页面刷新或跳转后自动重新应用当前缩放档位。
- README 改写为中文产品说明和技术规范文档。

### 修复

- 修复 Android 7.0 系统 WebView 旧内核导致页面加载不完整的问题。
- 修复地址栏隐藏后 GeckoView 高度计算不稳定的问题。
- 修复沉浸式模式下系统栏、控制栏、网页视口高度互相影响的问题。
- 修复顶部控制栏可能被动态添加的 GeckoView 遮挡的问题。
- 修复顶部控制栏被页面交互持续刷新倒计时，导致 10 秒自动隐藏不生效的问题。
- 修复缩放脚本跳转导致地址栏显示 `javascript:(...)` 的问题。
- 修复缩放后页面左右出现大量空白的问题，配合业务网页宽度调整后可保持铺满显示。

### 验证

- `:App:testDebugUnitTest` 通过。
- `:App:assembleDebug` 通过。
- `apksigner verify --verbose` 通过，v2 签名有效。
- `aapt dump badging` 确认 `sdkVersion='24'`、`targetSdkVersion='36'`、`LEANBACK_LAUNCHER`、`native-code='armeabi-v7a'`。
- APK ZIP 完整性验证通过。

### 产物

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：1574a9d70c5cc98a17f69443e79df81ed7ad4d9e3b89e513b4644f8dc24d2516
签名类型：debug 签名
```

### 已知限制

- 当前为 debug APK，适合个人测试和自用安装，不适合作为正式生产分发包。
- 当前目标 URL 写在源码中，修改页面地址后需要重新构建 APK。
- 当前缩放能力通过页面样式注入实现，特殊网页结构可能需要业务页面配合调整根容器宽度。
- 正式分发前需要使用固定 release keystore 重新签名，并递增版本号。

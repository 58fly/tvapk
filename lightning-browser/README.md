# 电视网页展示 APK

## 用途

本项目是基于开源项目 `threethan/LightningBrowser` 最小化改造的 Android TV 网页展示应用。
应用启动后自动进入固定业务页面：

```text
http://47.94.161.17:3000/wl.html
```

本项目用于替代依赖系统 `android.webkit.WebView` 的旧方案。旧方案在 Android 7.0 设备上会受系统 WebView 版本限制，可能导致页面脚本、样式或资源加载不完整；本项目改用随 APK 打包的 GeckoView 浏览器内核，降低对系统浏览器内核版本的依赖。

## 产品说明

### 产品定位

- 产品名称：电视网页展示 APK
- 目标设备：Android 7.0 及以上电视盒子、Android TV 设备、大屏展示终端
- 核心功能：开机或手动启动后全屏展示指定网页
- 默认页面：`http://47.94.161.17:3000/wl.html`
- 当前产物：`/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk`
- 当前版本：`V1.4.1-debug`

### 功能范围

- 启动应用后自动打开固定业务页面
- 使用内置 GeckoView 内核渲染网页
- 支持 `http` 明文页面访问
- 支持 Android TV 启动入口
- 支持沉浸式全屏展示
- 支持页面显示缩放，范围 `10%` 到 `100%`，每档 `10%`
- 支持遥控器基础方向键、确认键交互

### 适用场景

- 电视盒子大屏看板
- 固定业务页面展示
- 内网或专网网页终端
- 单页面 kiosk 展示
- Android 7.0 老设备网页兼容性补强

### 不适用场景

- 多标签浏览器
- 高频站点切换
- 完整浏览器菜单操作
- 需要账号、证书、密钥内置到 APK 的场景
- 要求极小安装包体积的场景

## 技术规范

### 基础架构

- 基础项目：`threethan/LightningBrowser`
- 应用类型：Android 原生应用
- 页面容器：GeckoView
- 最低系统：Android 7.0，对应 `minSdkVersion 24`
- 构建目标：`targetSdkVersion 36`
- 启动入口：`WrapperActivity`
- 展示入口：`BrowserActivity`
- 网络策略：允许访问明文 `http` 页面
- TV 入口：Manifest 声明 `LEANBACK_LAUNCHER`

### 核心文件

```text
App/src/main/java/com/threethan/browser/wrapper/WrapperActivity.java
App/src/main/java/com/threethan/browser/browser/BrowserActivity.java
App/src/main/java/com/threethan/browser/browser/KioskLayoutController.java
App/src/main/java/com/threethan/browser/browser/KioskZoomController.java
App/src/main/java/com/threethan/browser/browser/GeckoView/BrowserWebView.java
App/src/main/java/com/threethan/browser/browser/GeckoView/KioskGeckoSettingsFactory.java
App/src/main/java/com/threethan/browser/browser/GeckoView/Delegate/CustomNavigationDelegate.java
App/src/main/java/com/threethan/browser/browser/GeckoView/Delegate/CustomProgressDelegate.java
```

### 关键改动

`WrapperActivity.java`：

- 应用启动后读取本地配置文件中的目标地址
- 保留浏览器原启动链路和标签页逻辑
- 目标地址不再硬编码在源码里

`BrowserActivity.java`：

- 在页面启动时启用沉浸式全屏
- 隐藏系统状态栏和导航栏
- 菜单键弹出半透明设置面板
- 设置面板支持网页地址、缩放比例、保存、取消、恢复默认
- 缩放修改支持实时预览
- 设置保存到 app 私有目录 `kiosk_settings.properties`
- 页面启动后自动读取配置恢复目标地址和缩放
- 保留原浏览器内核、加载能力和遥控器基础操作能力

`KioskLayoutController.java`：

- 统一控制看板模式下的 GeckoView 布局偏移
- 当前规则：GeckoView 永远保持全屏布局，顶部和底部偏移均为 `0`

`KioskZoomController.java`：

- 统一控制页面缩放档位
- 默认范围：`10%` 到 `100%`
- 默认步进：`10%`

`BrowserWebView.java`：

- 负责 GeckoView 会话创建、页面加载和缩放样式注入
- 页面刷新或跳转后会重新应用当前缩放档位
- GeckoView 会话关闭跟踪保护，避免业务页第三方字体 CDN 被误拦截

`KioskGeckoSettingsFactory.java`：

- 统一生成 GeckoRuntime 和 GeckoSession 设置
- 显式开启网页登录字体，保证 `iconfont`、FontAwesome 等图标字体正常渲染

### 安装包信息

```text
APK 路径：/Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
APK 大小：约 87MB
SHA256：f5cfaf9c555091b8678caa74308148faad1b1a3cf0ccd6ac7b28d27f5706cc94
签名类型：debug 签名
```

### 已验证项目

- `sdkVersion` 为 `24`
- `targetSdkVersion` 为 `36`
- Manifest 包含 `LEANBACK_LAUNCHER`
- Manifest 开启 `usesCleartextTraffic=true`
- APK 内包含 `lib/armeabi-v7a/libxul.so`
- APK 内包含 `lib/armeabi-v7a/libmozglue.so`
- `apksigner verify --verbose` 验证通过
- `aapt dump badging` 确认 `versionCode='1401'`、`versionName='1.4.1'`
- ZIP 结构完整性验证通过

## 依赖

### 本机工具链

```text
JDK：/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home
Android SDK：/Volumes/data/tvapk/temp/toolchains/android-sdk
```

### 构建依赖

- JDK 21
- Android SDK Platform 36
- Android Build Tools 36 或以上
- Gradle Wrapper
- Android Debug Bridge，即 `adb`

## 构建

进入源码目录：

```bash
cd /Volumes/data/tvapk/lightning-browser
```

执行构建：

```bash
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home \
ANDROID_SDK_ROOT=/Volumes/data/tvapk/temp/toolchains/android-sdk \
PATH=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home/bin:/Volumes/data/tvapk/temp/toolchains/android-sdk/platform-tools:$PATH \
./gradlew assembleDebug
```

如果 `gradlew` 没有执行权限，可临时授权：

```bash
chmod +x /Volumes/data/tvapk/lightning-browser/gradlew
```

构建完成后建议恢复权限，避免产生无关文件权限变更：

```bash
chmod 644 /Volumes/data/tvapk/lightning-browser/gradlew
```

## 安装

连接 Android TV 设备后执行：

```bash
adb install -r /Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
```

如设备存在旧版本且签名不一致，先卸载旧版本后重新安装。

## 验证

### 静态验证

查看 APK 基础信息：

```bash
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home \
PATH=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home/bin:/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/37.0.0:/Volumes/data/tvapk/temp/toolchains/android-sdk/platform-tools:$PATH \
aapt dump badging /Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
```

校验签名：

```bash
ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk \
JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home \
PATH=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home/bin:/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/37.0.0:$PATH \
apksigner verify --verbose /Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
```

校验文件摘要：

```bash
shasum -a 256 /Volumes/data/tvapk/dist/tvapk-lightning-gecko-debug.apk
```

### 真机验证

- 设备系统版本为 Android 7.0 或以上
- 安装 APK 后可在 TV 应用入口看到应用
- 启动后自动打开 `http://47.94.161.17:3000/wl.html`
- 启动后默认进入沉浸式全屏展示，地址栏默认隐藏
- 按返回键或菜单键可打开半透明设置面板
- 设置面板中调整缩放时会实时预览效果
- 保存后写入 `kiosk_settings.properties`，下次启动自动恢复
- 设置面板显示版本号信息
- 设置面板提供退出按钮，退出按钮有红色电视焦点态
- 页面主体内容完整显示
- 遥控器方向键和确认键可进行基础操作
- 页面全屏展示，无明显系统栏遮挡

## 配置

配置文件位置：

```text
/data/data/com.threethan.browser/files/kiosk_settings.properties
```

字段：

```text
targetUrl=http://47.94.161.17:3000/wl.html
zoomPercent=100
versionName=1.4.1
versionCode=1401
updatedAt=...
```

操作方式：

1. 启动应用
2. 按菜单键
3. 在半透明设置面板中修改网页地址和缩放比例
4. 点保存

下次启动会按配置文件自动恢复。

## 常见问题

### 页面加载不完整

临时方案：确认 TV 设备网络可访问 `http://47.94.161.17:3000/wl.html`，并重启应用重新加载。

根治方案：检查目标页面是否依赖过新的浏览器 API、跨域资源、外部 CDN、WebSocket、音视频能力或 HTTPS 混合内容策略。

预防方案：业务页面上线前使用 Android 7.0 真机或同等系统环境做兼容性验收。

### 页面图标显示成方块

临时方案：安装 `V1.4.1-debug` 或更新版本后重启应用，并确认设备网络可访问业务页面依赖的字体 CDN。

根治方案：APK 已显式开启 GeckoView 网页字体能力，并关闭会话级跟踪保护，避免 `iconfont`、FontAwesome 等远端图标字体被拦截或禁用。

预防方案：业务页面发布前保留图标字体文件的稳定地址；如果部署在内网，建议把 `.woff`、`.woff2`、`.ttf` 字体文件随业务系统同源发布，减少老设备和网络策略差异。

### 安装失败

临时方案：执行 `adb uninstall` 卸载旧包后重新安装。

根治方案：统一应用包名、签名证书和版本号管理，避免 debug 包与 release 包混装。

预防方案：正式分发前使用固定 release keystore 重新签名。

### 安装包体积较大

原因：GeckoView 内核和原生库随 APK 一起打包，体积会明显大于系统 WebView 壳应用。

处理建议：如需降低体积，可在后续版本中按目标设备 CPU 架构拆分 APK。

### 如何调整页面缩放

操作方式：启动应用后按返回键或菜单键，打开半透明设置面板；移动缩放滑块调整比例。

档位范围：`10%`、`20%`、`30%`、`40%`、`50%`、`60%`、`70%`、`80%`、`90%`、`100%`。

说明：当前缩放通过页面样式注入实现，页面刷新或跳转后会自动重新应用当前缩放档位。

## 风险说明

- 当前 APK 为 debug 包，只适合测试安装，不适合作为正式生产分发包。
- 当前 URL 由配置文件控制，首次安装后可直接在设置面板中修改。
- 明文 `http` 页面存在传输被监听或篡改风险，公网生产环境建议改用 `https`。
- 内置 GeckoView 可规避系统 WebView 过旧问题，但不能保证所有网页 API 都完全兼容。
- Android 7.0 老设备性能有限，复杂动画、高清视频、大量脚本可能影响流畅度。

## 维护规范

- 修改目标页面只调整设置面板中的网页地址，不改动浏览器核心逻辑。
- 修改缩放档位只调整 `KioskZoomController`，默认保留 `10%` 到 `100%` 范围。
- 修改全屏、遥控器、启动行为前，需要先在 Android TV 真机验证。
- 构建产物统一放入 `/Volumes/data/tvapk/dist/`。
- 临时文件统一放入 `/Volumes/data/tvapk/temp/`。
- 发布前必须重新执行静态验证、签名验证和真机验证。
- 版本变更统一记录到 `CHANGELOG.md`。

## 当前状态

- 已完成半透明设置面板改造
- 已生成可安装 debug APK
- 已完成 APK 单测与构建验证
- 已支持返回键/菜单键打开设置面板、实时缩放预览、配置文件持久化、设置页退出
- 待完成目标 TV 真机验收和正式发布签名

## 变更日志

完整版本记录见：

```text
CHANGELOG.md
```

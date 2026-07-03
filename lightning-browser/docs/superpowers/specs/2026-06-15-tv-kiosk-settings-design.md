# TV Kiosk Settings Design

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 电视网页展示 APK 改成电视直播软件风格的半透明设置菜单，支持实时预览缩放、编辑网页地址、保存到本地配置，并保留版本号显示。

**Architecture:** 主页面保持全屏渲染，设置入口改成半透明覆盖层，不再用顶部悬浮条挤压网页高度。网页地址和缩放值通过本地配置文件持久化，菜单打开时读取当前配置，调整时实时预览，保存时写回磁盘。版本号在主界面角落与设置面板底部同时显示，作为现场辨识标记。

**Tech Stack:** Android 原生 UI、GeckoView、app 私有目录 `kiosk_settings.properties` 配置文件、现有 Java 工程结构、单元测试 + Gradle 构建

---

## 需求范围

- 菜单键打开半透明设置面板。
- 面板支持实时预览缩放效果。
- 面板可编辑网页地址。
- 点击保存后写入本地配置文件。
- 下次启动自动读取配置并打开保存的网址。
- 保留版本号显示。
- 取消时恢复进入菜单前状态。

## 交互规则

- 主页面默认无顶部设置条。
- 设置面板为居中浮层，背景半透明，网页仍继续渲染。
- 调整缩放时立即作用到当前页面，用户可看到实时结果。
- 保存前的修改视为草稿，取消后回滚草稿。
- 返回键优先关闭设置面板，其次才退出页面。
- 设置面板显示版本号 `v1.3.2 / build 1302`。

## 数据规则

- 持久化字段至少包含：
  - `targetUrl`
  - `zoomPercent`
  - `versionName`
  - `versionCode`
  - `updatedAt`
- 配置文件固定写入 app 私有文件目录：
  - 文件名：`kiosk_settings.properties`
  - Android 路径：`context.getFilesDir()/kiosk_settings.properties`
  - 读取失败或字段非法时自动回退默认值，避免现场设备启动失败。
- 默认值：
  - `targetUrl = http://47.94.161.17:3000/wl.html`
  - `zoomPercent = 100`
- 缩放档位仍限制为 `10%` 到 `100%`，每档 `10%`。

## 组件边界

- `KioskSettingsStore`
  - 负责读取、写入、恢复默认配置。
- `KioskSettingsController`
  - 负责草稿状态、保存/取消、预览同步。
- `KioskSettingsOverlay`
  - 负责半透明面板 UI 与事件分发。
- `BrowserActivity`
  - 负责菜单键/返回键入口、页面应用配置、版本号入口。

## 验收标准

- 菜单键可打开/关闭设置面板。
- 缩放滑动时网页实时变化。
- 输入网址并保存后，下次启动自动打开新网址。
- 取消后页面与配置不变。
- 版本号可在页面与面板中辨识。
- 主页面不再依赖顶部悬浮条显示/隐藏。

## 风险

- 若目标页自身强制居中或限制宽度，实时预览会受网页 DOM 结构影响。
- 若保存配置文件路径设计不稳，可能导致重启后读取失败。
- 若菜单键在部分电视遥控器上不可用，需要补备用入口，例如设置图标或长按菜单。

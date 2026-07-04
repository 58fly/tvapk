# AGENT.md

本文件是 AI Agent 在本项目中的工作入口。
**详细规则见 `/docs/` 目录，Agent 在开始工作前必须阅读。**

---

## 📋 必读文档

| 文档 | 路径 | 说明 |
|------|------|------|
| **版本号规范** | [`/docs/versioning.md`](./docs/versioning.md) | versionCode/versionName 计算规则 |
| **构建规范** | [`/docs/build.md`](./docs/build.md) | 编译命令和验证步骤 |
| **发布规范** | [`/docs/release.md`](./docs/release.md) | Tag、Release、产物命名 |
| **工作流程** | [`/docs/workflow.md`](./docs/workflow.md) | 代码修改后的完整操作流 |

---

## 🚀 快速开始

```bash
# 1. 修改代码前，先阅读相关文档
# 2. 修改后按 workflow.md 执行
# 3. 构建前检查版本号（见 versioning.md）
# 4. 构建后验证 APK（见 build.md）
# 5. 发布时遵循 release.md
```

---

## 📦 项目结构

```
tvapk/
├── chromium-kiosk/      # 方案一：内置 Chromium WebView
│   ├── app/build.gradle   # 版本号在此处
│   └── CHANGELOG.md       # 版本变更日志
├── lightning-browser/   # 方案二：内置 GeckoView
│   ├── App/build.gradle   # 版本号在此处
│   └── CHANGELOG.md       # 版本变更日志
├── dist/                # 编译产物目录
├── docs/                # 详细规范文档（必读）
│   ├── versioning.md
│   ├── build.md
│   ├── release.md
│   └── workflow.md
└── AGENT.md             # 本文件（索引入口）
```

---

## ❓ 常见问题索引

| 问题 | 查看文档 |
|------|---------|
| versionCode 怎么算？ | `/docs/versioning.md` §2.1 ~ §2.2 |
| APK 产物怎么命名？ | `/docs/release.md` §4 |
| 怎么创建 GitHub Release？ | `/docs/release.md` §5.3 |
| 修改代码后要做什么？ | `/docs/workflow.md` §3 |

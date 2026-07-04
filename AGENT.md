# AGENT.md

本文件定义 AI Agent（Codex / ZCode / Claude 等）在本项目中的工作规范。
**Agent 在执行任何构建、修改、发布任务前，必须先阅读此文件。**

---

## 1. 项目概述

```
tvapk/                          # 电视网页展示 APK 项目
├── chromium-kiosk/             # 方案一：内置 Chromium WebView
│   ├── app/
│   │   └── build.gradle          # versionCode=119002, versionName="1.0.1-chromium119"
│   └── ...
├── lightning-browser/           # 方案二：内置 GeckoView
│   ├── App/
│   │   └── build.gradle          # versionCode=1405, versionName="1.4.5"
│   └── ...
├── dist/                        # 编译产物目录
└── AGENT.md                     # 本文件
```

| 项目 | 说明 | 包名 |
|------|------|------|
| `chromium-kiosk` | 基于 Chromium WebView 的 kiosk 方案 | `com.pingfeng.tvapk.chromium` |
| `lightning-browser` | 基于 GeckoView 的浏览器方案 | `com.threethan.browser` |

---

## 2. 版本号规范（必须遵守）

### 2.1 lightning-browser (GeckoView)

```
versionCode = MAJOR * 1000 + MINOR * 100 + PATCH

# 示例
1.4.5 → versionCode = 1*1000 + 4*100 + 5 = 1405
1.5.0 → versionCode = 1500
2.0.0 → versionCode = 2000
```

### 2.2 chromium-kiosk (Chromium WebView)

```
versionCode = <ChromiumMajor> * 1000 + <Patch>

# 示例
Chromium 119, patch 1 → versionCode = 119*1000 + 1 = 119001
Chromium 119, patch 2 → versionCode = 119*1000 + 2 = 119002
Chromium 120, patch 0 → versionCode = 120000
```

### 2.3 versionName 格式

| 项目 | 格式 | 示例 |
|------|------|------|
| lightning-browser | `MAJOR.MINOR.PATCH` | `1.4.5` |
| chromium-kiosk | `MAJOR.MINOR.PATCH-chromium<内核版本>` | `1.0.1-chromium119` |

**⚠️ Agent 在执行任何代码修改后，如果需要发布新版本，必须按此规范递增 versionCode 和 versionName。**

---

## 3. 构建规范

### 3.1 前置环境

```bash
export JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home
export ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

### 3.2 构建命令

```bash
# lightning-browser
$ cd /Volumes/data/tvapk/lightning-browser
$ chmod +x gradlew
$ ./gradlew :App:assembleDebug

# chromium-kiosk
$ cd /Volumes/data/tvapk/chromium-kiosk
$ chmod +x gradlew
$ ./gradlew :app:assembleDebug
```

### 3.3 构建后验证（必须执行）

```bash
AAPT="/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/36.0.0/aapt"

# 验证 lightning-browser
$AAPT dump badging lightning-browser/App/build/outputs/apk/debug/app-debug.apk

# 验证 chromium-kiosk
$AAPT dump badging chromium-kiosk/app/build/outputs/apk/debug/app-debug.apk
```

必须确认：
- `versionCode` 正确
- `versionName` 正确
- `package` name 匹配

---

## 4. 产物命名规范

### 4.1 文件名格式

构建成功后，APK 必须按以下格式复制到 `dist/` 目录：

```
# lightning-browser
tvapk-lightning-gecko-v{versionName}-{versionCode}-debug.apk
# 示例: tvapk-lightning-gecko-v1.4.5-1405-debug.apk

# chromium-kiosk
tvapk-chromium{ChromiumMajor}-v{versionName}.apk
# 示例: tvapk-chromium119-v1.0.1.apk
```

### 4.2 Symlink（快捷链接）

在 `dist/` 目录下维护指向最新版本的 symlink：

```bash
cd /Volumes/data/tvapk/dist

# lightning-browser
ln -sf tvapk-lightning-gecko-v1.4.5-1405-debug.apk tvapk-lightning-gecko-debug.apk

# chromium-kiosk
ln -sf tvapk-chromium119-v1.0.1.apk tvapk-chromium119.apk
```

---

## 5. Git 与 GitHub Release 规范

### 5.1 Commit 规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>
```

| type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `docs` | 文档修改 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具链/杂项 |

### 5.2 Tag 命名

```bash
# lightning-browser
git tag -a v{MAJOR.MINOR.PATCH}-gecko -m "描述"
# 示例: git tag -a v1.4.5-gecko -m "lightning-browser v1.4.5"

# chromium-kiosk
git tag -a v{MAJOR.MINOR.PATCH}-chromium{ChromiumMajor} -m "描述"
# 示例: git tag -a v1.0.1-chromium119 -m "chromium-kiosk v1.0.1"
```

### 5.3 GitHub Release 创建

```bash
# lightning-browser
gh release create v1.4.5-gecko \
  --title "lightning-browser v1.4.5 - 描述" \
  --notes "Release notes..." \
  /path/to/apk

# chromium-kiosk
gh release create v1.0.1-chromium119 \
  --title "chromium-kiosk v1.0.1 - 描述" \
  --notes "Release notes..." \
  /path/to/apk
```

---

## 6. 代码修改规范

### 6.1 修改前必读

1. **先阅读相关 CHANGELOG** — 了解历史变更和版本演进
2. **先阅读相关源码** — 理解现有实现
3. **确认修改范围** — 避免影响不相关的模块

### 6.2 修改后必须执行

1. **更新 versionCode 和 versionName**（如果需要发布）
2. **更新 CHANGELOG.md** — 在对应子项目 CHANGELOG 顶部新增条目
3. **构建验证** — 确保编译通过
4. **产物验证** — 确认 APK 版本号正确
5. **提交代码** — 遵循 Conventional Commits 规范
6. **创建 Tag** — 遵循 Tag 命名规范
7. **创建 GitHub Release** — 上传 APK 附件

---

## 7. 历史版本速查

| 项目 | 版本 | versionCode | versionName | Tag |
|------|------|-------------|-------------|-----|
| lightning-browser | v1.4.5 | 1405 | 1.4.5 | v1.4.5-gecko |
| lightning-browser | v1.4.4 | 1404 | 1.4.4 | (无) |
| lightning-browser | v1.4.3 | 1403 | 1.4.3 | (无) |
| lightning-browser | v1.4.2 | 1402 | 1.4.2 | (无) |
| chromium-kiosk | v1.0.1 | 119002 | 1.0.1-chromium119 | v1.0.1-chromium119 |
| chromium-kiosk | v1.0.0 | 119001 | 1.0.0-chromium119 | v1.0.0 |

---

## 8. 常见问题

### Q: 如何确定 versionCode 应该递增多少？
**A:** 查看对应子项目 `build.gradle` 中的当前值，按规范计算下一个值。不要跳过数字。

### Q: 两个子项目可以独立发布吗？
**A:** 可以。两个子项目有独立的版本号体系，互不影响。

### Q: 修改了公共文件（如 README.md）需要发版本吗？
**A:** 不需要。只有修改子项目代码且需要生成 APK 时才需要发版本。

### Q: dist/ 目录下的旧版本 APK 需要删除吗？
**A:** 不要删除。保留历史版本便于回溯和对比。

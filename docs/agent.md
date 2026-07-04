# Agent 编译与发布规范

本文件定义 `tvapk` 项目中两个 APK 子模块的**编译流程、版本号管理、产物命名和 GitHub Release 发布**规范。所有 Agent 在执行构建和发布任务时**必须**遵循此文档。

---

## 1. 项目结构

```
tvapk/
├── chromium-kiosk/     # 基于 Chromium WebView 的 kiosk 方案
├── lightning-browser/  # 基于 GeckoView 的浏览器方案
├── dist/               # 编译产物目录（APK 和 symlink）
└── docs/
    └── agent.md        # 本文件
```

| 子项目 | 包名 | 构建目录 | 输出 APK 名称 |
|--------|------|---------|--------------|
| `chromium-kiosk` | `com.pingfeng.tvapk.chromium` | `chromium-kiosk/app` | `app-debug.apk` |
| `lightning-browser` | `com.threethan.browser` | `lightning-browser/App` | `app-debug.apk` |

---

## 2. 版本号规范

### 2.1 Semantic Versioning

两个子项目均遵循 [语义化版本](https://semver.org/lang/zh-CN/)：`MAJOR.MINOR.PATCH`

| 位 | 含义 | 何时递增 |
|----|------|---------|
| **MAJOR** | 主版本，重大变更或内核更换 | 内核大版本更换、架构重构 |
| **MINOR** | 次版本，功能新增 | 新功能、新特性 |
| **PATCH** | 修订版本，修复问题 | Bug 修复、性能优化 |

### 2.2 versionCode 规则

`versionCode` 是 Android 系统用于比较版本的**整数**，必须是**递增的**。

#### lightning-browser (GeckoView)

```
versionCode = MAJOR * 1000 + MINOR * 100 + PATCH

# 示例
1.4.2 → 1402
1.4.5 → 1405
1.5.0 → 1500
2.0.0 → 2000
```

#### chromium-kiosk (Chromium WebView)

```
versionCode = <ChromiumMajor> * 1000 + <Patch>

# 示例
Chromium 119, patch 0  → 119000
Chromium 119, patch 1  → 119001  (1.0.0-chromium119)
Chromium 119, patch 2  → 119002  (1.0.1-chromium119)
Chromium 120, patch 0  → 120000  (1.0.0-chromium120)
```

> 注意：chromium-kiosk 的 `MAJOR` 位固定映射到内置 Chromium 内核主版本号，`MINOR.PATCH` 作为自身版本。

### 2.3 versionName 规则

| 子项目 | 格式 | 示例 |
|--------|------|------|
| lightning-browser | `MAJOR.MINOR.PATCH` | `1.4.5` |
| chromium-kiosk | `MAJOR.MINOR.PATCH-chromium<内核主版本>` | `1.0.1-chromium119` |

---

## 3. 编译规范

### 3.1 前置条件

```bash
# 环境变量（根据实际情况调整路径）
export JAVA_HOME=/Volumes/data/tvapk/temp/toolchains/jdk21/Contents/Home
export ANDROID_HOME=/Volumes/data/tvapk/temp/toolchains/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

### 3.2 编译步骤

#### Step 1: 确认版本号已更新

在 `build.gradle` 中确认 `versionCode` 和 `versionName`：

- `lightning-browser/App/build.gradle`
- `chromium-kiosk/app/build.gradle`

#### Step 2: 编译

```bash
# lightning-browser
$ cd /Volumes/data/tvapk/lightning-browser
$ ./gradlew :App:assembleDebug

# chromium-kiosk
$ cd /Volumes/data/tvapk/chromium-kiosk
$ ./gradlew :app:assembleDebug
```

#### Step 3: 验证 APK

```bash
AAPT="/Volumes/data/tvapk/temp/toolchains/android-sdk/build-tools/36.0.0/aapt"

#  lightning-browser
$ $AAPT dump badging lightning-browser/App/build/outputs/apk/debug/app-debug.apk

#  chromium-kiosk
$ $AAPT dump badging chromium-kiosk/app/build/outputs/apk/debug/app-debug.apk
```

必须检查：
- `versionCode` 是否正确
- `versionName` 是否正确
- `package` name 是否匹配预期包名

#### Step 4: 计算 SHA256

```bash
$ sha256sum <APK_PATH>
```

---

## 4. 产物命名规范

### 4.1 dist 目录产物

编译完成后，APK **必须**复制到 `dist/` 目录，并使用统一命名格式：

```
dist/
├── tvapk-lightning-gecko-v{versionName}-{versionCode}-debug.apk    # 具体版本
├── tvapk-lightning-gecko-debug.apk -> (symlink to latest)          # 快捷链接
├── tvapk-chromium119-v{versionName}.apk                            # 具体版本
└── tvapk-chromium119.apk -> (symlink to latest)                    # 快捷链接
```

#### lightning-browser 文件名格式

```
tvapk-lightning-gecko-v{MAJOR.MINOR.PATCH}-{versionCode}-debug.apk
```

| 占位符 | 示例值 | 说明 |
|--------|--------|------|
| `MAJOR.MINOR.PATCH` | `1.4.5` | versionName |
| `versionCode` | `1405` | build.gradle 中的 versionCode |

**示例：**
```
tvapk-lightning-gecko-v1.4.5-1405-debug.apk
```

#### chromium-kiosk 文件名格式

```
tvapk-chromium{ChromiumMajor}-v{MAJOR.MINOR.PATCH}.apk
```

| 占位符 | 示例值 | 说明 |
|--------|--------|------|
| `ChromiumMajor` | `119` | 内置 Chromium 内核主版本 |
| `MAJOR.MINOR.PATCH` | `1.0.1` | 项目自身的 versionName |

**示例：**
```
tvapk-chromium119-v1.0.1.apk
```

### 4.2 快捷链接 (Symlink)

在 `dist/` 目录下维护**指向最新版本的 symlink**，方便外部脚本引用：

```bash
# lightning-browser
cd /Volumes/data/tvapk/dist
ln -sf tvapk-lightning-gecko-v1.4.5-1405-debug.apk tvapk-lightning-gecko-debug.apk

# chromium-kiosk
ln -sf tvapk-chromium119-v1.0.1.apk tvapk-chromium119.apk
```

---

## 5. GitHub Release 规范

### 5.1 Tag 命名

```
# lightning-browser
v{MAJOR.MINOR.PATCH}-gecko
# 示例: v1.4.5-gecko

# chromium-kiosk
v{MAJOR.MINOR.PATCH}-chromium{ChromiumMajor}
# 示例: v1.0.1-chromium119
```

### 5.2 Release 标题

```
# lightning-browser
lightning-browser v{MAJOR.MINOR.PATCH} - {一句话描述}
# 示例: lightning-browser v1.4.5 - 修复长时间运行OOM崩溃

# chromium-kiosk
chromium-kiosk v{MAJOR.MINOR.PATCH} - {一句话描述}
# 示例: chromium-kiosk v1.0.1 - 修复长时间运行OOM崩溃
```

### 5.3 Release 描述模板

每个 Release 描述必须包含以下结构：

```markdown
## lightning-browser v1.4.5 (GeckoView)

### 修复
- 修复内容 1
- 修复内容 2

### 新增
- 新增内容 1

### 变更
- 应用版本更新为 `versionCode=1405`、`versionName=1.4.5`。

### 验证
- `:App:assembleDebug` 构建通过。
- `aapt dump badging` 确认 `versionCode='1405'`、`versionName='1.4.5'`。

### 产物
| 项目 | 值 |
|------|-----|
| versionCode | 1405 |
| versionName | 1.4.5 |
| SHA256 | f43e58b69231a6f229bd5d512367e5fb358f033dda112edace4a7aa42b141b78 |
| 大小 | ~91MB |
```

### 5.4 Release 附件

每个 Release **必须**上传对应的 APK 文件作为附件。

---

## 6. 完整发布流程 Checklist

Agent 在发布新版本时，按以下顺序执行：

- [ ] **1. 确认修复/变更内容** — 明确本次发布的修改范围
- [ ] **2. 更新 versionCode 和 versionName** — 编辑对应子项目的 `build.gradle`
- [ ] **3. 编写 CHANGELOG** — 在对应子项目的 `CHANGELOG.md` 顶部新增版本记录
- [ ] **4. 编译** — 执行 `./gradlew assembleDebug`
- [ ] **5. 验证** — 使用 `aapt dump badging` 确认 versionCode/versionName
- [ ] **6. 复制到 dist** — 按命名规范复制并重命名 APK
- [ ] **7. 更新 symlink** — 更新快捷链接指向最新版本
- [ ] **8. 计算 SHA256** — 记录产物哈希
- [ ] **9. 提交代码** — `git add -A && git commit && git push`
- [ ] **10. 创建 Tag** — `git tag -a vX.Y.Z-xxx && git push origin --tags`
- [ ] **11. 创建 GitHub Release** — 使用 `gh release create` 并上传 APK
- [ ] **12. 验证 Release** — 确认 GitHub 页面可正常下载

---

## 7. 历史版本对照表

| Release | versionCode | versionName | Tag | 文件名 |
|---------|-------------|-------------|-----|--------|
| lightning-browser v1.4.5 | 1405 | 1.4.5 | v1.4.5-gecko | tvapk-lightning-gecko-v1.4.5-1405-debug.apk |
| lightning-browser v1.4.4 | 1404 | 1.4.4 | v1.4.4-gecko | tvapk-lightning-gecko-v1.4.4-1404-debug.apk |
| lightning-browser v1.4.3 | 1403 | 1.4.3 | (无) | tvapk-lightning-gecko-v1.4.3-1403-debug.apk |
| lightning-browser v1.4.2 | 1402 | 1.4.2 | (无) | tvapk-lightning-gecko-v1.4.2-1402-debug.apk |
| chromium-kiosk v1.0.1 | 119002 | 1.0.1-chromium119 | v1.0.1-chromium119 | tvapk-chromium119-v1.0.1.apk |
| chromium-kiosk v1.0.0 | 119001 | 1.0.0-chromium119 | v1.0.0 | tvapk-chromium119.apk (旧) |

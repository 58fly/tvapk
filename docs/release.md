# 发布规范

定义 Tag 命名、GitHub Release 创建和产物命名规则。

---

## 1. Tag 命名

### 1.1 格式

```
# lightning-browser
v{MAJOR.MINOR.PATCH}-gecko
# 示例: v1.4.5-gecko

# chromium-kiosk
v{MAJOR.MINOR.PATCH}-chromium{ChromiumMajor}
# 示例: v1.0.1-chromium119
```

### 1.2 创建

```bash
# lightning-browser
git tag -a v1.4.5-gecko -m "lightning-browser v1.4.5"

# chromium-kiosk
git tag -a v1.0.1-chromium119 -m "chromium-kiosk v1.0.1"

# 推送
git push origin --tags
```

---

## 2. GitHub Release 创建

### 2.1 命令

```bash
# lightning-browser
gh release create v1.4.5-gecko \
  --repo 58fly/tvapk \
  --title "lightning-browser v1.4.5 - {描述}" \
  --notes "## lightning-browser v1.4.5 (GeckoView)\n\n### 修复\n- ...\n\n### 新增\n- ...\n\n### 变更\n- 应用版本更新为 versionCode=1405, versionName=1.4.5\n\n### 产物\n| 项目 | 值 |\n|------|-----|\n| versionCode | 1405 |\n| versionName | 1.4.5 |\n| SHA256 | ... |\n| 大小 | ~91MB |\n" \
  /Volumes/data/tvapk/dist/tvapk-lightning-gecko-v1.4.5-1405-debug.apk

# chromium-kiosk
gh release create v1.0.1-chromium119 \
  --repo 58fly/tvapk \
  --title "chromium-kiosk v1.0.1 - {描述}" \
  --notes "## chromium-kiosk v1.0.1 (Chromium WebView)\n\n### 修复\n- ...\n\n### 新增\n- ...\n\n### 变更\n- 应用版本更新为 versionCode=119002, versionName=1.0.1-chromium119\n\n### 产物\n| 项目 | 值 |\n|------|-----|\n| versionCode | 119002 |\n| versionName | 1.0.1-chromium119 |\n| SHA256 | ... |\n| 大小 | ~87MB |\n" \
  /Volumes/data/tvapk/dist/tvapk-chromium119-v1.0.1.apk
```

### 2.2 Release 超时处理

如果 `gh release create` 命令超时但 Release 已创建为 Draft：

```bash
# 1. 检查 Draft Release
gh release list --repo 58fly/tvapk

# 2. 上传 APK（如果还没上传）
gh release upload v1.0.1-chromium119 /path/to/apk --repo 58fly/tvapk --clobber

# 3. 取消 Draft 状态
g h release edit v1.0.1-chromium119 --repo 58fly/tvapk --draft=false
```

---

## 3. 产物命名

### 3.1 dist 目录文件名

| 项目 | 文件名格式 | 示例 |
|------|-----------|------|
| lightning-browser | `tvapk-lightning-gecko-v{versionName}-{versionCode}-debug.apk` | `tvapk-lightning-gecko-v1.4.5-1405-debug.apk` |
| chromium-kiosk | `tvapk-chromium{ChromiumMajor}-v{versionName}.apk` | `tvapk-chromium119-v1.0.1.apk` |

### 3.2 Symlink（快捷链接）

```bash
cd /Volumes/data/tvapk/dist

# lightning-browser
ln -sf tvapk-lightning-gecko-v1.4.5-1405-debug.apk tvapk-lightning-gecko-debug.apk

# chromium-kiosk
ln -sf tvapk-chromium119-v1.0.1.apk tvapk-chromium119.apk
```

### 3.3 复制命令

```bash
# lightning-browser
cp /Volumes/data/tvapk/lightning-browser/App/build/outputs/apk/debug/app-debug.apk \
   /Volumes/data/tvapk/dist/tvapk-lightning-gecko-v1.4.5-1405-debug.apk

# chromium-kiosk
cp /Volumes/data/tvapk/chromium-kiosk/app/build/outputs/apk/debug/app-debug.apk \
   /Volumes/data/tvapk/dist/tvapk-chromium119-v1.0.1.apk
```

---

## 4. Commit 规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>(<scope>): <subject>

<body>
```

| type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `docs` | 文档修改 |
| `style` | 代码格式 |
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试 |
| `chore` | 构建/工具链 |

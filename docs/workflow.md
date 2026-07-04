# 工作流程

代码修改后的完整操作流程。

---

## 1. 修改前

- [ ] 阅读相关 CHANGELOG.md 了解历史变更
- [ ] 阅读需要修改的源码，理解现有实现
- [ ] 确认修改范围，避免影响不相关模块

---

## 2. 修改代码

- [ ] 执行代码修改
- [ ] 确保代码符合项目风格
- [ ] 添加/修改注释

---

## 3. 修改后

### 3.1 更新版本号（如果发布新版本）

- [ ] 编辑 `lightning-browser/App/build.gradle` 或 `chromium-kiosk/app/build.gradle`
- [ ] 递增 `versionCode`（见 `/docs/versioning.md`）
- [ ] 更新 `versionName`（见 `/docs/versioning.md`）

### 3.2 更新 CHANGELOG

- [ ] 在对应子项目的 `CHANGELOG.md` 顶部新增条目
- [ ] 包含：修复/新增/变更/验证/产物 等章节
- [ ] 更新 `README.md`（如需要）

---

## 4. 构建

- [ ] 执行编译（见 `/docs/build.md` §2）
- [ ] 验证 APK 信息（见 `/docs/build.md` §3）
- [ ] 计算 SHA256

---

## 5. 发布到 dist

- [ ] 按命名规范复制 APK（见 `/docs/release.md` §3）
- [ ] 更新 symlink

---

## 6. Git 操作

- [ ] `git add -A`
- [ ] `git commit -m "type(scope): subject"`
- [ ] `git push origin main`

---

## 7. GitHub Release

- [ ] 创建 Tag（见 `/docs/release.md` §1）
- [ ] `git push origin --tags`
- [ ] 创建 GitHub Release 并上传 APK（见 `/docs/release.md` §2）
- [ ] 验证 GitHub Release 页面可正常下载

---

## 8. 完成

- [ ] 确认 GitHub Release 已发布
- [ ] 确认 APK 可正常下载安装
- [ ] 确认版本号正确

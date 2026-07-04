# 版本号规范

定义 `tvapk` 两个子项目的 `versionCode` 和 `versionName` 规则。

---

## 1. 通用原则

- `versionCode` 必须是**单调递增**的正整数（Android 系统通过它比较版本新旧）
- `versionName` 是**人类可读**的版本字符串（展示给用户）
- 每次发布新版本，两者**必须同步更新**

---

## 2. lightning-browser (GeckoView)

### 2.1 versionCode

```
versionCode = MAJOR * 1000 + MINOR * 100 + PATCH
```

| versionName | 计算 | versionCode |
|-------------|------|-------------|
| `1.4.2` | `1*1000 + 4*100 + 2` | `1402` |
| `1.4.5` | `1*1000 + 4*100 + 5` | `1405` |
| `1.5.0` | `1*1000 + 5*100 + 0` | `1500` |
| `2.0.0` | `2*1000 + 0*100 + 0` | `2000` |

### 2.2 versionName

```
格式：MAJOR.MINOR.PATCH
示例："1.4.5"
```

### 2.3 文件位置

```
lightning-browser/App/build.gradle:12-13
```

```gradle
defaultConfig {
    versionCode 1405
    versionName "1.4.5"
}
```

---

## 3. chromium-kiosk (Chromium WebView)

### 3.1 versionCode

```
versionCode = <ChromiumMajor> * 1000 + <Patch>
```

- `ChromiumMajor`: 内置 Chromium 内核的主版本号（如 `119`）
- `Patch`: 项目自身的补丁版本号（从 0 开始递增）

| versionName | ChromiumMajor | Patch | 计算 | versionCode |
|-------------|---------------|-------|------|-------------|
| `1.0.0-chromium119` | `119` | `0` | `119*1000 + 0` | `119000` |
| `1.0.1-chromium119` | `119` | `1` | `119*1000 + 1` | `119001` |
| `1.0.1-chromium119` | `119` | `2` | `119*1000 + 2` | `119002` |
| `1.0.0-chromium120` | `120` | `0` | `120*1000 + 0` | `120000` |

### 3.2 versionName

```
格式：MAJOR.MINOR.PATCH-chromium<内核主版本>
示例："1.0.1-chromium119"
```

### 3.3 文件位置

```
chromium-kiosk/app/build.gradle:17-18
```

```gradle
defaultConfig {
    versionCode 119002
    versionName '1.0.1-chromium119'
}
```

---

## 4. 历史版本对照

| 项目 | versionName | versionCode | 文件 |
|------|-------------|-------------|------|
| lightning-browser | `1.4.5` | `1405` | `App/build.gradle` |
| lightning-browser | `1.4.4` | `1404` | `App/build.gradle` |
| lightning-browser | `1.4.3` | `1403` | `App/build.gradle` |
| lightning-browser | `1.4.2` | `1402` | `App/build.gradle` |
| chromium-kiosk | `1.0.1-chromium119` | `119002` | `app/build.gradle` |
| chromium-kiosk | `1.0.0-chromium119` | `119001` | `app/build.gradle` |

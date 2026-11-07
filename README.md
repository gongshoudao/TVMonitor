# TV Pulse (电视时钟) — Android TV 屏幕使用统计与家庭护眼管家

**TV Pulse（电视时钟）** 是一款专为 Android TV 16:9 客厅大屏与遥控器五向键（D-Pad）设计的**真实系统使用时长统计与家庭防沉迷护眼应用**。应用通过调用 Android 底层 `UsageStatsManager`、`PackageManager`、`ConnectivityManager`、`TvInputManager`、`CameraManager` 等系统 API，采集 100% 真实的电视开机与应用使用数据，拒绝写死假文案与虚假成员档案。

---

## 核心功能模块

### 1. 今日看板 (Dashboard)
- **实时系统遥测概览**：顶部实时检测网络制式（千兆有线 / 5G Wi-Fi / 2.4G Wi-Fi / 离线）、系统时钟、当前生效家庭档案（默认为 `主人`）以及设备型号（如 `BRAVIA 4K`）。
- **今日累计时长与屏幕健康指数**：基于 `UsageEvents`（`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` / `ACTIVITY_STOPPED`）精确切片计算今日总观看时长、家庭护眼配额消耗百分比、单次最长连续观看时长及动态综合健康评分。
- **正在运行与常用应用**：自动过滤系统桌面 Launcher、屏保（如 `backdrop`、`Living Decor`）及本应用自身，展示真实观看时长 Top 应用与 `TvInputManager` 物理信号源（HDMI / AV）连接状态。
- **24 小时大屏活跃峰值分布**：按 2 小时时间窗聚合今日 00:00–24:00 活跃分钟数与主导应用分类（影音 / 少儿 / 知识健身），自动高亮午间与晚间黄金档。
- **护眼与休眠守则**：支持一键触发 20-20-20 护眼眨眼引导、切换全局暖色防蓝光滤镜、开启客厅静默熄屏伴听模式。

### 2. 应用时长排行 (Apps Ranking)
- **多维时间窗与分类筛选**：支持按 **今日 / 近 7 日 / 本月（30 天）** 以及 **全部 / 影视追剧 / 儿童动画 / 知识与科普 / 游戏与运动 / 系统与投屏** 实时筛选。
- **应用深度洞察面板 (Spotlight Inspector)**：
  - 真实提取应用包名、应用图标、启动次数、区间总时长与日均时长。
  - 绘制近 7 天每日真实使用时长折线趋势图（Sparkline）。
  - 展示该应用在 **黄金档 (19:00–22:00) / 日间 (06:00–19:00) / 深夜 (22:00–06:00)** 的真实活跃占比。
  - 支持直接从详情面板**启动该应用**、配置**单应用每日限额（30m / 60m / 90m / 120m / 不限额）**及**晚间宵禁锁定**。

### 3. 家庭健康模式 (Family Limits)
- **纯净家庭成员档案**：默认仅内置 `主人`（本机默认主账号），无任何虚构家庭成员；支持家长按需添加自定义成员档案并独立配置工作日/周末配额。
- **防沉迷与就寝锁屏时间**：
  - 工作日单日限额步进调节（`-30m` / `+30m`，30 分钟至 4 小时）。
  - 周末及节假日单日限额步进调节（1 小时至 5 小时）。
  - 就寝强制熄屏开关与连续观看护眼间隔（30 / 40 / 45 / 60 分钟）切换。
- **分类访问策略与 4 位家长 PIN 锁**：按少儿、网课、游戏投屏、影视剧场四大分类配置访问策略（允许 / 无限额 / 密码锁 / 已封禁），支持修改 4 位家长安全 PIN 码与一键模拟全屏防沉迷锁屏拦截。

### 4. 周度报告与健康洞察 (Weekly Reports)
- **本周 / 上周对比切换**：汇总全周总开机时长、环比上周增降幅度、达标天数（`/7`）与工作日日均时长。
- **每日分类堆叠柱状图**：按周一至周日展示影视、少儿、游戏、教育/系统四类真实时长堆叠分布，自动标注全周峰值日与最低日。
- **时段热力分布环形图**：统计黄金档（19–22 点）、午间档（12–14 点）、深夜（23 点后）真实时长占比。
- **局域网扫码同步周报**：内置轻量级局域网 HTTP 报告服务与零依赖 QR 二维码生成器（[`QrAndReportServer.kt`](app/src/main/java/com/tvpulse/tracker/util/QrAndReportServer.kt)），手机在同一局域网下扫码即可在浏览器查看移动端排版周报。

### 5. 系统权限与全局设置 (Settings)
- 一键跳转或通过 ADB 授予 `PACKAGE_USAGE_STATS`（应用使用情况访问权限）与 `SYSTEM_ALERT_WINDOW`（跨应用护眼滤镜悬浮窗权限），配置全天家庭总护眼额度与夜间蓝光滤镜自动开启时间。

---

## Android TV 系统检测能力边界说明

| 检测维度 | 支持状态 | 实现机制与说明 |
| :--- | :---: | :--- |
| 各应用前台真实时长 / 启动次数 / 最近活跃时间 | 已实现 | `UsageStatsManager.queryEvents()` + `queryUsageStats()` 精确会话切片 |
| 24 小时分时段活跃热力与近 7/30 天历史趋势 | 已实现 | 按小时桶与自然日聚合真实前台会话，自动剔除桌面 Launcher、屏保与本应用 |
| 已安装 TV 应用图标、名称与智能分类 | 已实现 | `PackageManager` 扫描 `CATEGORY_LEANBACK_LAUNCHER` 与已安装应用 |
| 真实网络制式（千兆有线 / 5G / 2.4G Wi-Fi） | 已实现 | `ConnectivityManager` + `WifiManager.connectionInfo.frequency` |
| 物理信号源（HDMI / AV）列表与连接状态 | 已实现 | `TvInputManager.tvInputList` + `getInputState()` |
| 摄像头硬件在位检测 | 已实现 | `CameraManager.cameraIdList` 实时检测外接或内置摄像头 |
| 第三方应用内部具体播放剧集名称 | 系统受限 | Android 沙盒限制第三方普通应用读取其他 App 内部页面路由；仅在目标应用公开 `MediaSession` 时可感知播放态 |
| 无摄像头硬件时的观看距离测算 | 硬件受限 | 大多数普通电视无 ToF/摄像头硬件，应用会如实显示「未接摄像头」而不会伪造视距数据 |

---

## 技术栈与项目结构

- **开发语言与 UI 框架**：Kotlin 2.0 + Jetpack Compose (Material 3) + Android TV Leanback 适配
- **遥控器焦点交互**：自定义 [`Modifier.tvClickable`](app/src/main/java/com/tvpulse/tracker/ui/components/TvCommonComponents.kt) 与 [`Modifier.tvFocusableCard`](app/src/main/java/com/tvpulse/tracker/ui/components/TvCommonComponents.kt)，支持五向键获焦 `1.038x` 缩放动画、`2.5.dp` 高亮边框与低透明度遮罩，顶部获焦自动平滑滚回顶部
- **最低系统版本**：`minSdk = 26` (Android 8.0+)，`targetSdk = 35`

```text
app/src/main/java/com/tvpulse/tracker/
├── MainActivity.kt                  # TV 主入口、五向键导航栏与全局浮层编排
├── data/
│   ├── FamilyPolicyStore.kt         # 家庭档案、配额、PIN 码与单应用限额持久化
│   └── TvUsageRepository.kt         # UsageStatsManager 真实遥测采集与会话切片引擎
├── model/
│   └── TvModels.kt                  # UI 状态模型与真实统计数据结构
├── service/
│   └── TvMonitorService.kt          # 后台护眼提醒与防沉迷监测服务
├── ui/
│   ├── components/                  # 遥控器焦点组件、锁屏拦截页、弹窗与通用控件
│   ├── screens/                     # 五大核心大屏页面 (Dashboard / Apps / Family / Reports / Settings)
│   └── theme/                       # 暗色护眼主题色板 (TvPulseTheme)
└── util/
    └── QrAndReportServer.kt         # 局域网手机扫码周报 HTTP 服务与零依赖 QR Code 生成器
```

---

## 构建与真机部署

### 1. 编译 Debug APK
确保本机已安装 **JDK 17**（或 JDK 21）与 Android SDK：

```bash
./gradlew assembleDebug
```

生成的安装包路径为：
`app/build/outputs/apk/debug/app-debug.apk`

### 2. 安装至 Android TV 并授予使用统计权限
通过 ADB 连接电视后执行：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 授予核心应用使用情况统计权限（部分精简版电视 ROM 设置页隐藏了授权入口，推荐直接 ADB 授权）
adb shell appops set com.tvpulse.tracker GET_USAGE_STATS allow

# （可选）授予跨应用护眼滤镜悬浮窗权限
adb shell appops set com.tvpulse.tracker SYSTEM_ALERT_WINDOW allow

# 启动应用
adb shell am start -n com.tvpulse.tracker/.MainActivity
```

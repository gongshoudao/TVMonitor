package com.tvpulse.tracker.model

import android.graphics.drawable.Drawable

enum class NavTab(val id: String, val label: String) {
    DASHBOARD("dashboard", "今日看板"),
    APPS("apps", "应用排行"),
    FAMILY_LIMITS("family-limits", "家庭健康模式"),
    REPORTS("reports", "周度报告"),
    SETTINGS("settings", "设置")
}

enum class AppRangeFilter(val label: String, val days: Int) {
    TODAY("今日 Today", 1),
    SEVEN_DAYS("近7日 7-Day", 7),
    MONTH("本月 Month", 30)
}

enum class AppCategory(val displayName: String, val shortLabel: String) {
    ALL("全部分类", "全部"),
    VIDEO("影视追剧", "影音"),
    KIDS("儿童动画", "少儿"),
    EDUCATION("知识与科普", "知识/教育"),
    GAME_FITNESS("游戏与运动", "游戏/健身"),
    SYSTEM_CAST("系统与投屏", "系统")
}

enum class CategoryAccessPolicy(val label: String) {
    ALLOW("允许"),
    UNLIMITED("无限额"),
    PIN_LOCK("密码锁"),
    BLOCKED("已封禁")
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val totalTimeMs: Long,
    val todayTimeMs: Long,
    val lastTimeUsedMs: Long,
    val launchCount: Int,
    val percentageOfTotal: Int,
    val isCurrentlyRunning: Boolean,
    val dailyLimitMinutes: Int, // 0 means no specific app limit
    val inNightCurfew: Boolean,
    val trendPercent: Int, // vs previous period
    val dailySparklineHours: List<Float>, // 7 days (Mon..Sun or past 7 days)
    val hourlyMinutesToday: List<Int>, // 24 hours (0..23)
    val primeEveningPercent: Int, // 19:00 - 22:00 share
    val daytimePercent: Int,      // 06:00 - 19:00 share
    val lateNightPercent: Int,    // 22:00 - 06:00 share
    val activeMediaTitle: String?, // Real MediaSession title if available
    val iconDrawable: Drawable? = null
) {
    val isOverLimit: Boolean
        get() = dailyLimitMinutes > 0 && (todayTimeMs / 60_000L) >= dailyLimitMinutes
}

data class HourlySlotStat(
    val label: String,        // "02", "04", ..., "20", "现在", "22", "24"
    val startHour: Int,
    val endHour: Int,
    val totalMinutes: Int,
    val dominantCategory: AppCategory,
    val isCurrentSlot: Boolean,
    val isFutureSlot: Boolean
)

data class TvHardwareInputInfo(
    val id: String,
    val name: String,
    val typeLabel: String, // e.g. "HDMI 1", "HDMI 2", "AV"
    val isConnected: Boolean
)

data class DayStackedUsage(
    val dayOfWeekLabel: String, // "周一" .. "周日"
    val dateLabel: String,      // "MM-dd"
    val videoHours: Float,
    val kidsHours: Float,
    val gameHours: Float,
    val eduSystemHours: Float,
    val isToday: Boolean
) {
    val totalHours: Float
        get() = videoHours + kidsHours + gameHours + eduSystemHours
}

data class TimeOfDayDistribution(
    val primeEveningMinutes: Int, // 19:00 - 22:00
    val noonBreakMinutes: Int,    // 12:00 - 14:00
    val otherDaytimeMinutes: Int, // Other 07:00 - 23:00
    val lateNightMinutes: Int     // 23:00 - 07:00
) {
    private val rawSum: Int
        get() = primeEveningMinutes + noonBreakMinutes + otherDaytimeMinutes + lateNightMinutes

    val totalMinutes: Int
        get() = rawSum.coerceAtLeast(1)

    val primePercent: Int
        get() = if (rawSum <= 0) 0 else ((primeEveningMinutes * 100f) / totalMinutes).toInt()
    val noonPercent: Int
        get() = if (rawSum <= 0) 0 else ((noonBreakMinutes * 100f) / totalMinutes).toInt()
    val otherPercent: Int
        get() = if (rawSum <= 0) 0 else ((otherDaytimeMinutes * 100f) / totalMinutes).toInt()
    val lateNightPercent: Int
        get() = if (rawSum <= 0) 0 else (100 - primePercent - noonPercent - otherPercent).coerceAtLeast(0)
}

data class FamilyMemberProfile(
    val id: String,
    val name: String,
    val ageOrRoleTag: String,     // e.g. "7岁", "13岁", "管理", "公共"
    val subtitle: String,         // e.g. "儿童护眼专属模式", "青少年学习模式", "影院与剧场模式"
    val isRestricted: Boolean,    // true = 已受控, false = 自由观影
    val weekdayLimitMinutes: Int, // e.g. 90 (0 = unlimited)
    val weekendLimitMinutes: Int, // e.g. 150 (0 = unlimited)
    val curfewEnabled: Boolean,
    val curfewStartHour: Int,     // e.g. 21
    val curfewEndHour: Int,       // e.g. 7
    val eyeRestEnabled: Boolean,
    val eyeRestIntervalMinutes: Int, // e.g. 40
    val eyeRestDurationMinutes: Int, // e.g. 10
    val preferredCategories: Set<AppCategory>
)

data class MemberUsageSummary(
    val profile: FamilyMemberProfile,
    val todayUsedMinutes: Int,
    val todayQuotaMinutes: Int, // 0 if unlimited
    val weeklyUsedMinutes: Int,
    val weeklyQuotaMinutes: Int,
    val topAppNames: List<String>,
    val primaryTimeWindow: String,
    val weekOverWeekPercent: Int,
    val hasCurfewViolation: Boolean
) {
    val todayRemainingMinutes: Int
        get() = if (todayQuotaMinutes <= 0) Int.MAX_VALUE else (todayQuotaMinutes - todayUsedMinutes).coerceAtLeast(0)

    val todayProgressFraction: Float
        get() = if (todayQuotaMinutes <= 0) 1f else (todayUsedMinutes.toFloat() / todayQuotaMinutes.toFloat()).coerceIn(0f, 1f)
}

data class WeeklyReportData(
    val weekOffset: Int, // 0 = current week, -1 = last week
    val weekNumber: Int,
    val dateRangeText: String,
    val totalWeeklyMinutes: Int,
    val weekOverWeekChangePercent: Int,
    val dailyBars: List<DayStackedUsage>,
    val compliantDaysCount: Int,
    val weekdayAvgHours: Float,
    val peakDayIndex: Int,
    val lowestDayIndex: Int,
    val timeDistribution: TimeOfDayDistribution,
    val eyeCareCompletionPercent: Int,
    val familyLevel: Int,
    val aiAdvisorComment: String,
    val blueLightReducedHours: Float,
    val qrReportContent: String,
    val qrLocalUrl: String?
)

data class SystemTelemetrySnapshot(
    val hasUsagePermission: Boolean,
    val hasOverlayPermission: Boolean,
    val networkLabel: String,      // "5G Wi-Fi 在线", "2.4G Wi-Fi 在线", "以太网在线", "离线"
    val isNetworkOnline: Boolean,
    val currentTimeText: String,   // "HH:mm"
    val greetingTitle: String,     // "早上好 / 下午好 / 晚上好"
    val deviceModelTag: String,    // e.g. "Android TV 16:9 · BRAVIA 4K"
    val cameraStatusText: String,  // Real CameraManager hardware check
    val hasCameraHardware: Boolean,
    val tvInputSources: List<TvHardwareInputInfo>,
    val monitoredAppsCount: Int,
    val todayTotalMinutes: Int,
    val dailyFamilyQuotaMinutes: Int,
    val longestContinuousMinutes: Int,
    val blinkReminderCountToday: Int,
    val healthScore: Int,
    val healthRatingLabel: String,
    val healthSubLabel: String,
    val noonPeakSummary: String,
    val eveningPrimeSummary: String,
    val nightLightAutoHour: Int,
    val nightLightAutoMinute: Int,
    val nightLightCountdownText: String,
    val isBlueLightFilterActive: Boolean,
    val topRunningAndFrequentApps: List<AppUsageInfo>,
    val rankedApps: List<AppUsageInfo>,
    val hourlySlots: List<HourlySlotStat>,
    val weeklyReport: WeeklyReportData,
    val memberSummaries: List<MemberUsageSummary>,
    val activeProfileId: String,
    val categoryPolicies: Map<AppCategory, CategoryAccessPolicy>
)

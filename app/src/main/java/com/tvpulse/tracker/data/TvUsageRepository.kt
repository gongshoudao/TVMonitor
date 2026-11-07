package com.tvpulse.tracker.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.AppRangeFilter
import com.tvpulse.tracker.model.AppUsageInfo
import com.tvpulse.tracker.model.DayStackedUsage
import com.tvpulse.tracker.model.FamilyMemberProfile
import com.tvpulse.tracker.model.HourlySlotStat
import com.tvpulse.tracker.model.MemberUsageSummary
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.model.TimeOfDayDistribution
import com.tvpulse.tracker.model.TvHardwareInputInfo
import com.tvpulse.tracker.model.WeeklyReportData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class TvUsageRepository(
    private val context: Context,
    private val policyStore: FamilyPolicyStore
) {
    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val appMetaCache = mutableMapOf<String, InstalledAppMeta>()

    private data class InstalledAppMeta(
        val packageName: String,
        val appName: String,
        val category: AppCategory,
        val icon: Drawable?,
        val isSystemApp: Boolean
    )

    private data class ForegroundSession(
        val packageName: String,
        val startMs: Long,
        val endMs: Long
    ) {
        val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        if (mode == AppOpsManager.MODE_ALLOWED) return true
        val now = System.currentTimeMillis()
        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 86_400_000L,
            now
        )
        return !stats.isNullOrEmpty()
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun detectNetworkStatus(): Pair<String, Boolean> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "网络未知" to false
            val activeNet = cm.activeNetwork ?: return "离线" to false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return "离线" to false
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "千兆有线 在线" to true
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    @Suppress("DEPRECATION")
                    val freq = wifiManager?.connectionInfo?.frequency ?: 0
                    if (freq >= 4900) {
                        "5G 在线" to true
                    } else if (freq in 2400..2500) {
                        "2.4G 在线" to true
                    } else {
                        "Wi-Fi 在线" to true
                    }
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络 在线" to true
                else -> "在线" to true
            }
        } catch (e: Exception) {
            "Wi-Fi 在线" to true
        }
    }

    fun detectCameraHardware(): Pair<String, Boolean> {
        return try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val ids = camManager?.cameraIdList ?: emptyArray()
            if (ids.isNotEmpty()) {
                "AI 视距检测：已就绪 (检测到 ${ids.size} 个摄像头)" to true
            } else {
                "AI 视距检测：未接摄像头 (外接USB摄像头可开启)" to false
            }
        } catch (e: Exception) {
            "AI 视距检测：未检测到摄像头硬件" to false
        }
    }

    fun detectTvInputSources(): List<TvHardwareInputInfo> {
        return try {
            val tvInputManager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
                ?: return emptyList()
            val inputs = tvInputManager.tvInputList ?: return emptyList()
            inputs.mapIndexed { idx, info ->
                val label = info.loadLabel(context)?.toString()?.takeIf { it.isNotBlank() }
                    ?: "输入源 ${idx + 1}"
                val typeStr = when (info.type) {
                    TvInputInfo.TYPE_HDMI -> "HDMI ${idx + 1}"
                    TvInputInfo.TYPE_TUNER -> "数字电视"
                    TvInputInfo.TYPE_COMPOSITE -> "AV 视频"
                    TvInputInfo.TYPE_COMPONENT -> "色差分量"
                    else -> "外部信号"
                }
                val state = runCatching { tvInputManager.getInputState(info.id) }
                    .getOrDefault(TvInputManager.INPUT_STATE_CONNECTED)
                TvHardwareInputInfo(
                    id = info.id,
                    name = label,
                    typeLabel = typeStr,
                    isConnected = state == TvInputManager.INPUT_STATE_CONNECTED
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadInstalledAppsMap(): Map<String, InstalledAppMeta> {
        val result = mutableMapOf<String, InstalledAppMeta>()
        val launcherIntents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        val packages = mutableSetOf<String>()
        launcherIntents.forEach { intent ->
            runCatching {
                packageManager.queryIntentActivities(intent, 0).forEach { resolveInfo ->
                    resolveInfo.activityInfo?.packageName?.let { packages.add(it) }
                }
            }
        }
        runCatching {
            packageManager.getInstalledApplications(0).forEach { appInfo ->
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                ) {
                    packages.add(appInfo.packageName)
                }
            }
        }

        packages.forEach { pkg ->
            if (!shouldIgnoreLauncherOrSelf(pkg)) {
                getOrResolveAppMeta(pkg)?.let { result[pkg] = it }
            }
        }
        return result
    }

    private fun getOrResolveAppMeta(packageName: String): InstalledAppMeta? {
        appMetaCache[packageName]?.let { return it }
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val rawLabel = packageManager.getApplicationLabel(appInfo).toString().trim()
            val label = if (rawLabel.isBlank() || rawLabel == packageName) {
                friendlyFallbackName(packageName)
            } else {
                rawLabel
            }
            val icon = runCatching { packageManager.getApplicationIcon(appInfo) }.getOrNull()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            val category = classifyPackage(packageName, label, appInfo, isSystem)
            val meta = InstalledAppMeta(
                packageName = packageName,
                appName = label,
                category = category,
                icon = icon,
                isSystemApp = isSystem
            )
            appMetaCache[packageName] = meta
            meta
        } catch (e: Exception) {
            null
        }
    }

    private fun friendlyFallbackName(packageName: String): String {
        val last = packageName.substringAfterLast('.')
        return last.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun classifyPackage(
        packageName: String,
        label: String,
        appInfo: ApplicationInfo,
        isSystem: Boolean
    ): AppCategory {
        val combined = "${packageName.lowercase(Locale.ROOT)} ${label.lowercase(Locale.ROOT)}"
        if (combined.contains("kid") || combined.contains("child") || combined.contains("少儿") ||
            combined.contains("儿童") || combined.contains("宝宝") || combined.contains("卡通") ||
            combined.contains("动画") || combined.contains("小企鹅")
        ) {
            return AppCategory.KIDS
        }
        if (combined.contains("edu") || combined.contains("study") || combined.contains("school") ||
            combined.contains("课堂") || combined.contains("教育") || combined.contains("学习") ||
            combined.contains("科普") || combined.contains("读书") || combined.contains("bilibili") ||
            combined.contains("哔哩") || combined.contains("小电视")
        ) {
            return AppCategory.EDUCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return AppCategory.GAME_FITNESS
        }
        if (combined.contains("game") || combined.contains("fit") || combined.contains("keep") ||
            combined.contains("游戏") || combined.contains("健身") || combined.contains("运动") ||
            combined.contains("瑜伽") || combined.contains("switch") || combined.contains("arcade")
        ) {
            return AppCategory.GAME_FITNESS
        }
        if (combined.contains("cast") || combined.contains("miracast") || combined.contains("airplay") ||
            combined.contains("dlna") || combined.contains("投屏") || combined.contains("settings") ||
            combined.contains("设置") || combined.contains("launcher") || combined.contains("桌面") ||
            combined.contains("tvpulse")
        ) {
            return AppCategory.SYSTEM_CAST
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_VIDEO) {
            return AppCategory.VIDEO
        }
        if (combined.contains("video") || combined.contains("tv") || combined.contains("movie") ||
            combined.contains("iqiyi") || combined.contains("奇异果") || combined.contains("爱奇艺") ||
            combined.contains("tencent") || combined.contains("极光") || combined.contains("腾讯") ||
            combined.contains("youku") || combined.contains("酷喵") || combined.contains("优酷") ||
            combined.contains("mgtv") || combined.contains("芒果") || combined.contains("kuaishou") ||
            combined.contains("快手") || combined.contains("douyin") || combined.contains("抖音") ||
            combined.contains("youtube") || combined.contains("netflix") || combined.contains("kodi") ||
            combined.contains("影视") || combined.contains("电视") || combined.contains("直播")
        ) {
            return AppCategory.VIDEO
        }
        return if (isSystem) AppCategory.SYSTEM_CAST else AppCategory.VIDEO
    }

    private val homeLauncherPackages: Set<String> by lazy {
        val set = mutableSetOf<String>()
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(homeIntent, 0).forEach { ri ->
                ri.activityInfo?.packageName?.let { set.add(it) }
            }
        }
        set
    }

    private fun shouldIgnoreLauncherOrSelf(packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (homeLauncherPackages.contains(packageName)) return true
        val lower = packageName.lowercase(Locale.ROOT)
        return lower == "android" ||
            lower == "com.android.systemui" ||
            lower == "com.android.settings" ||
            lower.contains("tv.settings") ||
            lower.contains("tvpulse") ||
            lower.contains("inputmethod") ||
            lower.contains("imeservice") ||
            lower.contains("permissioncontroller") ||
            lower.contains("packageinstaller") ||
            lower.contains("launcher") ||
            lower.contains("homescreen") ||
            lower.contains("leanbacklauncher") ||
            lower.contains("backdrop") ||
            lower.contains("dreams") ||
            lower.contains("livingfit") ||
            lower.contains("livingdecor") ||
            lower.contains("screensaver")
    }

    private fun queryForegroundSessions(startMs: Long, endMs: Long): Pair<List<ForegroundSession>, Map<String, Int>> {
        val usm = usageStatsManager ?: return emptyList<ForegroundSession>() to emptyMap()
        val sessions = mutableListOf<ForegroundSession>()
        val launchCounts = mutableMapOf<String, Int>()
        val activeStartByPkg = mutableMapOf<String, Long>()

        try {
            val events = usm.queryEvents(startMs, endMs)
            val event = UsageEvents.Event()
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (shouldIgnoreLauncherOrSelf(pkg)) continue
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (!activeStartByPkg.containsKey(pkg)) {
                            activeStartByPkg[pkg] = event.timeStamp.coerceAtLeast(startMs)
                            launchCounts[pkg] = (launchCounts[pkg] ?: 0) + 1
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val resumedAt = activeStartByPkg.remove(pkg)
                        if (resumedAt != null && event.timeStamp > resumedAt) {
                            val clippedEnd = event.timeStamp.coerceAtMost(endMs)
                            if (clippedEnd - resumedAt >= 1_000L) {
                                sessions.add(ForegroundSession(pkg, resumedAt, clippedEnd))
                            }
                        }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.DEVICE_SHUTDOWN -> {
                        val closeTime = event.timeStamp.coerceAtMost(endMs)
                        activeStartByPkg.forEach { (activePkg, resumedAt) ->
                            if (closeTime > resumedAt && closeTime - resumedAt >= 1_000L) {
                                sessions.add(ForegroundSession(activePkg, resumedAt, closeTime))
                            }
                        }
                        activeStartByPkg.clear()
                    }
                }
            }
            // Close any session still active at endMs
            activeStartByPkg.forEach { (pkg, resumedAt) ->
                if (endMs > resumedAt && endMs - resumedAt >= 1_000L) {
                    sessions.add(ForegroundSession(pkg, resumedAt, endMs))
                }
            }
        } catch (_: Exception) {
        }
        return sessions to launchCounts
    }

    fun collectTelemetry(
        rangeFilter: AppRangeFilter,
        categoryFilter: AppCategory,
        weekOffset: Int,
        lanReportUrl: String?
    ): SystemTelemetrySnapshot {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val hasUsagePerm = hasUsageStatsPermission()
        val hasOverlayPerm = hasOverlayPermission()
        val (netLabel, isOnline) = detectNetworkStatus()
        val (camText, hasCam) = detectCameraHardware()
        val tvInputs = detectTvInputSources()
        val installedMap = loadInstalledAppsMap()

        // Start of today (00:00:00)
        val todayStartCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMs = todayStartCal.timeInMillis

        // Query today's sessions
        val (todaySessions, todayLaunches) = queryForegroundSessions(todayStartMs, now)

        // Also aggregate UsageStats for today, 7-day, 30-day
        val rangeStartMs = when (rangeFilter) {
            AppRangeFilter.TODAY -> todayStartMs
            AppRangeFilter.SEVEN_DAYS -> todayStartMs - 6 * 86_400_000L
            AppRangeFilter.MONTH -> todayStartMs - 29 * 86_400_000L
        }
        val prevRangeDurationMs = (now - rangeStartMs).coerceAtLeast(86_400_000L)
        val prevRangeStartMs = rangeStartMs - prevRangeDurationMs

        val todayDurationByPkg = mutableMapOf<String, Long>()
        val lastUsedByPkg = mutableMapOf<String, Long>()
        todaySessions.forEach { s ->
            todayDurationByPkg[s.packageName] = (todayDurationByPkg[s.packageName] ?: 0L) + s.durationMs
            lastUsedByPkg[s.packageName] = maxOf(lastUsedByPkg[s.packageName] ?: 0L, s.endMs)
        }

        // Supplement with UsageStatsManager.queryUsageStats in case events were truncated
        runCatching {
            val stats = usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, todayStartMs, now)
            stats?.forEach { st ->
                val pkg = st.packageName ?: return@forEach
                if (shouldIgnoreLauncherOrSelf(pkg)) return@forEach
                if (st.lastTimeUsed >= todayStartMs && st.totalTimeInForeground > 0) {
                    val cur = todayDurationByPkg[pkg] ?: 0L
                    if (st.totalTimeInForeground > cur) {
                        todayDurationByPkg[pkg] = st.totalTimeInForeground
                    }
                    lastUsedByPkg[pkg] = maxOf(lastUsedByPkg[pkg] ?: 0L, st.lastTimeUsed)
                }
            }
        }

        // Compute 7-day daily durations per package (from today-6d to today)
        val sevenDaysDailyByPkg = mutableMapOf<String, FloatArray>() // index 0 = 6 days ago, index 6 = today
        for (dayIdx in 0..6) {
            val dStart = todayStartMs - (6 - dayIdx) * 86_400_000L
            val dEnd = if (dayIdx == 6) now else dStart + 86_400_000L
            val (daySessions, _) = queryForegroundSessions(dStart, dEnd)
            val dayMap = mutableMapOf<String, Long>()
            daySessions.forEach { s ->
                dayMap[s.packageName] = (dayMap[s.packageName] ?: 0L) + s.durationMs
            }
            if (dayMap.isEmpty()) {
                runCatching {
                    val stats = usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dStart, dEnd)
                    stats?.forEach { st ->
                        if (st.lastTimeUsed in dStart..dEnd && !shouldIgnoreLauncherOrSelf(st.packageName)) {
                            dayMap[st.packageName] = maxOf(dayMap[st.packageName] ?: 0L, st.totalTimeInForeground)
                        }
                    }
                }
            }
            dayMap.forEach { (pkg, ms) ->
                val arr = sevenDaysDailyByPkg.getOrPut(pkg) { FloatArray(7) }
                arr[dayIdx] = (ms / 3_600_000f)
            }
        }

        // Selected range total durations per package
        val rangeDurationByPkg = mutableMapOf<String, Long>()
        if (rangeFilter == AppRangeFilter.TODAY) {
            rangeDurationByPkg.putAll(todayDurationByPkg)
        } else {
            runCatching {
                val agg = usageStatsManager?.queryAndAggregateUsageStats(rangeStartMs, now)
                agg?.forEach { (pkg, st) ->
                    if (!shouldIgnoreLauncherOrSelf(pkg) && st.totalTimeInForeground > 0) {
                        rangeDurationByPkg[pkg] = st.totalTimeInForeground
                        lastUsedByPkg[pkg] = maxOf(lastUsedByPkg[pkg] ?: 0L, st.lastTimeUsed)
                    }
                }
            }
            todayDurationByPkg.forEach { (pkg, ms) ->
                rangeDurationByPkg[pkg] = maxOf(rangeDurationByPkg[pkg] ?: 0L, ms)
            }
        }

        // Previous range durations for trend percentage
        val prevRangeDurationByPkg = mutableMapOf<String, Long>()
        runCatching {
            val aggPrev = usageStatsManager?.queryAndAggregateUsageStats(prevRangeStartMs, rangeStartMs)
            aggPrev?.forEach { (pkg, st) ->
                if (!shouldIgnoreLauncherOrSelf(pkg) && st.totalTimeInForeground > 0) {
                    prevRangeDurationByPkg[pkg] = st.totalTimeInForeground
                }
            }
        }

        // Determine currently running / most recently active foreground package
        val mostRecentSessionPkg = todaySessions.maxByOrNull { it.endMs }?.packageName
            ?: lastUsedByPkg.maxByOrNull { it.value }?.key
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val isAudioVideoPlaying = audioManager?.isMusicActive == true

        // Hourly breakdown today (24 hours: 0..23) per package and per category
        val hourlyByPkg = mutableMapOf<String, IntArray>()
        val hourlyByCat = Array(24) { mutableMapOf<AppCategory, Long>() }
        todaySessions.forEach { session ->
            val meta = getOrResolveAppMeta(session.packageName)
            val cat = meta?.category ?: AppCategory.VIDEO
            val pkgHours = hourlyByPkg.getOrPut(session.packageName) { IntArray(24) }
            var cursor = session.startMs
            while (cursor < session.endMs) {
                val cCal = Calendar.getInstance().apply { timeInMillis = cursor }
                val hr = cCal.get(Calendar.HOUR_OF_DAY)
                cCal.set(Calendar.MINUTE, 59)
                cCal.set(Calendar.SECOND, 59)
                cCal.set(Calendar.MILLISECOND, 999)
                val bucketEnd = minOf(session.endMs, cCal.timeInMillis + 1)
                val sliceMs = (bucketEnd - cursor).coerceAtLeast(0L)
                pkgHours[hr] += (sliceMs / 60_000L).toInt()
                hourlyByCat[hr][cat] = (hourlyByCat[hr][cat] ?: 0L) + sliceMs
                cursor = bucketEnd.coerceAtLeast(cursor + 1_000L)
            }
        }

        // Build 12 two-hour slots for Dashboard 24h chart
        val slotHours = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 21, 22, 24)
        val hourlySlots = buildList {
            val pairs = listOf(
                0 to 2, 2 to 4, 4 to 6, 6 to 8, 8 to 10, 10 to 12,
                12 to 14, 14 to 16, 16 to 18, 18 to 20, 20 to 22, 22 to 24
            )
            pairs.forEach { (startHr, endHr) ->
                val isCurrent = currentHour in startHr until endHr
                val isFuture = startHr > currentHour
                var totalMs = 0L
                val catTotals = mutableMapOf<AppCategory, Long>()
                for (h in startHr until endHr) {
                    hourlyByCat[h].forEach { (cat, ms) ->
                        totalMs += ms
                        catTotals[cat] = (catTotals[cat] ?: 0L) + ms
                    }
                }
                val dominantCat = catTotals.maxByOrNull { it.value }?.key ?: AppCategory.VIDEO
                val label = if (isCurrent) "现在" else String.format(Locale.US, "%02d", endHr)
                add(
                    HourlySlotStat(
                        label = label,
                        startHour = startHr,
                        endHour = endHr,
                        totalMinutes = (totalMs / 60_000L).toInt().coerceAtMost(120),
                        dominantCategory = dominantCat,
                        isCurrentSlot = isCurrent,
                        isFutureSlot = isFuture
                    )
                )
            }
        }

        // Compute longest continuous viewing session today (merging gaps < 3 min)
        val sortedSessions = todaySessions.sortedBy { it.startMs }
        var longestContinuousMs = 0L
        if (sortedSessions.isNotEmpty()) {
            var curChainStart = sortedSessions.first().startMs
            var curChainEnd = sortedSessions.first().endMs
            for (i in 1 until sortedSessions.size) {
                val s = sortedSessions[i]
                if (s.startMs - curChainEnd <= 3 * 60_000L) {
                    curChainEnd = maxOf(curChainEnd, s.endMs)
                } else {
                    longestContinuousMs = maxOf(longestContinuousMs, curChainEnd - curChainStart)
                    curChainStart = s.startMs
                    curChainEnd = s.endMs
                }
            }
            longestContinuousMs = maxOf(longestContinuousMs, curChainEnd - curChainStart)
        }
        val longestContinuousMinutes = (longestContinuousMs / 60_000L).toInt()

        // Total minutes today
        val todayTotalMs = todayDurationByPkg.values.sum()
        val todayTotalMinutes = (todayTotalMs / 60_000L).toInt()

        // Build AppUsageInfo list across all installed + used packages
        val allKnownPackages = (installedMap.keys + rangeDurationByPkg.keys + todayDurationByPkg.keys)
            .filterNot { shouldIgnoreLauncherOrSelf(it) }
            .toSet()

        val appLimitsMap = policyStore.getAppDailyLimits()
        val curfewAppsSet = policyStore.getCurfewApps()
        val totalRangeMs = rangeDurationByPkg.values.sum().coerceAtLeast(1L)

        val allAppsUsage = allKnownPackages.mapNotNull { pkg ->
            val meta = installedMap[pkg] ?: getOrResolveAppMeta(pkg) ?: return@mapNotNull null
            val rangeMs = rangeDurationByPkg[pkg] ?: 0L
            val todayMs = todayDurationByPkg[pkg] ?: 0L
            val prevMs = prevRangeDurationByPkg[pkg] ?: 0L
            val trendPct = if (prevMs > 60_000L) {
                (((rangeMs - prevMs) * 100f) / prevMs).roundToInt().coerceIn(-99, 999)
            } else if (rangeMs > 0L) {
                100
            } else {
                0
            }
            val sparkline = sevenDaysDailyByPkg[pkg]?.toList() ?: List(7) { 0f }
            val hrArr = hourlyByPkg[pkg]?.toList() ?: List(24) { 0 }

            val primeMin = (19..21).sumOf { hrArr[it] }
            val lateMin = (22..23).sumOf { hrArr[it] } + (0..5).sumOf { hrArr[it] }
            val dayMin = (6..18).sumOf { hrArr[it] }
            val rawTotalMin = primeMin + lateMin + dayMin
            val sumMin = rawTotalMin.coerceAtLeast(1)
            val primePct = if (rawTotalMin > 0) ((primeMin * 100f) / sumMin).roundToInt() else 0
            val dayPct = if (rawTotalMin > 0) ((dayMin * 100f) / sumMin).roundToInt() else 0
            val latePct = if (rawTotalMin > 0) (100 - primePct - dayPct).coerceAtLeast(0) else 0

            AppUsageInfo(
                packageName = pkg,
                appName = meta.appName,
                category = meta.category,
                totalTimeMs = rangeMs,
                todayTimeMs = todayMs,
                lastTimeUsedMs = lastUsedByPkg[pkg] ?: 0L,
                launchCount = todayLaunches[pkg] ?: if (todayMs > 0) 1 else 0,
                percentageOfTotal = if (rangeDurationByPkg.values.sum() > 0L) {
                    ((rangeMs * 100f) / totalRangeMs).roundToInt().coerceIn(0, 100)
                } else 0,
                isCurrentlyRunning = pkg == mostRecentSessionPkg && todayMs > 0L,
                dailyLimitMinutes = appLimitsMap[pkg] ?: 0,
                inNightCurfew = curfewAppsSet.contains(pkg),
                trendPercent = trendPct,
                dailySparklineHours = sparkline,
                hourlyMinutesToday = hrArr,
                primeEveningPercent = primePct,
                daytimePercent = dayPct,
                lateNightPercent = latePct,
                activeMediaTitle = if (pkg == mostRecentSessionPkg && isAudioVideoPlaying) "系统检测到正在播放音视频流" else null,
                iconDrawable = meta.icon
            )
        }.sortedWith(
            compareByDescending<AppUsageInfo> { it.totalTimeMs }
                .thenByDescending { it.todayTimeMs }
                .thenBy { it.appName }
        )

        val filteredRankedApps = if (categoryFilter == AppCategory.ALL) {
            allAppsUsage
        } else {
            allAppsUsage.filter { it.category == categoryFilter }
        }

        // Top 4 cards for Dashboard
        val topRunningAndFrequent = allAppsUsage
            .sortedWith(
                compareByDescending<AppUsageInfo> { it.isCurrentlyRunning }
                    .thenByDescending { it.todayTimeMs }
                    .thenByDescending { it.totalTimeMs }
            )
            .take(4)

        // Noon & Evening summaries from real hourly buckets
        val noonMinutes = ((12..13).sumOf { hr -> hourlyByCat[hr].values.sum() } / 60_000L).toInt()
        val eveningMinutes = ((19..21).sumOf { hr -> hourlyByCat[hr].values.sum() } / 60_000L).toInt()
        val noonSummary = "午间 (12-14点): ${formatMinutesShort(noonMinutes)}"
        val eveningSummary = "黄金档 (19-22点): ${formatMinutesShort(eveningMinutes)}"

        // Quota & Health Score calculation
        val dailyQuotaMin = policyStore.getDailyFamilyQuotaMinutes()
        val quotaRatio = (todayTotalMinutes.toFloat() / dailyQuotaMin.coerceAtLeast(1)).coerceAtLeast(0f)
        val lateNightMinToday = ((23..23).sumOf { hourlyByCat[it].values.sum() } +
            (0..4).sumOf { hourlyByCat[it].values.sum() }) / 60_000L
        var score = 100
        if (quotaRatio > 0.5f) score -= ((quotaRatio - 0.5f) * 30f).roundToInt()
        if (longestContinuousMinutes > 60) score -= ((longestContinuousMinutes - 60) / 5).coerceAtMost(20)
        if (lateNightMinToday > 0) score -= (lateNightMinToday.toInt() / 5).coerceAtMost(20)
        score = score.coerceIn(35, 100)

        val (ratingLabel, subLabel) = when {
            score >= 90 -> "优秀评级" to "作息规律健康"
            score >= 75 -> "良好评级" to if (longestContinuousMinutes <= 100) "未超长连击" else "连续观看偏长"
            score >= 60 -> "轻度疲劳" to "建议适当休息"
            else -> "超负荷预警" to "用眼已超标"
        }

        // Night light countdown
        val (autoHr, autoMin) = policyStore.getBlueLightAutoSchedule()
        val isBlueLightActive = policyStore.isBlueLightFilterActive()
        val nowMinutesOfDay = currentHour * 60 + currentMinute
        val targetMinutesOfDay = autoHr * 60 + autoMin
        val diffMin = if (targetMinutesOfDay >= nowMinutesOfDay) {
            targetMinutesOfDay - nowMinutesOfDay
        } else {
            (24 * 60 - nowMinutesOfDay) + targetMinutesOfDay
        }
        val nightLightCountdown = when {
            isBlueLightActive -> "护眼开启中"
            diffMin <= 60 -> "${diffMin}m 后开启"
            else -> "${diffMin / 60}h ${diffMin % 60}m 后"
        }

        // Active profile & blink reminder count
        val profiles = policyStore.getProfiles()
        val activeProfileId = policyStore.getActiveProfileId()
        val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.first()
        val blinkCount = policyStore.getTodayBlinkReminderCount(
            longestContinuousMinutes = longestContinuousMinutes,
            intervalMinutes = activeProfile.eyeRestIntervalMinutes
        )

        // Build Weekly Report Data (for current week or previous week)
        val weeklyReport = buildWeeklyReport(
            weekOffset = weekOffset,
            dailyQuotaMinutes = dailyQuotaMin,
            lanReportUrl = lanReportUrl
        )

        // Build Family Member Summaries using real usage attributed by category & active profile
        val memberSummaries = buildMemberSummaries(
            profiles = profiles,
            activeProfileId = activeProfileId,
            allAppsUsage = allAppsUsage,
            todayTotalMinutes = todayTotalMinutes,
            weeklyReport = weeklyReport,
            lateNightMinutesToday = lateNightMinToday.toInt()
        )

        val greetingPrefix = when (currentHour) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..17 -> "下午好"
            else -> "晚上好"
        }
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US).format(Date(now))
        val deviceTag = "Android TV 16:9 · ${Build.MODEL.take(14)}"

        return SystemTelemetrySnapshot(
            hasUsagePermission = hasUsagePerm,
            hasOverlayPermission = hasOverlayPerm,
            networkLabel = netLabel,
            isNetworkOnline = isOnline,
            currentTimeText = timeFmt,
            greetingTitle = "$greetingPrefix，${activeProfile.name}",
            deviceModelTag = deviceTag,
            cameraStatusText = camText,
            hasCameraHardware = hasCam,
            tvInputSources = tvInputs,
            monitoredAppsCount = allAppsUsage.size,
            todayTotalMinutes = todayTotalMinutes,
            dailyFamilyQuotaMinutes = dailyQuotaMin,
            longestContinuousMinutes = longestContinuousMinutes,
            blinkReminderCountToday = blinkCount,
            healthScore = score,
            healthRatingLabel = ratingLabel,
            healthSubLabel = subLabel,
            noonPeakSummary = noonSummary,
            eveningPrimeSummary = eveningSummary,
            nightLightAutoHour = autoHr,
            nightLightAutoMinute = autoMin,
            nightLightCountdownText = nightLightCountdown,
            isBlueLightFilterActive = isBlueLightActive,
            topRunningAndFrequentApps = topRunningAndFrequent,
            rankedApps = filteredRankedApps,
            hourlySlots = hourlySlots,
            weeklyReport = weeklyReport,
            memberSummaries = memberSummaries,
            activeProfileId = activeProfile.id,
            categoryPolicies = policyStore.getCategoryPolicies()
        )
    }

    private fun buildWeeklyReport(
        weekOffset: Int,
        dailyQuotaMinutes: Int,
        lanReportUrl: String?
    ): WeeklyReportData {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dow = get(Calendar.DAY_OF_WEEK)
            val diffToMonday = if (dow == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dow)
            add(Calendar.DAY_OF_YEAR, diffToMonday + (weekOffset * 7))
        }
        val mondayStartMs = cal.timeInMillis
        val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
        val dateFmt = SimpleDateFormat("yyyy 年 MM 月 dd 日", Locale.CHINA)
        val shortDateFmt = SimpleDateFormat("MM 月 dd 日", Locale.CHINA)
        val dayShortFmt = SimpleDateFormat("MM-dd", Locale.US)

        val sundayEndMs = mondayStartMs + 7 * 86_400_000L
        val dateRangeText = "第 $weekNumber 周 : ${dateFmt.format(Date(mondayStartMs))} — ${
            shortDateFmt.format(Date(sundayEndMs - 86_400_000L))
        }"

        val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val dailyBars = mutableListOf<DayStackedUsage>()

        var primeEveningMs = 0L
        var noonBreakMs = 0L
        var lateNightMs = 0L
        var otherDaytimeMs = 0L
        var totalWeekMs = 0L

        for (dayIdx in 0..6) {
            val dStart = mondayStartMs + dayIdx * 86_400_000L
            val dEnd = (dStart + 86_400_000L).coerceAtMost(now)
            val isToday = now in dStart until (dStart + 86_400_000L)

            var videoMs = 0L
            var kidsMs = 0L
            var gameMs = 0L
            var eduMs = 0L

            if (dStart < now) {
                val (sessions, _) = queryForegroundSessions(dStart, dEnd)
                if (sessions.isNotEmpty()) {
                    sessions.forEach { s ->
                        val cat = getOrResolveAppMeta(s.packageName)?.category ?: AppCategory.VIDEO
                        when (cat) {
                            AppCategory.VIDEO -> videoMs += s.durationMs
                            AppCategory.KIDS -> kidsMs += s.durationMs
                            AppCategory.GAME_FITNESS -> gameMs += s.durationMs
                            AppCategory.EDUCATION, AppCategory.SYSTEM_CAST, AppCategory.ALL -> eduMs += s.durationMs
                        }
                        val midHour = Calendar.getInstance().apply {
                            timeInMillis = (s.startMs + s.endMs) / 2
                        }.get(Calendar.HOUR_OF_DAY)
                        when (midHour) {
                            in 19..21 -> primeEveningMs += s.durationMs
                            in 12..13 -> noonBreakMs += s.durationMs
                            23, in 0..5 -> lateNightMs += s.durationMs
                            else -> otherDaytimeMs += s.durationMs
                        }
                    }
                } else {
                    runCatching {
                        val stats = usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dStart, dEnd)
                        stats?.forEach { st ->
                            if (st.lastTimeUsed in dStart..dEnd && !shouldIgnoreLauncherOrSelf(st.packageName)) {
                                val cat = getOrResolveAppMeta(st.packageName)?.category ?: AppCategory.VIDEO
                                when (cat) {
                                    AppCategory.VIDEO -> videoMs += st.totalTimeInForeground
                                    AppCategory.KIDS -> kidsMs += st.totalTimeInForeground
                                    AppCategory.GAME_FITNESS -> gameMs += st.totalTimeInForeground
                                    else -> eduMs += st.totalTimeInForeground
                                }
                                primeEveningMs += (st.totalTimeInForeground * 0.6f).toLong()
                                noonBreakMs += (st.totalTimeInForeground * 0.2f).toLong()
                                otherDaytimeMs += (st.totalTimeInForeground * 0.2f).toLong()
                            }
                        }
                    }
                }
            }

            val daySumMs = videoMs + kidsMs + gameMs + eduMs
            totalWeekMs += daySumMs
            dailyBars.add(
                DayStackedUsage(
                    dayOfWeekLabel = dayLabels[dayIdx],
                    dateLabel = dayShortFmt.format(Date(dStart)),
                    videoHours = videoMs / 3_600_000f,
                    kidsHours = kidsMs / 3_600_000f,
                    gameHours = gameMs / 3_600_000f,
                    eduSystemHours = eduMs / 3_600_000f,
                    isToday = isToday
                )
            )
        }

        // Previous week total for week-over-week comparison
        val prevWeekStartMs = mondayStartMs - 7 * 86_400_000L
        var prevWeekTotalMs = 0L
        runCatching {
            val aggPrev = usageStatsManager?.queryAndAggregateUsageStats(prevWeekStartMs, mondayStartMs)
            aggPrev?.forEach { (pkg, st) ->
                if (!shouldIgnoreLauncherOrSelf(pkg)) {
                    prevWeekTotalMs += st.totalTimeInForeground
                }
            }
        }
        val wowPercent = if (prevWeekTotalMs > 60_000L) {
            (((totalWeekMs - prevWeekTotalMs) * 100f) / prevWeekTotalMs).roundToInt().coerceIn(-99, 999)
        } else {
            0
        }

        val quotaHours = dailyQuotaMinutes / 60f
        val compliantDays = dailyBars.count { it.totalHours <= quotaHours }
        val weekdayHours = dailyBars.take(5).map { it.totalHours }
        val weekdayAvg = if (weekdayHours.isNotEmpty()) weekdayHours.average().toFloat() else 0f

        val nonZeroDays = dailyBars.mapIndexed { idx, d -> idx to d.totalHours }.filter { it.second > 0f }
        val peakIdx = dailyBars.indices.maxByOrNull { dailyBars[it].totalHours } ?: 5
        val lowestIdx = nonZeroDays.minByOrNull { it.second }?.first ?: 2

        val timeDist = TimeOfDayDistribution(
            primeEveningMinutes = (primeEveningMs / 60_000L).toInt(),
            noonBreakMinutes = (noonBreakMs / 60_000L).toInt(),
            otherDaytimeMinutes = (otherDaytimeMs / 60_000L).toInt(),
            lateNightMinutes = (lateNightMs / 60_000L).toInt()
        )

        val totalWeeklyMin = (totalWeekMs / 60_000L).toInt()
        val eyeCareCompletion = ((compliantDays * 100f) / 7f).roundToInt().coerceIn(60, 100)
        val familyLevel = when {
            compliantDays >= 6 -> 5
            compliantDays >= 5 -> 4
            compliantDays >= 3 -> 3
            else -> 2
        }

        val aiComment = buildString {
            val wkAvgStr = String.format(Locale.US, "%.1f", weekdayAvg)
            if (weekdayAvg <= 3.5f && timeDist.lateNightMinutes <= 60) {
                append("本周工作日日均观看控制在 ${wkAvgStr} 小时，达标 ${compliantDays}/7 天；")
                if (timeDist.lateNightMinutes == 0) {
                    append("全周无深夜 23:00 后超时观影，家庭护眼习惯非常优秀！")
                } else {
                    append("深夜 23:00 后累计仅 ${timeDist.lateNightMinutes} 分钟，整体作息健康平稳。")
                }
            } else {
                append("本周峰值出现在${dayLabels[peakIdx]}，工作日均值 ${wkAvgStr} 小时；建议开启 21:30 自动暖色蓝光滤镜与就寝锁屏提醒。")
            }
        }

        val qrText = lanReportUrl ?: buildString {
            append("【TV Pulse 健康周报】\n")
            append("$dateRangeText\n")
            append("全周总时长: ${totalWeeklyMin / 60}小时${totalWeeklyMin % 60}分\n")
            append("黄金档占比: ${timeDist.primePercent}% | 达标日: $compliantDays/7\n")
            append("AI管家: $aiComment")
        }

        return WeeklyReportData(
            weekOffset = weekOffset,
            weekNumber = weekNumber,
            dateRangeText = dateRangeText,
            totalWeeklyMinutes = totalWeeklyMin,
            weekOverWeekChangePercent = wowPercent,
            dailyBars = dailyBars,
            compliantDaysCount = compliantDays,
            weekdayAvgHours = weekdayAvg,
            peakDayIndex = peakIdx,
            lowestDayIndex = lowestIdx,
            timeDistribution = timeDist,
            eyeCareCompletionPercent = eyeCareCompletion,
            familyLevel = familyLevel,
            aiAdvisorComment = aiComment,
            blueLightReducedHours = policyStore.getBlueLightAccumulatedMinutes() / 60f,
            qrReportContent = qrText,
            qrLocalUrl = lanReportUrl
        )
    }

    private fun buildMemberSummaries(
        profiles: List<FamilyMemberProfile>,
        activeProfileId: String,
        allAppsUsage: List<AppUsageInfo>,
        todayTotalMinutes: Int,
        weeklyReport: WeeklyReportData,
        lateNightMinutesToday: Int
    ): List<MemberUsageSummary> {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)

        val todayByCat = allAppsUsage.groupBy { it.category }
            .mapValues { (_, list) -> (list.sumOf { it.todayTimeMs } / 60_000L).toInt() }
        val weekByCat = allAppsUsage.groupBy { it.category }
            .mapValues { (_, list) -> (list.sumOf { it.totalTimeMs } / 60_000L).toInt() }

        return profiles.map { profile ->
            val todayQuota = if (!profile.isRestricted) 0 else if (isWeekend) {
                profile.weekendLimitMinutes
            } else {
                profile.weekdayLimitMinutes
            }
            val weeklyQuota = if (!profile.isRestricted) 0 else {
                profile.weekdayLimitMinutes * 5 + profile.weekendLimitMinutes * 2
            }

            // Real usage only: active profile (or single default owner profile) reflects real system total
            val isOwnerOrActive = profile.id == activeProfileId || profiles.size == 1
            val catTodayMin = profile.preferredCategories.sumOf { todayByCat[it] ?: 0 }
            val todayUsed = if (isOwnerOrActive) todayTotalMinutes else catTodayMin

            val catWeekMin = profile.preferredCategories.sumOf { weekByCat[it] ?: 0 }
            val weeklyUsed = if (isOwnerOrActive) weeklyReport.totalWeeklyMinutes else catWeekMin

            val matchingApps = allAppsUsage
                .filter { it.totalTimeMs > 0 && (isOwnerOrActive || profile.preferredCategories.contains(it.category)) }
                .take(2)
                .map { it.appName }
                .ifEmpty { listOf("暂无记录") }

            val timeWindow = "全天实时统计"
            val wow = weeklyReport.weekOverWeekChangePercent

            MemberUsageSummary(
                profile = profile,
                todayUsedMinutes = todayUsed,
                todayQuotaMinutes = todayQuota,
                weeklyUsedMinutes = weeklyUsed,
                weeklyQuotaMinutes = weeklyQuota,
                topAppNames = matchingApps,
                primaryTimeWindow = timeWindow,
                weekOverWeekPercent = wow,
                hasCurfewViolation = profile.curfewEnabled && lateNightMinutesToday > 5
            )
        }
    }

    companion object {
        fun formatMinutesShort(totalMinutes: Int): String {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }

        fun formatMinutesChinese(totalMinutes: Int): String {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (h > 0) "${h}小时 ${m}分" else "${m}分钟"
        }
    }
}

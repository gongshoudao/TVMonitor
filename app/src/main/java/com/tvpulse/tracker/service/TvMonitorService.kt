package com.tvpulse.tracker.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.tvpulse.tracker.MainActivity
import com.tvpulse.tracker.data.FamilyPolicyStore
import java.util.Calendar

class TvMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var policyStore: FamilyPolicyStore
    private var windowManager: WindowManager? = null
    private var blueLightOverlayView: View? = null
    private var lastBlinkNotifyTimeMs: Long = 0L
    private var continuousSessionStartMs: Long = System.currentTimeMillis()
    private var unlockedGraceUntilMs: Long = 0L

    private val monitorRunnable = object : Runnable {
        override fun run() {
            try {
                syncSystemBlueLightOverlay()
                checkForegroundUsageAndEnforce()
            } catch (_: Exception) {
            } finally {
                handler.postDelayed(this, 10_000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        policyStore = FamilyPolicyStore(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        continuousSessionStartMs = System.currentTimeMillis()
        handler.post(monitorRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_GRANT_GRACE_PERIOD) {
            // Grant 15 minutes of grace after parent unlocks via PIN
            unlockedGraceUntilMs = System.currentTimeMillis() + 15 * 60_000L
        }
        syncSystemBlueLightOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        removeSystemBlueLightOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun syncSystemBlueLightOverlay() {
        val shouldShow = policyStore.isBlueLightFilterActive() && Settings.canDrawOverlays(this)
        if (shouldShow && blueLightOverlayView == null) {
            try {
                val view = View(this).apply {
                    setBackgroundColor(0x28FF9500) // Warm amber eye-care filter
                }
                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                }
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                windowManager?.addView(view, params)
                blueLightOverlayView = view
            } catch (_: Exception) {
            }
        } else if (!shouldShow && blueLightOverlayView != null) {
            removeSystemBlueLightOverlay()
        }
    }

    private fun removeSystemBlueLightOverlay() {
        blueLightOverlayView?.let { v ->
            runCatching { windowManager?.removeView(v) }
        }
        blueLightOverlayView = null
    }

    private fun checkForegroundUsageAndEnforce() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // Auto-enable scheduled Night Blue-Light Filter at configured time
        val (autoHr, autoMin) = policyStore.getBlueLightAutoSchedule()
        if (hour == autoHr && minute == autoMin && !policyStore.isBlueLightFilterActive()) {
            policyStore.setBlueLightFilterActive(true)
            syncSystemBlueLightOverlay()
        }

        // Check continuous viewing blink reminder interval
        val profiles = policyStore.getProfiles()
        val activeProfile = profiles.find { it.id == policyStore.getActiveProfileId() } ?: profiles.firstOrNull()
        val intervalMs = ((activeProfile?.eyeRestIntervalMinutes ?: 45).coerceAtLeast(15)) * 60_000L
        if (now - continuousSessionStartMs >= intervalMs && now - lastBlinkNotifyTimeMs >= intervalMs) {
            lastBlinkNotifyTimeMs = now
            policyStore.incrementBlinkReminderToday()
            Toast.makeText(
                applicationContext,
                "【TV Pulse 护眼提醒】您已连续观看 ${activeProfile?.eyeRestIntervalMinutes ?: 45} 分钟，请眨眼或远眺休息！",
                Toast.LENGTH_LONG
            ).show()
        }

        // Skip lock enforcement if inside parental unlock grace window
        if (now < unlockedGraceUntilMs) return

        val currentFgPkg = detectCurrentForegroundPackage(now)
        if (currentFgPkg == null || currentFgPkg == packageName) return

        // 1. Check Bedtime Curfew for active profile
        if (activeProfile != null && activeProfile.curfewEnabled) {
            val inCurfew = if (activeProfile.curfewStartHour > activeProfile.curfewEndHour) {
                hour >= activeProfile.curfewStartHour || hour < activeProfile.curfewEndHour
            } else {
                hour in activeProfile.curfewStartHour until activeProfile.curfewEndHour
            }
            if (inCurfew) {
                launchLockScreen(
                    title = "就寝宵禁时间已到 (${activeProfile.name})",
                    subtitle = "当前处于 ${activeProfile.curfewStartHour}:00 ~ ${activeProfile.curfewEndHour}:00 就寝熄屏时段"
                )
                return
            }
        }

        // 2. Check Per-App Daily Limit
        val appLimitMin = policyStore.getAppDailyLimits()[currentFgPkg] ?: 0
        if (appLimitMin > 0) {
            val usedTodayMin = queryPackageTodayMinutes(currentFgPkg, now)
            if (usedTodayMin >= appLimitMin) {
                launchLockScreen(
                    title = "应用今日时长已达上限",
                    subtitle = "该应用今日已使用 ${usedTodayMin} 分钟（每日限额 ${appLimitMin} 分钟）"
                )
            }
        }
    }

    private fun detectCurrentForegroundPackage(now: Long): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        var lastResumedPkg: String? = null
        try {
            val events = usm.queryEvents(now - 60_000L, now)
            val event = UsageEvents.Event()
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumedPkg = event.packageName
                }
            }
        } catch (_: Exception) {
        }
        return lastResumedPkg
    }

    private fun queryPackageTodayMinutes(pkg: String, now: Long): Int {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startCal.timeInMillis, now)
            val totalMs = stats?.filter { it.packageName == pkg }?.maxOfOrNull { it.totalTimeInForeground } ?: 0L
            (totalMs / 60_000L).toInt()
        } catch (_: Exception) {
            0
        }
    }

    private fun launchLockScreen(title: String, subtitle: String) {
        val lockIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_LOCK_TITLE, title)
            putExtra(EXTRA_LOCK_SUBTITLE, subtitle)
        }
        runCatching { startActivity(lockIntent) }
    }

    companion object {
        const val ACTION_GRANT_GRACE_PERIOD = "com.tvpulse.tracker.ACTION_GRANT_GRACE_PERIOD"
        const val EXTRA_LOCK_TITLE = "extra_lock_title"
        const val EXTRA_LOCK_SUBTITLE = "extra_lock_subtitle"
    }
}

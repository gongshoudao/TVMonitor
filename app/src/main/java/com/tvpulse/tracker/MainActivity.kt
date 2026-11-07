package com.tvpulse.tracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tvpulse.tracker.data.FamilyPolicyStore
import com.tvpulse.tracker.data.TvUsageRepository
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.AppRangeFilter
import com.tvpulse.tracker.model.NavTab
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.service.TvMonitorService
import com.tvpulse.tracker.ui.components.ParentalPinLockScreenOverlay
import com.tvpulse.tracker.ui.components.SilentAmbientScreenOverlay
import com.tvpulse.tracker.ui.components.TvSidebarNavigation
import com.tvpulse.tracker.ui.components.TvTopHeader
import com.tvpulse.tracker.ui.screens.AppsScreen
import com.tvpulse.tracker.ui.screens.DashboardScreen
import com.tvpulse.tracker.ui.screens.FamilyLimitsScreen
import com.tvpulse.tracker.ui.screens.ReportsScreen
import com.tvpulse.tracker.ui.screens.SettingsScreen
import com.tvpulse.tracker.ui.theme.TvColors
import com.tvpulse.tracker.ui.theme.TvPulseTheme
import com.tvpulse.tracker.util.LocalWeeklyReportHttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var policyStore: FamilyPolicyStore
    private lateinit var repository: TvUsageRepository
    private val reportServer = LocalWeeklyReportHttpServer()
    private var incomingLockTitle by mutableStateOf<String?>(null)
    private var incomingLockSubtitle by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        policyStore = FamilyPolicyStore(applicationContext)
        repository = TvUsageRepository(applicationContext, policyStore)
        reportServer.startIfNeeded()
        runCatching { startService(Intent(this, TvMonitorService::class.java)) }
        handleLockIntent(intent)

        setContent {
            TvPulseTheme {
                TvPulseMainApp(
                    policyStore = policyStore,
                    repository = repository,
                    reportServer = reportServer,
                    externalLockTitle = incomingLockTitle,
                    externalLockSubtitle = incomingLockSubtitle,
                    onClearExternalLock = {
                        incomingLockTitle = null
                        incomingLockSubtitle = null
                        val graceIntent = Intent(this, TvMonitorService::class.java).apply {
                            action = TvMonitorService.ACTION_GRANT_GRACE_PERIOD
                        }
                        runCatching { startService(graceIntent) }
                    },
                    onDimWindowBrightness = { dim ->
                        val lp = window.attributes
                        lp.screenBrightness = if (dim) 0.01f else -1f
                        window.attributes = lp
                    },
                    onOpenUsageSettings = { openUsageAccessSettings() },
                    onOpenOverlaySettings = { openOverlaySettings() },
                    onShowToast = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLockIntent(intent)
    }

    private fun handleLockIntent(intent: Intent?) {
        val title = intent?.getStringExtra(TvMonitorService.EXTRA_LOCK_TITLE)
        val subtitle = intent?.getStringExtra(TvMonitorService.EXTRA_LOCK_SUBTITLE)
        if (!title.isNullOrBlank()) {
            incomingLockTitle = title
            incomingLockSubtitle = subtitle ?: "已触发防沉迷或就寝宵禁策略"
        }
    }

    override fun onDestroy() {
        reportServer.stop()
        super.onDestroy()
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
            }
            Toast.makeText(
                this,
                "当前电视ROM隐藏了使用情况设置页，请使用 ADB 授权: adb shell appops set com.tvpulse.tracker GET_USAGE_STATS allow",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun TvPulseMainApp(
    policyStore: FamilyPolicyStore,
    repository: TvUsageRepository,
    reportServer: LocalWeeklyReportHttpServer,
    externalLockTitle: String?,
    externalLockSubtitle: String?,
    onClearExternalLock: () -> Unit,
    onDimWindowBrightness: (Boolean) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var selectedRange by remember { mutableStateOf(AppRangeFilter.TODAY) }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var focusedPackageForAppsScreen by remember { mutableStateOf<String?>(null) }

    var isSilentScreenActive by remember { mutableStateOf(false) }
    var isPinLockActive by remember { mutableStateOf(false) }
    var pinLockReasonTitle by remember { mutableStateOf("防沉迷与就寝锁屏守护") }
    var pinLockReasonSubtitle by remember { mutableStateOf("当前档案已到达设定观看时长或就寝宵禁时间") }

    LaunchedEffect(externalLockTitle, externalLockSubtitle) {
        if (!externalLockTitle.isNullOrBlank()) {
            pinLockReasonTitle = externalLockTitle
            pinLockReasonSubtitle = externalLockSubtitle ?: "当前档案已到达设定观看时长或就寝宵禁时间"
            isPinLockActive = true
        }
    }

    LaunchedEffect(isSilentScreenActive, isPinLockActive) {
        onDimWindowBrightness(isSilentScreenActive || isPinLockActive)
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var snapshot by remember {
        mutableStateOf(
            repository.collectTelemetry(
                rangeFilter = selectedRange,
                categoryFilter = selectedCategory,
                weekOffset = weekOffset,
                lanReportUrl = reportServer.getLanReportUrl()
            )
        )
    }

    suspend fun reloadSnapshot() {
        val latest = withContext(Dispatchers.IO) {
            repository.collectTelemetry(
                rangeFilter = selectedRange,
                categoryFilter = selectedCategory,
                weekOffset = weekOffset,
                lanReportUrl = reportServer.getLanReportUrl()
            )
        }
        reportServer.updateReportContent(latest.weeklyReport, latest.memberSummaries)
        snapshot = latest
    }

    LaunchedEffect(selectedRange, selectedCategory, weekOffset, refreshTrigger) {
        reloadSnapshot()
    }

    // Periodic 15-second live telemetry refresh
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            reloadSnapshot()
            if (policyStore.isBlueLightFilterActive()) {
                policyStore.addBlueLightActiveMinutes(1)
            }
        }
    }

    // Refresh immediately when returning from Android Settings permission grant
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Intercept Back key when overlays are active
    BackHandler(enabled = isSilentScreenActive || isPinLockActive) {
        if (isSilentScreenActive) {
            isSilentScreenActive = false
        }
        // When isPinLockActive is true, Back key is intentionally blocked until PIN unlock!
    }

    val activeProfileSummary = snapshot.memberSummaries.find { it.profile.id == snapshot.activeProfileId }
        ?: snapshot.memberSummaries.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Fixed Sidebar
            TvSidebarNavigation(
                currentTab = currentTab,
                deviceModelTag = snapshot.deviceModelTag,
                hasUsagePermission = snapshot.hasUsagePermission,
                onSelectTab = { tab -> currentTab = tab }
            )

            // Right Main Content Area
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                TvTopHeader(
                    networkLabel = snapshot.networkLabel,
                    isNetworkOnline = snapshot.isNetworkOnline,
                    currentTimeText = snapshot.currentTimeText,
                    activeProfileName = activeProfileSummary?.profile?.name ?: "家庭管理员",
                    isBlueLightActive = snapshot.isBlueLightFilterActive,
                    onToggleBlueLight = {
                        policyStore.setBlueLightFilterActive(!snapshot.isBlueLightFilterActive)
                        refreshTrigger++
                    },
                    onSwitchProfileClick = {
                        currentTab = NavTab.FAMILY_LIMITS
                    }
                )

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        NavTab.DASHBOARD -> {
                            DashboardScreen(
                                snapshot = snapshot,
                                onRequestUsagePermission = onOpenUsageSettings,
                                onInspectApp = { app ->
                                    focusedPackageForAppsScreen = app.packageName
                                    currentTab = NavTab.APPS
                                },
                                onToggleBlueLightFilter = {
                                    val next = !snapshot.isBlueLightFilterActive
                                    policyStore.setBlueLightFilterActive(next)
                                    onShowToast(if (next) "已开启深夜暖色蓝光滤镜" else "已关闭深夜暖色蓝光滤镜")
                                    refreshTrigger++
                                },
                                onActivateSilentScreenMode = {
                                    isSilentScreenActive = true
                                },
                                onTriggerBlinkReminder = {
                                    policyStore.incrementBlinkReminderToday()
                                    onShowToast("护眼提醒：请远眺 6 米外景物并眨眼放松 20 秒")
                                    refreshTrigger++
                                }
                            )
                        }

                        NavTab.APPS -> {
                            AppsScreen(
                                snapshot = snapshot,
                                selectedRange = selectedRange,
                                selectedCategory = selectedCategory,
                                initialSelectedPackage = focusedPackageForAppsScreen,
                                onSelectRange = { range -> selectedRange = range },
                                onCycleCategory = {
                                    val list = AppCategory.entries
                                    selectedCategory = list[(list.indexOf(selectedCategory) + 1) % list.size]
                                },
                                onSetAppDailyLimit = { pkg, limitMin ->
                                    policyStore.setAppDailyLimit(pkg, limitMin)
                                    onShowToast(if (limitMin > 0) "已设置每日限额 ${limitMin} 分钟" else "已取消该应用限额")
                                    refreshTrigger++
                                },
                                onToggleAppCurfew = { pkg ->
                                    val added = policyStore.toggleAppCurfew(pkg)
                                    onShowToast(if (added) "已加入晚间宵禁受控列表" else "已移出晚间宵禁列表")
                                    refreshTrigger++
                                }
                            )
                        }

                        NavTab.REPORTS -> {
                            ReportsScreen(
                                snapshot = snapshot,
                                onToggleWeekOffset = {
                                    weekOffset = if (weekOffset == 0) -1 else 0
                                },
                                onSelectMemberProfile = { profileId ->
                                    policyStore.setActiveProfileId(profileId)
                                    refreshTrigger++
                                }
                            )
                        }

                        NavTab.FAMILY_LIMITS -> {
                            FamilyLimitsScreen(
                                snapshot = snapshot,
                                currentPin = policyStore.getParentalPin(),
                                onSelectProfile = { profileId ->
                                    policyStore.setActiveProfileId(profileId)
                                    refreshTrigger++
                                },
                                onUpdateProfile = { updated ->
                                    policyStore.updateProfile(updated)
                                    refreshTrigger++
                                },
                                onAddProfile = { name, ageTag, isRestricted, weekdayMin, weekendMin ->
                                    val created = policyStore.addProfile(name, ageTag, isRestricted, weekdayMin, weekendMin)
                                    policyStore.setActiveProfileId(created.id)
                                    onShowToast("已添加家庭成员：${created.name}")
                                    refreshTrigger++
                                },
                                onCycleCategoryPolicy = { cat ->
                                    val next = policyStore.cycleCategoryPolicy(cat)
                                    onShowToast("${cat.displayName} 权限已切换为：${next.label}")
                                    refreshTrigger++
                                },
                                onSaveNewPin = { newPin ->
                                    policyStore.setParentalPin(newPin)
                                    onShowToast("家长安全 PIN 码已更新")
                                },
                                onSimulateLockScreen = {
                                    val profName = activeProfileSummary?.profile?.name ?: "小宝"
                                    pinLockReasonTitle = "防沉迷物理级黑屏锁 ($profName)"
                                    pinLockReasonSubtitle = "已触发家庭健康防沉迷策略，屏幕与遥控器方向键已锁定"
                                    isPinLockActive = true
                                }
                            )
                        }

                        NavTab.SETTINGS -> {
                            SettingsScreen(
                                snapshot = snapshot,
                                onRequestUsagePermission = onOpenUsageSettings,
                                onRequestOverlayPermission = onOpenOverlaySettings,
                                onUpdateFamilyDailyQuota = { min ->
                                    policyStore.setDailyFamilyQuotaMinutes(min)
                                    onShowToast("全家每日预设护眼额度已设为 ${min / 60} 小时")
                                    refreshTrigger++
                                },
                                onToggleBlueLightFilter = {
                                    val next = !snapshot.isBlueLightFilterActive
                                    policyStore.setBlueLightFilterActive(next)
                                    refreshTrigger++
                                },
                                onUpdateBlueLightSchedule = { h, m ->
                                    policyStore.setBlueLightAutoSchedule(h, m)
                                    onShowToast(String.format("已设置 %02d:%02d 自动开启暖色护眼", h, m))
                                    refreshTrigger++
                                },
                                onRefreshNow = {
                                    scope.launch { reloadSnapshot() }
                                    onShowToast("已同步最新系统使用数据")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Global Overlay 1: Warm Blue-Light Eye-Care Filter (Non-touchable warm amber tint)
        if (snapshot.isBlueLightFilterActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x26FF9800))
            )
        }

        // Global Overlay 2: Living Room Silent Mode ("客厅静默模式 · 熄屏伴听")
        if (isSilentScreenActive) {
            SilentAmbientScreenOverlay(
                currentTimeText = snapshot.currentTimeText,
                onDismiss = { isSilentScreenActive = false }
            )
        }

        // Global Overlay 3: Parental PIN Lock Screen
        if (isPinLockActive) {
            ParentalPinLockScreenOverlay(
                reasonTitle = pinLockReasonTitle,
                reasonSubtitle = pinLockReasonSubtitle,
                currentTimeText = snapshot.currentTimeText,
                expectedPin = policyStore.getParentalPin(),
                onUnlocked = {
                    isPinLockActive = false
                    onClearExternalLock()
                    onShowToast("家长 PIN 验证通过，已解除锁屏 (授予15分钟宽限期)")
                }
            )
        }
    }
}

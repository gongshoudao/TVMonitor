package com.tvpulse.tracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tvpulse.tracker.data.TvUsageRepository
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.AppUsageInfo
import com.tvpulse.tracker.model.HourlySlotStat
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.model.TvHardwareInputInfo
import com.tvpulse.tracker.ui.components.AppIconBox
import com.tvpulse.tracker.ui.components.tvClickable
import com.tvpulse.tracker.ui.components.tvFocusableCard
import com.tvpulse.tracker.ui.theme.TvColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    snapshot: SystemTelemetrySnapshot,
    onRequestUsagePermission: () -> Unit,
    onInspectApp: (AppUsageInfo) -> Unit,
    onToggleBlueLightFilter: () -> Unit,
    onActivateSilentScreenMode: () -> Unit,
    onTriggerBlinkReminder: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = {
        scope.launch {
            scrollState.animateScrollTo(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission Banner if Usage Access is not yet granted
        if (!snapshot.hasUsagePermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvClickable(
                        shapeRadius = 14.dp,
                        focusedScale = 1.015f,
                        focusedBorderColor = TvColors.Error,
                        unfocusedBorderColor = TvColors.Error,
                        unfocusedBorderWidth = 1.dp,
                        onFocused = scrollToTop
                    ) { onRequestUsagePermission() }
                    .background(TvColors.ErrorContainer.copy(alpha = 0.85f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TvColors.Error,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "尚未授予「使用情况访问权限 (PACKAGE_USAGE_STATS)」，点击此处立即前往授权",
                            color = TvColors.OnErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "授权后即可实时统计电视机各应用真实使用时长、24小时分时峰值与单次连续观看时长。",
                            color = TvColors.OnErrorContainer.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvColors.Error)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "立即前往授权",
                        color = TvColors.OnError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Section 1: Top Welcome & Real-Time Screen Health Summary
        TopWelcomeHeroSection(
            snapshot = snapshot,
            onFocusedToTop = scrollToTop
        )

        // Section 2: Running & Frequent Apps (4-Card Swimlane)
        RunningAppsSwimlaneSection(
            apps = snapshot.topRunningAndFrequentApps,
            tvInputs = snapshot.tvInputSources,
            onInspectApp = onInspectApp
        )

        // Section 3: 24h Hourly Timeline (8 cols) + Eye-Care & Bedtime Safeguard (4 cols)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            HourlyTimelineCard(
                slots = snapshot.hourlySlots,
                noonSummary = snapshot.noonPeakSummary,
                eveningSummary = snapshot.eveningPrimeSummary,
                modifier = Modifier.weight(7.6f)
            )

            EyeCareSafeguardCard(
                snapshot = snapshot,
                onToggleBlueLightFilter = onToggleBlueLightFilter,
                onActivateSilentScreenMode = onActivateSilentScreenMode,
                onTriggerBlinkReminder = onTriggerBlinkReminder,
                modifier = Modifier.weight(4.4f)
            )
        }
    }
}

@Composable
private fun TopWelcomeHeroSection(
    snapshot: SystemTelemetrySnapshot,
    onFocusedToTop: () -> Unit
) {
    val quotaHours = snapshot.dailyFamilyQuotaMinutes / 60
    val quotaRemMins = snapshot.dailyFamilyQuotaMinutes % 60
    val usedHours = snapshot.todayTotalMinutes / 60
    val usedMins = snapshot.todayTotalMinutes % 60
    val quotaConsumedPercent = ((snapshot.todayTotalMinutes * 100f) /
        snapshot.dailyFamilyQuotaMinutes.coerceAtLeast(1)).roundToInt().coerceIn(0, 999)
    val remainingQuotaMins = snapshot.dailyFamilyQuotaMinutes - snapshot.todayTotalMinutes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TvColors.SurfaceContainerLow)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Greeting & Summary (Focusable so D-Pad Up from below always scrolls to top)
        Column(
            modifier = Modifier
                .weight(1f)
                .tvFocusableCard(
                    shapeRadius = 14.dp,
                    focusedScale = 1.015f,
                    onFocused = onFocusedToTop
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (snapshot.hasUsagePermission) TvColors.SecondaryContainer else TvColors.TertiaryFixedDim)
                )
                Text(
                    text = if (snapshot.hasUsagePermission) {
                        "今日客厅大屏健康监测 · 实时系统数据同步"
                    } else {
                        "今日客厅大屏健康监测 · 等待系统使用权限授权"
                    },
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = snapshot.greetingTitle,
                    color = TvColors.OnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(TvColors.SurfaceContainerHigh)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${snapshot.memberSummaries.size}人共享档案",
                        color = TvColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val autoTimeStr = String.format(
                Locale.US,
                "%02d:%02d",
                snapshot.nightLightAutoHour,
                snapshot.nightLightAutoMinute
            )
            val dynamicAdvice = if (remainingQuotaMins >= 0) {
                "全天预设护眼额度 ${quotaHours}小时${String.format("%02d", quotaRemMins)}分钟，已累计观看 ${usedHours}小时${usedMins}分（剩余 ${remainingQuotaMins} 分钟），晚间 $autoTimeStr 自动开启护眼。"
            } else {
                "全天预设护眼额度 ${quotaHours}小时${String.format("%02d", quotaRemMins)}分钟，今日已超支 ${-remainingQuotaMins} 分钟，强烈建议立即执行客厅静默熄屏休息。"
            }

            Text(
                text = dynamicAdvice,
                color = TvColors.OnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right Quick Stats Bento Cells (Also focusable so D-Pad Up from right cards scrolls to top)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Card A: Time Gauge Card
            Column(
                modifier = Modifier
                    .width(166.dp)
                    .tvFocusableCard(
                        shapeRadius = 14.dp,
                        focusedScale = 1.03f,
                        onFocused = onFocusedToTop
                    )
                    .background(TvColors.SurfaceContainer)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日累计已看",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = TvColors.PrimaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$usedHours",
                        color = TvColors.Primary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "h ",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "$usedMins",
                        color = TvColors.Primary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "m",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(TvColors.SurfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((quotaConsumedPercent / 100f).coerceIn(0.03f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        TvColors.PrimaryContainer,
                                        TvColors.Secondary,
                                        TvColors.TertiaryFixedDim
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "额度 ${quotaConsumedPercent}%",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (remainingQuotaMins >= 0) TvColors.TertiaryContainer else TvColors.ErrorContainer
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (remainingQuotaMins >= 0) {
                                "余 ${TvUsageRepository.formatMinutesShort(remainingQuotaMins)}"
                            } else {
                                "超 ${TvUsageRepository.formatMinutesShort(-remainingQuotaMins)}"
                            },
                            color = if (remainingQuotaMins >= 0) TvColors.OnTertiaryContainer else TvColors.OnErrorContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Card B: Screen Health Score Card
            Column(
                modifier = Modifier
                    .width(162.dp)
                    .tvFocusableCard(
                        shapeRadius = 14.dp,
                        focusedScale = 1.03f,
                        onFocused = onFocusedToTop
                    )
                    .background(TvColors.SurfaceContainer)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "屏幕健康指数",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = TvColors.Secondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val ringColor = when {
                            snapshot.healthScore >= 75 -> TvColors.Secondary
                            snapshot.healthScore >= 60 -> TvColors.TertiaryFixedDim
                            else -> TvColors.Error
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = TvColors.SurfaceContainerHighest,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = (snapshot.healthScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${snapshot.healthScore}",
                            color = TvColors.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = snapshot.healthRatingLabel,
                            color = TvColors.Secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = snapshot.healthSubLabel,
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = TvColors.Secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "最长连续 ${TvUsageRepository.formatMinutesShort(snapshot.longestContinuousMinutes)}",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RunningAppsSwimlaneSection(
    apps: List<AppUsageInfo>,
    tvInputs: List<TvHardwareInputInfo>,
    onInspectApp: (AppUsageInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = TvColors.PrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "正在运行与常用应用 (本机真实检测)",
                    color = TvColors.OnSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "点击任意卡片查看详情或配置每日限额",
                color = TvColors.OnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        val displayApps = if (tvInputs.isNotEmpty() && apps.size >= 3) {
            apps.take(3)
        } else {
            apps.take(4)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayApps.forEachIndexed { idx, app ->
                DashboardAppCard(
                    app = app,
                    isPrimaryHighlight = idx == 0,
                    onClick = { onInspectApp(app) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (tvInputs.isNotEmpty()) {
                HardwareInputCard(
                    input = tvInputs.first(),
                    modifier = Modifier.weight(1f)
                )
            } else if (displayApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TvColors.SurfaceContainer)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无今日应用使用记录，使用电视应用后将在此实时显示",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardAppCard(
    app: AppUsageInfo,
    isPrimaryHighlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedMinutes = (app.todayTimeMs / 60_000L).toInt()
    val displayMs = if (app.todayTimeMs > 0) app.todayTimeMs else app.totalTimeMs
    val hours = (displayMs / 3_600_000L).toInt()
    val mins = ((displayMs % 3_600_000L) / 60_000L).toInt()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    Column(
        modifier = modifier
            .tvClickable(
                shapeRadius = 15.dp,
                focusedScale = 1.04f,
                unfocusedBorderColor = if (app.isOverLimit) {
                    TvColors.TertiaryFixedDim.copy(alpha = 0.6f)
                } else if (isPrimaryHighlight) {
                    TvColors.OutlineVariant.copy(alpha = 0.45f)
                } else {
                    Color.Transparent
                },
                unfocusedBorderWidth = if (app.isOverLimit || isPrimaryHighlight) 1.dp else 0.dp,
                onClick = onClick
            )
            .background(if (isPrimaryHighlight) TvColors.SurfaceContainerHigh else TvColors.SurfaceContainer)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row: App Icon + Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            AppIconBox(
                drawable = app.iconDrawable,
                category = app.category,
                size = 36.dp,
                cornerRadius = 9.dp
            )

            when {
                app.isOverLimit -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TvColors.TertiaryContainer.copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = TvColors.TertiaryFixedDim,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "达上限",
                            color = TvColors.TertiaryFixedDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                app.isCurrentlyRunning -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TvColors.PrimaryContainer.copy(alpha = 0.2f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(TvColors.PrimaryContainer)
                        )
                        Text(
                            text = "最近前台",
                            color = TvColors.PrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TvColors.SurfaceContainerHighest)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = app.category.displayName,
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Middle Row: App Name + Duration + Progress Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = app.appName,
                color = TvColors.OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${hours}h ${mins}m",
                    color = if (app.isOverLimit) TvColors.TertiaryFixedDim else TvColors.OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (app.isOverLimit) "触发锁屏" else "${app.percentageOfTotal}% 占比",
                    color = if (app.isOverLimit) TvColors.Error else TvColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val barColor = when {
                app.isOverLimit -> TvColors.TertiaryFixedDim
                app.isCurrentlyRunning -> TvColors.PrimaryContainer
                app.category == AppCategory.EDUCATION -> TvColors.SecondaryFixedDim
                else -> TvColors.PrimaryContainer
            }
            val barFraction = if (app.dailyLimitMinutes > 0) {
                (usedMinutes.toFloat() / app.dailyLimitMinutes).coerceIn(0.05f, 1f)
            } else {
                (app.percentageOfTotal / 100f).coerceIn(0.08f, 1f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerLowest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor)
                )
            }
        }

        // Bottom Footer: Real Last Used / Limit Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftText = when {
                app.activeMediaTitle != null -> app.activeMediaTitle
                app.dailyLimitMinutes > 0 -> "限额 ${app.dailyLimitMinutes}m/日"
                app.lastTimeUsedMs > 0 -> "活跃 ${timeFormat.format(Date(app.lastTimeUsedMs))}"
                else -> app.packageName.substringAfterLast('.')
            }
            val rightText = when {
                app.isOverLimit -> "锁定中"
                app.activeMediaTitle != null -> "播放中"
                app.isCurrentlyRunning -> "活跃中"
                app.todayTimeMs > 0 -> "启动${app.launchCount}次"
                else -> "待机"
            }
            Text(
                text = leftText,
                color = TvColors.OnSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rightText,
                color = if (app.isOverLimit) TvColors.Error else TvColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun HardwareInputCard(
    input: TvHardwareInputInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .tvFocusableCard(shapeRadius = 15.dp, focusedScale = 1.038f)
            .background(TvColors.SurfaceContainer)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(TvColors.SurfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsInputHdmi,
                    contentDescription = null,
                    tint = TvColors.Secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerHighest)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = input.typeLabel,
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "物理信号源 · ${input.name}",
                color = TvColors.OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (input.isConnected) "信号就绪" else "未接入",
                    color = TvColors.OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "TV Input",
                    color = TvColors.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (input.isConnected) 1f else 0.2f)
                        .fillMaxHeight()
                        .background(TvColors.SecondaryContainer)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "通道: ${input.id.takeLast(10)}",
                color = TvColors.OnSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (input.isConnected) "已连接" else "已断开",
                color = TvColors.Secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun HourlyTimelineCard(
    slots: List<HourlySlotStat>,
    noonSummary: String,
    eveningSummary: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .tvFocusableCard(shapeRadius = 18.dp, focusedScale = 1.015f)
            .background(TvColors.SurfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "24小时大屏活跃峰值分布",
                        color = TvColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "基于 UsageEvents 实时切片各时段时长",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot(color = TvColors.PrimaryContainer, label = "影音")
                LegendDot(color = TvColors.Secondary, label = "知识/健身")
                LegendDot(color = TvColors.TertiaryFixedDim, label = "少儿")
            }
        }

        // Bar Chart Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TvColors.SurfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val maxSlotMin = (slots.maxOfOrNull { it.totalMinutes } ?: 30).coerceAtLeast(30)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                slots.forEach { slot ->
                    val frac = if (slot.totalMinutes > 0) {
                        (slot.totalMinutes.toFloat() / maxSlotMin).coerceIn(0.08f, 1f)
                    } else {
                        0.05f
                    }
                    val barColor = when {
                        slot.totalMinutes == 0 -> TvColors.SurfaceContainerHighest
                        slot.dominantCategory == AppCategory.KIDS -> TvColors.TertiaryFixedDim
                        slot.dominantCategory == AppCategory.EDUCATION ||
                            slot.dominantCategory == AppCategory.GAME_FITNESS -> TvColors.Secondary
                        else -> TvColors.PrimaryContainer
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (slot.totalMinutes > 0) "${slot.totalMinutes}m" else "",
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.76f)
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(barColor)
                                    .border(
                                        width = if (slot.isCurrentSlot) 1.5.dp else 0.dp,
                                        color = if (slot.isCurrentSlot) TvColors.Primary else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = slot.label,
                            color = if (slot.isCurrentSlot) TvColors.Primary else TvColors.OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (slot.isCurrentSlot) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Bottom Annotation Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = TvColors.TertiaryFixedDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = noonSummary,
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = eveningSummary,
                        color = TvColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            color = TvColors.OnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun EyeCareSafeguardCard(
    snapshot: SystemTelemetrySnapshot,
    onToggleBlueLightFilter: () -> Unit,
    onActivateSilentScreenMode: () -> Unit,
    onTriggerBlinkReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TvColors.SurfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = TvColors.Secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "护眼与休眠守则",
                    color = TvColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.SecondaryContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "已激活",
                    color = TvColors.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 1: Anti-fatigue blink reminder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shapeRadius = 10.dp, focusedScale = 1.025f) { onTriggerBlinkReminder() }
                .background(TvColors.SurfaceContainerLow.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "防疲劳眨眼提醒",
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "每45分钟屏显提示 · 点击触发",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${snapshot.blinkReminderCountToday} 次",
                color = TvColors.Primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        // Row 2: Longest single continuous viewing session
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusableCard(shapeRadius = 10.dp, focusedScale = 1.02f)
                .background(TvColors.SurfaceContainerLow.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "单次最长连续观看",
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "建议不超过 100 分钟",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = TvUsageRepository.formatMinutesShort(snapshot.longestContinuousMinutes),
                color = if (snapshot.longestContinuousMinutes > 100) TvColors.Error else TvColors.TertiaryFixedDim,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        // Row 3: Night Blue Light Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shapeRadius = 10.dp, focusedScale = 1.025f) { onToggleBlueLightFilter() }
                .background(TvColors.SurfaceContainerLow.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "深夜护眼蓝光滤镜",
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%02d:%02d 自动暖色 · 点击切换",
                        snapshot.nightLightAutoHour,
                        snapshot.nightLightAutoMinute
                    ),
                    color = TvColors.Secondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (snapshot.isBlueLightFilterActive) TvColors.TertiaryFixedDim else TvColors.Secondary)
                )
                Text(
                    text = snapshot.nightLightCountdownText,
                    color = TvColors.OnSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Action Button: Execute Living Room Silent Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shapeRadius = 12.dp, focusedScale = 1.03f) { onActivateSilentScreenMode() }
                .background(TvColors.SurfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Nightlight,
                contentDescription = null,
                tint = TvColors.PrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "开启客厅静默模式 (熄屏伴听)",
                color = TvColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.tvpulse.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.ui.components.tvClickable
import com.tvpulse.tracker.ui.theme.TvColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    snapshot: SystemTelemetrySnapshot,
    onRequestUsagePermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onUpdateFamilyDailyQuota: (Int) -> Unit,
    onToggleBlueLightFilter: () -> Unit,
    onUpdateBlueLightSchedule: (hour: Int, minute: Int) -> Unit,
    onRefreshNow: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = {
        scope.launch { scrollState.animateScrollTo(0) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "系统权限与全局健康设置",
                    color = TvColors.OnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "管理 Android TV 底层使用统计权限、跨应用护眼悬浮窗与全家每日总护眼额度",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .tvClickable(
                        shapeRadius = 10.dp,
                        focusedScale = 1.04f,
                        onFocused = scrollToTop,
                        onClick = onRefreshNow
                    )
                    .background(TvColors.PrimaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "立即刷新系统数据",
                    color = TvColors.OnPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Permission Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: PACKAGE_USAGE_STATS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvColors.SurfaceContainer)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = TvColors.PrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "应用使用情况访问权限 (核心)",
                            color = TvColors.OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (snapshot.hasUsagePermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (snapshot.hasUsagePermission) TvColors.Secondary else TvColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (snapshot.hasUsagePermission) "已授权" else "未授权",
                            color = if (snapshot.hasUsagePermission) TvColors.Secondary else TvColors.Error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "用于读取 UsageStatsManager 真实应用前台时长、24小时分时活跃事件流与周度统计。若部分精简版电视 ROM 设置页被隐藏，可通过 ADB 授权：\nadb shell appops set com.tvpulse.tracker GET_USAGE_STATS allow",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Box(
                    modifier = Modifier
                        .tvClickable(
                            shapeRadius = 10.dp,
                            focusedScale = 1.035f,
                            onFocused = scrollToTop,
                            onClick = onRequestUsagePermission
                        )
                        .background(if (snapshot.hasUsagePermission) TvColors.SurfaceContainerHigh else TvColors.PrimaryContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (snapshot.hasUsagePermission) "打开系统使用情况设置" else "立即授权使用情况统计",
                        color = if (snapshot.hasUsagePermission) TvColors.OnSurface else TvColors.OnPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Card 2: SYSTEM_ALERT_WINDOW
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvColors.SurfaceContainer)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TvColors.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "跨应用护眼与锁屏悬浮窗",
                            color = TvColors.OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (snapshot.hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (snapshot.hasOverlayPermission) TvColors.Secondary else TvColors.TertiaryFixedDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (snapshot.hasOverlayPermission) "已授权" else "可选授权",
                            color = if (snapshot.hasOverlayPermission) TvColors.Secondary else TvColors.TertiaryFixedDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "用于在观看其他视频应用时叠加全局暖色防蓝光滤镜及防沉迷提醒。ADB 快捷授权命令：\nadb shell appops set com.tvpulse.tracker SYSTEM_ALERT_WINDOW allow",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Box(
                    modifier = Modifier
                        .tvClickable(
                            shapeRadius = 10.dp,
                            focusedScale = 1.035f,
                            onFocused = scrollToTop,
                            onClick = onRequestOverlayPermission
                        )
                        .background(TvColors.SurfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "打开悬浮窗权限设置",
                        color = TvColors.OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Family Daily Quota & Night Light Schedule
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvColors.SurfaceContainer)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "全天预设家庭总护眼额度",
                        color = TvColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val presets = listOf(180, 240, 300, 360, 420)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { min ->
                        val isSelected = snapshot.dailyFamilyQuotaMinutes == min
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvClickable(shapeRadius = 10.dp, focusedScale = 1.05f) { onUpdateFamilyDailyQuota(min) }
                                .background(if (isSelected) TvColors.PrimaryContainer else TvColors.SurfaceContainerHigh)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${min / 60}小时",
                                color = if (isSelected) TvColors.OnPrimaryContainer else TvColors.OnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
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
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = TvColors.Secondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "深夜护眼暖色滤镜定时",
                            color = TvColors.OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .tvClickable(shapeRadius = 8.dp, focusedScale = 1.05f) { onToggleBlueLightFilter() }
                            .background(if (snapshot.isBlueLightFilterActive) TvColors.TertiaryContainer else TvColors.SurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (snapshot.isBlueLightFilterActive) "已开启 · 点击关闭" else "已关闭 · 点击开启",
                            color = if (snapshot.isBlueLightFilterActive) TvColors.OnTertiaryContainer else TvColors.OnSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val scheduleOptions = listOf(20 to 30, 21 to 0, 21 to 30, 22 to 0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scheduleOptions.forEach { (h, m) ->
                        val isSelected = snapshot.nightLightAutoHour == h && snapshot.nightLightAutoMinute == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvClickable(shapeRadius = 10.dp, focusedScale = 1.05f) { onUpdateBlueLightSchedule(h, m) }
                                .background(if (isSelected) TvColors.SecondaryContainer else TvColors.SurfaceContainerHigh)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", h, m),
                                color = if (isSelected) TvColors.OnSecondaryContainer else TvColors.OnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

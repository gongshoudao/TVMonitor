package com.tvpulse.tracker.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.AppRangeFilter
import com.tvpulse.tracker.model.AppUsageInfo
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.ui.components.AppHourlyDetailDialog
import com.tvpulse.tracker.ui.components.AppIconBox
import com.tvpulse.tracker.ui.components.AppLimitConfigDialog
import com.tvpulse.tracker.ui.components.tvClickable
import com.tvpulse.tracker.ui.theme.TvColors
import java.util.Locale

@Composable
fun AppsScreen(
    snapshot: SystemTelemetrySnapshot,
    selectedRange: AppRangeFilter,
    selectedCategory: AppCategory,
    initialSelectedPackage: String?,
    onSelectRange: (AppRangeFilter) -> Unit,
    onCycleCategory: () -> Unit,
    onSetAppDailyLimit: (packageName: String, limitMinutes: Int) -> Unit,
    onToggleAppCurfew: (packageName: String) -> Unit
) {
    val apps = snapshot.rankedApps
    var selectedPkg by remember(initialSelectedPackage) {
        mutableStateOf(initialSelectedPackage ?: apps.firstOrNull()?.packageName)
    }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty() && apps.none { it.packageName == selectedPkg }) {
            selectedPkg = apps.first().packageName
        }
    }

    val selectedIndex = apps.indexOfFirst { it.packageName == selectedPkg }.coerceAtLeast(0)
    val selectedApp = apps.getOrNull(selectedIndex)

    var showLimitDialog by remember { mutableStateOf(false) }
    var showHourlyDialog by remember { mutableStateOf(false) }

    val totalRangeMs = apps.sumOf { it.totalTimeMs }
    val totalRangeHours = totalRangeMs / 3_600_000L
    val totalRangeMins = (totalRangeMs % 3_600_000L) / 60_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Segmented Bar & Telemetry Ambient Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerLowest)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppRangeFilter.entries.forEach { filter ->
                    val isSelected = filter == selectedRange
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .tvClickable(shapeRadius = 999.dp, focusedScale = 1.04f) { onSelectRange(filter) }
                            .background(if (isSelected) TvColors.PrimaryContainer else TvColors.SurfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TvColors.OnPrimaryContainer)
                            )
                        }
                        Text(
                            text = filter.label.substringBefore(' '),
                            color = if (isSelected) TvColors.OnPrimaryContainer else TvColors.OnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Category Filter Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .tvClickable(shapeRadius = 999.dp, focusedScale = 1.04f) { onCycleCategory() }
                        .background(
                            if (selectedCategory != AppCategory.ALL) TvColors.SecondaryContainer else TvColors.SurfaceContainer
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (selectedCategory != AppCategory.ALL) TvColors.OnSecondaryContainer else TvColors.OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "分类: ${selectedCategory.shortLabel}",
                        color = if (selectedCategory != AppCategory.ALL) TvColors.OnSecondaryContainer else TvColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Right Telemetry Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TvColors.Secondary)
                    )
                    Text(
                        text = "已监测 ${snapshot.monitoredAppsCount} 款",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(TvColors.OutlineVariant)
                )
                Text(
                    text = "${selectedRange.label.substringBefore(' ')}累计: ${totalRangeHours}h ${totalRangeMins}m",
                    color = TvColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Main 12-Col Split Viewport Layout (5 Cols Left Spotlight + 7 Cols Right List)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column (5 cols): Spotlight Inspector
            Box(modifier = Modifier.weight(5f)) {
                if (selectedApp != null) {
                    AppSpotlightInspectorCard(
                        app = selectedApp,
                        rank = selectedIndex + 1,
                        rangeFilter = selectedRange,
                        onOpenLimitDialog = { showLimitDialog = true },
                        onToggleCurfew = { onToggleAppCurfew(selectedApp.packageName) },
                        onOpenHourlyDetail = { showHourlyDialog = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(TvColors.SurfaceContainer)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "该分类下暂无应用记录",
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Right Column (7 cols): High-Contrast TV Remote Focus List
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "应用时长排行榜 (共 ${apps.size} 款)",
                        color = TvColors.OnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = null,
                            tint = TvColors.OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "遥控器上下浏览 · 点击选中",
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(apps, key = { _, item -> item.packageName }) { idx, item ->
                        val isSelected = item.packageName == selectedApp?.packageName
                        AppLeaderboardRow(
                            rank = idx + 1,
                            app = item,
                            isSelected = isSelected,
                            onSelect = { selectedPkg = item.packageName }
                        )
                    }
                }
            }
        }
    }

    if (showLimitDialog && selectedApp != null) {
        AppLimitConfigDialog(
            app = selectedApp,
            onDismiss = { showLimitDialog = false },
            onSaveLimit = { limitMin ->
                onSetAppDailyLimit(selectedApp.packageName, limitMin)
                showLimitDialog = false
            }
        )
    }

    if (showHourlyDialog && selectedApp != null) {
        AppHourlyDetailDialog(
            app = selectedApp,
            onDismiss = { showHourlyDialog = false }
        )
    }
}

@Composable
private fun AppSpotlightInspectorCard(
    app: AppUsageInfo,
    rank: Int,
    rangeFilter: AppRangeFilter,
    onOpenLimitDialog: () -> Unit,
    onToggleCurfew: () -> Unit,
    onOpenHourlyDetail: () -> Unit
) {
    val totalHours = app.totalTimeMs / 3_600_000L
    val totalMins = (app.totalTimeMs % 3_600_000L) / 60_000L
    val avgHoursPerDay = (app.totalTimeMs / 3_600_000f) / rangeFilter.days.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(TvColors.SurfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Header Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AppIconBox(
                    drawable = app.iconDrawable,
                    category = app.category,
                    size = 48.dp,
                    cornerRadius = 12.dp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvColors.PrimaryContainer)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "TV",
                        color = TvColors.OnPrimaryContainer,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvColors.Primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "高频应用 #$rank",
                            color = TvColors.Primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = if (app.trendPercent >= 0) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = if (app.trendPercent >= 0) TvColors.Secondary else TvColors.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                        val sign = if (app.trendPercent >= 0) "+" else ""
                        Text(
                            text = "$sign${app.trendPercent}%",
                            color = if (app.trendPercent >= 0) TvColors.Secondary else TvColors.Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = app.appName,
                    color = TvColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${app.category.displayName} · ${app.packageName}",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Metric Highlight Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(TvColors.SurfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "统计区间使用时长",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$totalHours",
                        color = TvColors.Primary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " 小时 ",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "$totalMins",
                        color = TvColors.Primary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " 分",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "日均观看",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = String.format(Locale.US, "%.1f 小时", avgHoursPerDay),
                    color = TvColors.Secondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 7-Day Usage Trend Sparkline
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val dayNames = listOf("6天前", "5天前", "4天前", "3天前", "前天", "昨天", "今日")
            val peakIdx = app.dailySparklineHours.indices.maxByOrNull { app.dailySparklineHours[it] } ?: 6
            val peakVal = app.dailySparklineHours.getOrElse(peakIdx) { 0f }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "近7天观看波动趋势 (h)",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = String.format(Locale.US, "峰值 %s (%.1fh)", dayNames[peakIdx], peakVal),
                    color = TvColors.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(TvColors.SurfaceContainerLowest)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                SparklineAreaChart(
                    values = app.dailySparklineHours,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayNames.forEachIndexed { idx, label ->
                        Text(
                            text = label,
                            color = if (idx == peakIdx) TvColors.Primary else TvColors.OnSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = if (idx == peakIdx) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Time-of-Day Distribution Segmented Bar (100% Real Telemetry)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "活跃时段分布 (真实事件流)",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "3 大活跃频带",
                    color = TvColors.OnSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerLowest)
            ) {
                val p1 = app.primeEveningPercent.coerceAtLeast(5) / 100f
                val p2 = app.daytimePercent.coerceAtLeast(5) / 100f
                val p3 = app.lateNightPercent.coerceAtLeast(2) / 100f
                Box(
                    modifier = Modifier
                        .weight(p1)
                        .fillMaxHeight()
                        .background(TvColors.PrimaryContainer)
                )
                Box(
                    modifier = Modifier
                        .weight(p2)
                        .fillMaxHeight()
                        .background(TvColors.SecondaryContainer)
                )
                Box(
                    modifier = Modifier
                        .weight(p3)
                        .fillMaxHeight()
                        .background(TvColors.TertiaryContainer)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendSegmentItem(color = TvColors.PrimaryContainer, text = "黄金档 ${app.primeEveningPercent}%")
                LegendSegmentItem(color = TvColors.SecondaryContainer, text = "日间 ${app.daytimePercent}%")
                LegendSegmentItem(color = TvColors.TertiaryContainer, text = "深夜 ${app.lateNightPercent}%")
            }
        }

        // Quick Remote D-Pad Action Rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val limitLabel = if (app.dailyLimitMinutes > 0) {
                "修改应用每日限额 (已设 ${app.dailyLimitMinutes}m)"
            } else {
                "设置应用每日限额 (当前不限额)"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvClickable(shapeRadius = 12.dp, focusedScale = 1.03f) { onOpenLimitDialog() }
                    .background(TvColors.Primary)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = TvColors.OnPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = limitLabel,
                        color = TvColors.OnPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TvColors.OnPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .tvClickable(shapeRadius = 12.dp, focusedScale = 1.03f) { onToggleCurfew() }
                        .background(
                            if (app.inNightCurfew) TvColors.TertiaryContainer else TvColors.SurfaceContainerHigh
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = if (app.inNightCurfew) TvColors.OnTertiaryContainer else TvColors.TertiaryFixedDim,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (app.inNightCurfew) "已加入宵禁" else "加入晚间宵禁",
                        color = if (app.inNightCurfew) TvColors.OnTertiaryContainer else TvColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .tvClickable(shapeRadius = 12.dp, focusedScale = 1.03f) { onOpenHourlyDetail() }
                        .background(TvColors.SurfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "单日时段明细",
                        color = TvColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SparklineAreaChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val safeValues = if (values.size == 7) values else List(7) { 0f }
    val maxV = (safeValues.maxOrNull() ?: 1f).coerceAtLeast(0.5f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stepX = w / (safeValues.size - 1).coerceAtLeast(1)

        val points = safeValues.mapIndexed { idx, v ->
            val x = idx * stepX
            val y = h - ((v / maxV).coerceIn(0f, 1f) * (h * 0.82f) + h * 0.08f)
            Offset(x, y)
        }

        val areaPath = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                listOf(
                    TvColors.PrimaryContainer.copy(alpha = 0.35f),
                    TvColors.PrimaryContainer.copy(alpha = 0.0f)
                )
            )
        )

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = linePath,
            color = TvColors.PrimaryContainer,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEachIndexed { idx, pt ->
            drawCircle(
                color = if (idx == points.lastIndex) TvColors.Primary else TvColors.Background,
                radius = if (idx == points.lastIndex) 5.dp.toPx() else 3.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = TvColors.PrimaryContainer,
                radius = if (idx == points.lastIndex) 5.dp.toPx() else 3.5.dp.toPx(),
                center = pt,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun LegendSegmentItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = TvColors.OnSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AppLeaderboardRow(
    rank: Int,
    app: AppUsageInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val active = isSelected || isFocused
    val hours = app.totalTimeMs / 3_600_000L
    val mins = (app.totalTimeMs % 3_600_000L) / 60_000L
    val durText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(
                shapeRadius = 14.dp,
                focusedScale = 1.022f,
                unfocusedBorderColor = if (isSelected) TvColors.OutlineVariant.copy(alpha = 0.55f) else Color.Transparent,
                unfocusedBorderWidth = if (isSelected) 1.dp else 0.dp,
                onFocused = {
                    isFocused = true
                    onSelect()
                },
                onClick = onSelect
            )
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (active) TvColors.SurfaceContainerHigh else TvColors.SurfaceContainer)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (active) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(TvColors.PrimaryContainer)
                )
            }
            Text(
                text = "$rank",
                color = if (active) TvColors.Primary else TvColors.OnSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )
            AppIconBox(
                drawable = app.iconDrawable,
                category = app.category,
                size = 44.dp,
                cornerRadius = 10.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = app.appName,
                        color = if (active) TvColors.Primary else TvColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    when {
                        isSelected -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvColors.PrimaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "当前选中",
                                    color = TvColors.OnPrimaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        app.isOverLimit -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvColors.ErrorContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = TvColors.OnErrorContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "已达限额",
                                    color = TvColors.OnErrorContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        app.dailyLimitMinutes > 0 -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvColors.TertiaryContainer.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "限额 ${app.dailyLimitMinutes}m",
                                    color = TvColors.TertiaryFixedDim,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        app.category == AppCategory.SYSTEM_CAST -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvColors.SurfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "系统",
                                    color = TvColors.OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvColors.SurfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "正常",
                                    color = TvColors.Secondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "${app.category.displayName} · ${app.packageName}",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durText,
                    color = if (app.isOverLimit) TvColors.Error else if (active) TvColors.Primary else TvColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(TvColors.SurfaceContainerLowest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((app.percentageOfTotal / 100f).coerceIn(0.04f, 1f))
                                .fillMaxHeight()
                                .background(
                                    if (app.isOverLimit) TvColors.Error else if (active) TvColors.PrimaryContainer else TvColors.Secondary
                                )
                        )
                    }
                    Text(
                        text = "${app.percentageOfTotal}%",
                        color = if (active) TvColors.Primary else TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (active) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(TvColors.PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TvColors.OnPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TvColors.OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

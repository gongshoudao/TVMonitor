package com.tvpulse.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpulse.tracker.data.TvUsageRepository
import com.tvpulse.tracker.model.MemberUsageSummary
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.model.WeeklyReportData
import com.tvpulse.tracker.ui.components.tvClickable
import com.tvpulse.tracker.ui.components.tvFocusableCard
import com.tvpulse.tracker.ui.theme.TvColors
import com.tvpulse.tracker.util.QrGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReportsScreen(
    snapshot: SystemTelemetrySnapshot,
    onToggleWeekOffset: () -> Unit,
    onSelectMemberProfile: (String) -> Unit
) {
    val report = snapshot.weeklyReport
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
        // Header Banner
        WeeklyReportHeaderBanner(
            report = report,
            memberCount = snapshot.memberSummaries.size,
            onFocusedToTop = scrollToTop,
            onToggleWeekOffset = onToggleWeekOffset
        )

        // Main 3-Column Bento Deck (4.4 + 3.8 + 3.8)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            WeeklyBreakdownColumn(
                report = report,
                modifier = Modifier.weight(4.4f)
            )

            TimeOfDayDonutColumn(
                report = report,
                modifier = Modifier.weight(3.8f)
            )

            AiAdvisorAndQrColumn(
                report = report,
                modifier = Modifier.weight(3.8f)
            )
        }

        // Bottom Swimlane: Family Member Screen Time Contribution
        MemberContributionSwimlane(
            members = snapshot.memberSummaries,
            activeProfileId = snapshot.activeProfileId,
            onSelectMemberProfile = onSelectMemberProfile
        )
    }
}

@Composable
private fun WeeklyReportHeaderBanner(
    report: WeeklyReportData,
    memberCount: Int,
    onFocusedToTop: () -> Unit,
    onToggleWeekOffset: () -> Unit
) {
    val totalHours = report.totalWeeklyMinutes / 60
    val totalMins = report.totalWeeklyMinutes % 60
    val isDown = report.weekOverWeekChangePercent <= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TvColors.SurfaceContainerLow)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .tvFocusableCard(shapeRadius = 12.dp, focusedScale = 1.015f, onFocused = onFocusedToTop)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TvColors.Primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LEANBACK HEALTH REPORT",
                        color = TvColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TvColors.Secondary)
                )
                Text(
                    text = "全家 $memberCount 人档案",
                    color = TvColors.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = if (report.weekOffset == 0) "本周电视使用洞察报告" else "上周电视使用洞察报告",
                color = TvColors.OnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = report.dateRangeText,
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Right Cluster: Switch Week + Total Weekly Screen Time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .tvClickable(
                        shapeRadius = 999.dp,
                        focusedScale = 1.045f,
                        onFocused = onFocusedToTop,
                        onClick = onToggleWeekOffset
                    )
                    .background(TvColors.SurfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (report.weekOffset == 0) "切换至上周" else "切换至本周",
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvColors.SurfaceContainerHighest.copy(alpha = 0.7f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "全周总开机时长",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalHours",
                            color = TvColors.Primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " 小时 ",
                            color = TvColors.OnSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                            text = "$totalMins",
                            color = TvColors.Primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " 分",
                            color = TvColors.OnSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDown) TvColors.Secondary.copy(alpha = 0.12f)
                                else TvColors.TertiaryContainer.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isDown) {
                                Icons.AutoMirrored.Filled.TrendingDown
                            } else {
                                Icons.AutoMirrored.Filled.TrendingUp
                            },
                            contentDescription = null,
                            tint = if (isDown) TvColors.Secondary else TvColors.TertiaryFixedDim,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${abs(report.weekOverWeekChangePercent)}%",
                            color = if (isDown) TvColors.Secondary else TvColors.TertiaryFixedDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isDown) "较上周降幅" else "较上周增幅",
                        color = if (isDown) TvColors.Secondary else TvColors.TertiaryFixedDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBreakdownColumn(
    report: WeeklyReportData,
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
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvColors.PrimaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "每日使用趋势",
                        color = TvColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "分类时长累计 (小时)",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(TvColors.Secondary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "达标日 ${report.compliantDaysCount}/7",
                    color = TvColors.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Category Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallCatLegend(color = TvColors.PrimaryContainer, text = "影视")
            SmallCatLegend(color = TvColors.Secondary, text = "少儿")
            SmallCatLegend(color = TvColors.TertiaryContainer, text = "游戏")
            SmallCatLegend(color = TvColors.SurfaceBright, text = "教育/系统")
        }

        // Stacked Bar Chart
        val maxDayHours = (report.dailyBars.maxOfOrNull { it.totalHours } ?: 2f).coerceAtLeast(2f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            report.dailyBars.forEachIndexed { idx, day ->
                val isPeak = idx == report.peakDayIndex && day.totalHours > 0f
                val isLowest = idx == report.lowestDayIndex && day.totalHours > 0f && idx != report.peakDayIndex
                val totalFrac = (day.totalHours / maxDayHours).coerceIn(0.08f, 1f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isPeak) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TvColors.TertiaryContainer)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "峰值",
                                        color = TvColors.OnTertiaryContainer,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else if (isLowest) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TvColors.Secondary.copy(alpha = 0.2f))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "最低",
                                        color = TvColors.Secondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Text(
                                text = String.format(Locale.US, "%.1f", day.totalHours),
                                color = when {
                                    isPeak -> TvColors.TertiaryFixedDim
                                    isLowest -> TvColors.Secondary
                                    else -> TvColors.OnSurfaceVariant
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(3.dp))

                            // Stacked Bar
                            val sumNonZero = day.totalHours.coerceAtLeast(0.01f)
                            Column(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(totalFrac)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TvColors.SurfaceContainerLow)
                                    .border(
                                        width = if (isPeak || isLowest) 1.dp else 0.dp,
                                        color = when {
                                            isPeak -> TvColors.TertiaryFixedDim
                                            isLowest -> TvColors.Secondary
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                if (day.totalHours <= 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(TvColors.SurfaceContainerHighest)
                                    )
                                } else {
                                    if (day.eduSystemHours > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight((day.eduSystemHours / sumNonZero).coerceAtLeast(0.05f))
                                                .background(TvColors.SurfaceBright)
                                        )
                                    }
                                    if (day.gameHours > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight((day.gameHours / sumNonZero).coerceAtLeast(0.05f))
                                                .background(TvColors.TertiaryContainer)
                                        )
                                    }
                                    if (day.kidsHours > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight((day.kidsHours / sumNonZero).coerceAtLeast(0.05f))
                                                .background(TvColors.Secondary)
                                        )
                                    }
                                    if (day.videoHours >= 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight((day.videoHours / sumNonZero).coerceAtLeast(0.1f))
                                                .background(TvColors.PrimaryContainer)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day.dayOfWeekLabel,
                        color = when {
                            isPeak -> TvColors.TertiaryFixedDim
                            isLowest -> TvColors.Secondary
                            else -> TvColors.OnSurfaceVariant
                        },
                        fontSize = 10.sp,
                        fontWeight = if (isPeak || isLowest) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Bottom Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(TvColors.SurfaceContainerLow)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = TvColors.Secondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "工作日日均 %.1f 小时 (健康阈值 3.5h)",
                        report.weekdayAvgHours
                    ),
                    color = TvColors.OnSurface,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (report.weekdayAvgHours <= 3.5f) "已达标" else "需关注",
                color = if (report.weekdayAvgHours <= 3.5f) TvColors.Secondary else TvColors.TertiaryFixedDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SmallCatLegend(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = TvColors.OnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun TimeOfDayDonutColumn(
    report: WeeklyReportData,
    modifier: Modifier = Modifier
) {
    val dist = report.timeDistribution

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
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvColors.Secondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TvColors.Secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "时段热力分布",
                        color = TvColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "日间习惯与宵禁遵从率",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Nightlight,
                contentDescription = null,
                tint = TvColors.OnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Donut Visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                drawArc(
                    color = TvColors.SurfaceContainerLow,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                var startAngle = -90f
                val segments = listOf(
                    dist.primePercent to TvColors.PrimaryContainer,
                    dist.noonPercent to TvColors.Secondary,
                    dist.otherPercent to TvColors.SurfaceBright,
                    dist.lateNightPercent to TvColors.Error
                )
                segments.forEach { (pct, color) ->
                    val sweep = (pct / 100f) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = stroke
                        )
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "黄金档占比",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dist.primePercent}%",
                    color = TvColors.Primary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "健康时段",
                    color = TvColors.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Segment Details List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DonutSegmentRow(
                dotColor = TvColors.PrimaryContainer,
                title = "黄金档 (19-22点)",
                valueText = String.format(
                    Locale.US,
                    "%d%% (%.1fh)",
                    dist.primePercent,
                    dist.primeEveningMinutes / 60f
                ),
                valueColor = TvColors.Primary
            )
            DonutSegmentRow(
                dotColor = TvColors.Secondary,
                title = "午间档 (12-14点)",
                valueText = String.format(
                    Locale.US,
                    "%d%% (%.1fh)",
                    dist.noonPercent,
                    dist.noonBreakMinutes / 60f
                ),
                valueColor = TvColors.Secondary
            )
            DonutSegmentRow(
                dotColor = TvColors.Error,
                title = "深夜 (23点后)",
                valueText = "${dist.lateNightPercent}% (${dist.lateNightMinutes}分)",
                valueColor = TvColors.Error
            )
        }
    }
}

@Composable
private fun DonutSegmentRow(
    dotColor: Color,
    title: String,
    valueText: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TvColors.SurfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = title,
                color = TvColors.OnSurface,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = valueText,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun AiAdvisorAndQrColumn(
    report: WeeklyReportData,
    modifier: Modifier = Modifier
) {
    var countdownSeconds by remember { mutableIntStateOf(299) }
    LaunchedEffect(report.qrReportContent) {
        countdownSeconds = 299
        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        }
    }
    val qrBitmap = remember(report.qrReportContent) {
        QrGenerator.generateQrBitmap(report.qrReportContent, sizePx = 280)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Advisor Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusableCard(shapeRadius = 18.dp, focusedScale = 1.015f)
                .background(TvColors.SurfaceContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TvColors.Secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = TvColors.Secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "健康模范家庭",
                        color = TvColors.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(TvColors.SecondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LV.${report.familyLevel} 达成",
                        color = TvColors.OnSecondaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvColors.SurfaceContainerHigh)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "护眼打卡完成率",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${report.eyeCareCompletionPercent}%",
                        color = TvColors.Secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(TvColors.SurfaceContainerLow)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(report.eyeCareCompletionPercent / 100f)
                            .fillMaxHeight()
                            .background(TvColors.Secondary)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier
                        .size(15.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = "AI 管家评语：${report.aiAdvisorComment}",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvColors.SurfaceContainerLow)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "夜间护眼蓝光滤镜",
                        color = TvColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = String.format(Locale.US, "累计削减 %.1f 小时高危眩光", report.blueLightReducedHours),
                        color = TvColors.Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // QR Code Mobile Companion Sync Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusableCard(shapeRadius = 18.dp, focusedScale = 1.015f)
                .background(TvColors.SurfaceContainerLow)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "扫码带走完整健康周报",
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = if (report.qrLocalUrl != null) {
                    "手机连同局域网扫码查看实时周报"
                } else {
                    "微信/相机扫码直接读取本周真实数据"
                },
                color = TvColors.OnSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvColors.SurfaceContainerLowest)
                    .border(1.dp, TvColors.PrimaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Weekly Report QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            val mm = countdownSeconds / 60
            val ss = countdownSeconds % 60
            Text(
                text = String.format(Locale.US, "有效时间 : %02d:%02d 秒", mm, ss),
                color = TvColors.OnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun MemberContributionSwimlane(
    members: List<MemberUsageSummary>,
    activeProfileId: String,
    onSelectMemberProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = TvColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "家庭成员使用概览",
                    color = TvColors.OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvColors.SurfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "共 ${members.size} 位成员",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            members.take(4).forEach { member ->
                val isSelected = member.profile.id == activeProfileId
                val wh = member.weeklyUsedMinutes / 60
                val wm = member.weeklyUsedMinutes % 60
                val wQuotaHours = member.weeklyQuotaMinutes / 60
                val wRemMins = (member.weeklyQuotaMinutes - member.weeklyUsedMinutes).coerceAtLeast(0)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .tvClickable(
                            shapeRadius = 14.dp,
                            focusedScale = 1.035f,
                            unfocusedBorderColor = if (isSelected) TvColors.OutlineVariant.copy(alpha = 0.5f) else Color.Transparent,
                            unfocusedBorderWidth = if (isSelected) 1.dp else 0.dp
                        ) { onSelectMemberProfile(member.profile.id) }
                        .background(if (isSelected) TvColors.SurfaceContainerHigh else TvColors.SurfaceContainerLow)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) TvColors.PrimaryContainer else TvColors.SurfaceContainerHighest
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.profile.name.take(1),
                                    color = if (isSelected) TvColors.OnPrimaryContainer else TvColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = "${member.profile.name} (${member.profile.ageOrRoleTag})",
                                    color = TvColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (member.profile.isRestricted) "健康守护开启" else member.profile.subtitle,
                                    color = if (member.profile.isRestricted) TvColors.Secondary else TvColors.OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TvColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "本周使用",
                                color = TvColors.OnSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${wh}h ${wm}m",
                                color = if (isSelected) TvColors.Primary else TvColors.OnSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val frac = if (member.weeklyQuotaMinutes > 0) {
                            (member.weeklyUsedMinutes.toFloat() / member.weeklyQuotaMinutes).coerceIn(0.06f, 1f)
                        } else {
                            0.6f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(TvColors.SurfaceContainerLowest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(frac)
                                    .fillMaxHeight()
                                    .background(if (member.profile.isRestricted) TvColors.Secondary else TvColors.PrimaryContainer)
                            )
                        }

                        val subQuotaText = if (member.weeklyQuotaMinutes > 0) {
                            "周限额 ${wQuotaHours}h / 剩余 ${TvUsageRepository.formatMinutesShort(wRemMins)}"
                        } else {
                            "常用: ${member.topAppNames.joinToString(", ")}"
                        }
                        Text(
                            text = subQuotaText,
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "常看: ${member.topAppNames.joinToString(", ")}",
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (member.hasCurfewViolation) "有深夜记录" else "无宵禁违规",
                            color = if (member.hasCurfewViolation) TvColors.Error else TvColors.Secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

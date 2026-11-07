package com.tvpulse.tracker.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Weekend
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.CategoryAccessPolicy
import com.tvpulse.tracker.model.FamilyMemberProfile
import com.tvpulse.tracker.model.MemberUsageSummary
import com.tvpulse.tracker.model.SystemTelemetrySnapshot
import com.tvpulse.tracker.ui.components.AddFamilyMemberDialog
import com.tvpulse.tracker.ui.components.ChangePinDialog
import com.tvpulse.tracker.ui.components.tvClickable
import com.tvpulse.tracker.ui.components.tvFocusableCard
import com.tvpulse.tracker.ui.theme.TvColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun FamilyLimitsScreen(
    snapshot: SystemTelemetrySnapshot,
    currentPin: String,
    onSelectProfile: (String) -> Unit,
    onUpdateProfile: (FamilyMemberProfile) -> Unit,
    onAddProfile: (name: String, ageTag: String, isRestricted: Boolean, weekdayMin: Int, weekendMin: Int) -> Unit,
    onCycleCategoryPolicy: (AppCategory) -> Unit,
    onSaveNewPin: (String) -> Unit,
    onSimulateLockScreen: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = {
        scope.launch { scrollState.animateScrollTo(0) }
    }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val activeSummary = snapshot.memberSummaries.find { it.profile.id == snapshot.activeProfileId }
        ?: snapshot.memberSummaries.firstOrNull()
    val activeProfile = activeSummary?.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Top Profile Switcher Swimlane
        ProfileSwitcherSection(
            summaries = snapshot.memberSummaries,
            activeProfileId = snapshot.activeProfileId,
            onFocusedToTop = scrollToTop,
            onSelectProfile = onSelectProfile,
            onOpenAddMember = { showAddMemberDialog = true }
        )

        // Section 2: Active Profile Detail & Rule Matrix (7 cols + 5 cols)
        if (activeProfile != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                CurfewAndQuotaSettingsCard(
                    profile = activeProfile,
                    onUpdateProfile = onUpdateProfile,
                    modifier = Modifier.weight(7f)
                )

                AppWhitelistAndPinCard(
                    snapshot = snapshot,
                    onCycleCategoryPolicy = onCycleCategoryPolicy,
                    onOpenChangePin = { showChangePinDialog = true },
                    onSimulateLockScreen = onSimulateLockScreen,
                    modifier = Modifier.weight(5f)
                )
            }
        }

        // Section 3: Bottom Safety Status Strip
        SafetyStatusStrip(snapshot = snapshot)
    }

    if (showAddMemberDialog) {
        AddFamilyMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAdd = { name, ageTag, isRestricted, weekdayMin, weekendMin ->
                onAddProfile(name, ageTag, isRestricted, weekdayMin, weekendMin)
                showAddMemberDialog = false
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            currentPin = currentPin,
            onDismiss = { showChangePinDialog = false },
            onSaveNewPin = { newPin ->
                onSaveNewPin(newPin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
private fun ProfileSwitcherSection(
    summaries: List<MemberUsageSummary>,
    activeProfileId: String,
    onFocusedToTop: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenAddMember: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvColors.SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupervisedUserCircle,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "家庭成员防沉迷配额",
                        color = TvColors.OnSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "切换家庭档案，实时生效对应限额与护眼防沉迷策略",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TvColors.Secondary)
                )
                Text(
                    text = "健康模式实时生效中",
                    color = TvColors.Secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4-Card Horizontal Carousel (3 Member Cards + 1 Add New Member Card)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            summaries.take(3).forEach { summary ->
                val isSelected = summary.profile.id == activeProfileId
                ProfileCardItem(
                    summary = summary,
                    isSelected = isSelected,
                    onFocused = onFocusedToTop,
                    onClick = { onSelectProfile(summary.profile.id) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 4th Tile: Add New Family Member
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(148.dp)
                    .tvClickable(
                        shapeRadius = 16.dp,
                        focusedScale = 1.035f,
                        unfocusedBorderColor = TvColors.OutlineVariant.copy(alpha = 0.4f),
                        unfocusedBorderWidth = 1.dp,
                        onFocused = onFocusedToTop,
                        onClick = onOpenAddMember
                    )
                    .background(TvColors.SurfaceContainerLowest.copy(alpha = 0.7f))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TvColors.SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = TvColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "添加家庭成员",
                    color = TvColors.OnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "配置独立限额与护眼策略",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ProfileCardItem(
    summary: MemberUsageSummary,
    isSelected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = summary.profile
    val iconVec = when {
        profile.id.contains("kid") -> Icons.Default.ChildCare
        profile.id.contains("teen") -> Icons.Default.SportsEsports
        else -> Icons.Default.Movie
    }

    Column(
        modifier = modifier
            .height(148.dp)
            .tvClickable(
                shapeRadius = 16.dp,
                focusedScale = 1.035f,
                unfocusedBorderColor = if (isSelected) TvColors.OutlineVariant.copy(alpha = 0.5f) else Color.Transparent,
                unfocusedBorderWidth = if (isSelected) 1.dp else 0.dp,
                onFocused = onFocused,
                onClick = onClick
            )
            .background(if (isSelected) TvColors.SurfaceContainer else TvColors.SurfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) TvColors.PrimaryContainer else TvColors.SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVec,
                        contentDescription = null,
                        tint = if (isSelected) TvColors.OnPrimaryContainer else TvColors.OnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.name,
                            color = TvColors.OnSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(TvColors.SurfaceContainerHighest)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = profile.ageOrRoleTag,
                                color = if (isSelected) TvColors.Primary else TvColors.OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = profile.subtitle,
                        color = if (isSelected) TvColors.Primary else TvColors.OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (profile.isRestricted) TvColors.Secondary.copy(alpha = 0.15f)
                        else TvColors.SurfaceContainerHigh
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (profile.isRestricted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TvColors.Secondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = if (profile.isRestricted) "已受控" else "自由观影",
                        color = if (profile.isRestricted) TvColors.Secondary else TvColors.OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (profile.isRestricted) "今日剩余可用" else "今日电视时长",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp
                )
                if (profile.isRestricted && summary.todayQuotaMinutes > 0) {
                    val rem = summary.todayRemainingMinutes
                    val rh = rem / 60
                    val rm = rem % 60
                    val remText = if (rh > 0) "${rh}h ${rm}分" else "${rm}分钟"
                    Text(
                        text = remText,
                        color = if (isSelected) TvColors.Primary else TvColors.OnSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "无限制",
                        color = TvColors.OnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(summary.todayProgressFraction.coerceIn(0.08f, 1f))
                        .fillMaxHeight()
                        .background(
                            if (profile.isRestricted) {
                                Brush.horizontalGradient(listOf(TvColors.PrimaryContainer, TvColors.SecondaryContainer))
                            } else {
                                Brush.horizontalGradient(listOf(TvColors.TertiaryContainer, TvColors.TertiaryFixedDim))
                            }
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已用 ${summary.todayUsedMinutes} 分钟",
                    color = TvColors.Outline,
                    fontSize = 11.sp
                )
                val quotaLabel = if (profile.isRestricted && summary.todayQuotaMinutes > 0) {
                    String.format(Locale.US, "配额 %.1f 小时", summary.todayQuotaMinutes / 60f)
                } else {
                    "无强制休眠"
                }
                Text(
                    text = quotaLabel,
                    color = TvColors.Outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CurfewAndQuotaSettingsCard(
    profile: FamilyMemberProfile,
    onUpdateProfile: (FamilyMemberProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TvColors.SurfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvColors.ErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = TvColors.Error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "防沉迷与就寝锁屏时间",
                        color = TvColors.OnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "已选档案：${profile.name} (${profile.subtitle})",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TvColors.SecondaryContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = TvColors.Secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (profile.isRestricted) "守护中" else "自由模式",
                    color = TvColors.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Control 1: Weekday Limit Stepper
        QuotaStepperRow(
            icon = Icons.Default.CalendarToday,
            iconTint = TvColors.Primary,
            title = "工作日单日限额 (周一至五)",
            currentMinutes = profile.weekdayLimitMinutes,
            minMinutes = 30,
            maxMinutes = 240,
            barColor = TvColors.PrimaryContainer,
            minLabel = "30分钟 (严格)",
            maxLabel = "4小时 (放宽)",
            onChangeMinutes = { newMin ->
                onUpdateProfile(profile.copy(weekdayLimitMinutes = newMin, isRestricted = newMin > 0))
            }
        )

        // Control 2: Weekend Limit Stepper
        QuotaStepperRow(
            icon = Icons.Default.Weekend,
            iconTint = TvColors.TertiaryFixedDim,
            title = "周末节假日限额 (周六日)",
            currentMinutes = profile.weekendLimitMinutes,
            minMinutes = 60,
            maxMinutes = 300,
            barColor = TvColors.TertiaryContainer,
            minLabel = "1小时",
            maxLabel = "5小时",
            onChangeMinutes = { newMin ->
                onUpdateProfile(profile.copy(weekendLimitMinutes = newMin, isRestricted = newMin > 0))
            }
        )

        // Grid of 2 Toggles: Bedtime Curfew & Continuous Eye Rest
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Curfew Card (Clickable toggle)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .tvClickable(shapeRadius = 12.dp, focusedScale = 1.03f) {
                        onUpdateProfile(profile.copy(curfewEnabled = !profile.curfewEnabled))
                    }
                    .background(TvColors.SurfaceContainerLow)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = TvColors.Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "就寝强制熄屏",
                            color = TvColors.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (profile.curfewEnabled) TvColors.ErrorContainer else TvColors.SurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (profile.curfewEnabled) "已启用" else "已关闭",
                            color = if (profile.curfewEnabled) TvColors.OnErrorContainer else TvColors.OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(
                        text = String.format(
                            Locale.US,
                            "%02d:00 ~ 次日 %02d:00",
                            profile.curfewStartHour,
                            profile.curfewEndHour
                        ),
                        color = if (profile.curfewEnabled) TvColors.Error else TvColors.OnSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "到达时间触发黑屏锁，需4位PIN解锁 (点击切换)",
                        color = TvColors.Outline,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Eye Relax Interval Card (Clickable toggle / cycle interval)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .tvClickable(shapeRadius = 12.dp, focusedScale = 1.03f) {
                        val nextInterval = when (profile.eyeRestIntervalMinutes) {
                            30 -> 40
                            40 -> 45
                            45 -> 60
                            else -> 30
                        }
                        onUpdateProfile(profile.copy(eyeRestIntervalMinutes = nextInterval, eyeRestEnabled = true))
                    }
                    .background(TvColors.SurfaceContainerLow)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TvColors.Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "连续观看强制护眼",
                            color = TvColors.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TvColors.SecondaryContainer.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "已开启",
                            color = TvColors.Secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(
                        text = "每 ${profile.eyeRestIntervalMinutes} 分钟 / 歇 ${profile.eyeRestDurationMinutes} 分",
                        color = TvColors.Secondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "连续观看达标弹出屏显护眼引导 (点击切换间隔)",
                        color = TvColors.Outline,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotaStepperRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    currentMinutes: Int,
    minMinutes: Int,
    maxMinutes: Int,
    barColor: Color,
    minLabel: String,
    maxLabel: String,
    onChangeMinutes: (Int) -> Unit
) {
    val displayMinutes = if (currentMinutes <= 0) maxMinutes else currentMinutes
    val hours = displayMinutes / 60
    val mins = displayMinutes % 60
    val valueLabel = if (currentMinutes <= 0) "不限额" else "${hours}小时 ${mins}分"
    val progress = ((displayMinutes - minMinutes).toFloat() / (maxMinutes - minMinutes).coerceAtLeast(1))
        .coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TvColors.SurfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .tvClickable(shapeRadius = 6.dp, focusedScale = 1.08f) {
                            val next = (displayMinutes - 30).coerceAtLeast(minMinutes)
                            onChangeMinutes(next)
                        }
                        .background(TvColors.SurfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "-30m",
                        color = TvColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = valueLabel,
                    color = iconTint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                Box(
                    modifier = Modifier
                        .tvClickable(shapeRadius = 6.dp, focusedScale = 1.08f) {
                            val next = (displayMinutes + 30).coerceAtMost(maxMinutes)
                            onChangeMinutes(next)
                        }
                        .background(TvColors.SurfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "+30m",
                        color = TvColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TvColors.SurfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(barColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = minLabel, color = TvColors.Outline, fontSize = 11.sp)
            Text(text = "当前配额: ${displayMinutes}分钟", color = TvColors.Outline, fontSize = 11.sp)
            Text(text = maxLabel, color = TvColors.Outline, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AppWhitelistAndPinCard(
    snapshot: SystemTelemetrySnapshot,
    onCycleCategoryPolicy: (AppCategory) -> Unit,
    onOpenChangePin: () -> Unit,
    onSimulateLockScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val policies = snapshot.categoryPolicies

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TvColors.SurfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvColors.PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = TvColors.OnPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "应用权限与内容锁",
                        color = TvColors.OnSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "点击任意行切换权限策略 (允许 / 无限额 / 密码锁 / 封禁)",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 4 Category Rule Rows with real installed app names per category
        val kidsApps = snapshot.rankedApps.filter { it.category == AppCategory.KIDS }.map { it.appName }
        val eduApps = snapshot.rankedApps.filter { it.category == AppCategory.EDUCATION }.map { it.appName }
        val gameApps = snapshot.rankedApps.filter {
            it.category == AppCategory.GAME_FITNESS || it.category == AppCategory.SYSTEM_CAST
        }.map { it.appName }
        val videoApps = snapshot.rankedApps.filter { it.category == AppCategory.VIDEO }.map { it.appName }

        CategoryPolicyRow(
            icon = Icons.Default.SmartDisplay,
            iconTint = TvColors.Secondary,
            title = "少儿动画专区 (${kidsApps.size}款)",
            subtitle = if (kidsApps.isNotEmpty()) kidsApps.take(3).joinToString(" / ") else "本机暂未安装少儿专区应用",
            policy = policies[AppCategory.KIDS] ?: CategoryAccessPolicy.ALLOW,
            onClick = { onCycleCategoryPolicy(AppCategory.KIDS) }
        )

        CategoryPolicyRow(
            icon = Icons.Default.School,
            iconTint = TvColors.Primary,
            title = "网课与知识 (${eduApps.size}款)",
            subtitle = if (eduApps.isNotEmpty()) "免计入限额 · ${eduApps.take(3).joinToString(" / ")}" else "免计入娱乐限额 · 绿色通道",
            policy = policies[AppCategory.EDUCATION] ?: CategoryAccessPolicy.UNLIMITED,
            onClick = { onCycleCategoryPolicy(AppCategory.EDUCATION) }
        )

        CategoryPolicyRow(
            icon = Icons.Default.CastConnected,
            iconTint = TvColors.TertiaryFixedDim,
            title = "游戏与系统投屏 (${gameApps.size}款)",
            subtitle = if (gameApps.isNotEmpty()) "需PIN锁 · ${gameApps.take(3).joinToString(" / ")}" else "启动需输入 4 位家长安全 PIN",
            policy = policies[AppCategory.GAME_FITNESS] ?: CategoryAccessPolicy.PIN_LOCK,
            onClick = { onCycleCategoryPolicy(AppCategory.GAME_FITNESS) }
        )

        CategoryPolicyRow(
            icon = Icons.Default.Block,
            iconTint = TvColors.Error,
            title = "影视剧场应用 (${videoApps.size}款)",
            subtitle = if (videoApps.isNotEmpty()) videoApps.take(3).joinToString(" / ") else "受每日总配额与晚间宵禁管控",
            policy = policies[AppCategory.VIDEO] ?: CategoryAccessPolicy.ALLOW,
            onClick = { onCycleCategoryPolicy(AppCategory.VIDEO) }
        )

        // Bottom Action Buttons: Change PIN & Simulate Lock Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .tvClickable(shapeRadius = 12.dp, focusedScale = 1.035f) { onOpenChangePin() }
                    .background(TvColors.SurfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = null,
                    tint = TvColors.OnSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "修改家长 PIN",
                    color = TvColors.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .tvClickable(shapeRadius = 12.dp, focusedScale = 1.035f) { onSimulateLockScreen() }
                    .background(TvColors.PrimaryContainer)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Preview,
                    contentDescription = null,
                    tint = TvColors.OnPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "模拟锁屏效果",
                    color = TvColors.OnPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun CategoryPolicyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    policy: CategoryAccessPolicy,
    onClick: () -> Unit
) {
    val (badgeBg, badgeFg, badgeIcon) = when (policy) {
        CategoryAccessPolicy.ALLOW -> Triple(
            TvColors.Secondary.copy(alpha = 0.15f),
            TvColors.Secondary,
            Icons.Default.CheckCircle
        )
        CategoryAccessPolicy.UNLIMITED -> Triple(
            TvColors.PrimaryContainer,
            TvColors.OnPrimaryContainer,
            Icons.Default.LockOpen
        )
        CategoryAccessPolicy.PIN_LOCK -> Triple(
            TvColors.TertiaryContainer,
            TvColors.OnTertiaryContainer,
            Icons.Default.Pin
        )
        CategoryAccessPolicy.BLOCKED -> Triple(
            TvColors.ErrorContainer,
            TvColors.OnErrorContainer,
            Icons.Default.DoNotDisturbOn
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(shapeRadius = 12.dp, focusedScale = 1.025f) { onClick() }
            .background(TvColors.SurfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvColors.SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TvColors.Outline,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeFg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = policy.label,
                color = badgeFg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SafetyStatusStrip(snapshot: SystemTelemetrySnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusableCard(shapeRadius = 14.dp, focusedScale = 1.015f)
            .background(TvColors.SurfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = TvColors.Secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (snapshot.hasUsagePermission) "防沉迷实时监测：已激活" else "防沉迷监测：待授权",
                    color = TvColors.Secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(TvColors.OutlineVariant)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsRemote,
                    contentDescription = null,
                    tint = TvColors.OnSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "锁屏状态按键拦截：需 4 位家长 PIN 解锁",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(TvColors.OutlineVariant)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (snapshot.hasCameraHardware) TvColors.Secondary else TvColors.OnSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = snapshot.cameraStatusText,
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

package com.tvpulse.tracker.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ConnectedTv
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.AppUsageInfo
import com.tvpulse.tracker.model.NavTab
import com.tvpulse.tracker.ui.theme.TvColors

/**
 * Unified TV D-Pad remote focus + click modifier:
 * - Single focus node (never chains both .focusable() and .clickable())
 * - Smooth scale animation on D-Pad focus
 * - Low-opacity white/cyan mask overlay + glowing border when focused
 * - Optional [onFocused] callback to support scrolling to top when top-row items receive D-Pad focus
 */
fun Modifier.tvClickable(
    shapeRadius: Dp = 12.dp,
    focusedScale: Float = 1.038f,
    maskAlpha: Float = 0.14f,
    focusedBorderColor: Color = Color.White,
    unfocusedBorderColor: Color = Color.Transparent,
    unfocusedBorderWidth: Dp = 0.dp,
    onFocused: () -> Unit = {},
    onClick: () -> Unit
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        label = "tvFocusScale"
    )
    val shape = RoundedCornerShape(shapeRadius)
    this
        .zIndex(if (isFocused) 2f else 0f)
        .scale(scale)
        .clip(shape)
        .border(
            width = if (isFocused) 2.5.dp else unfocusedBorderWidth,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .drawWithContent {
            drawContent()
            if (isFocused) {
                drawRoundRect(
                    color = TvColors.PrimaryContainer.copy(alpha = 0.12f),
                    cornerRadius = CornerRadius(shapeRadius.toPx(), shapeRadius.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = maskAlpha),
                    cornerRadius = CornerRadius(shapeRadius.toPx(), shapeRadius.toPx())
                )
            }
        }
        .onFocusChanged { state ->
            isFocused = state.isFocused
            if (state.isFocused) {
                onFocused()
            }
        }
        .clickable { onClick() }
}

/**
 * TV D-Pad focusable card modifier for informational cards (such as top Hero cards)
 * so D-Pad Up/Down/Left/Right can navigate predictably across the 2D grid and trigger [onFocused].
 */
fun Modifier.tvFocusableCard(
    shapeRadius: Dp = 14.dp,
    focusedScale: Float = 1.025f,
    maskAlpha: Float = 0.12f,
    onFocused: () -> Unit = {},
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        label = "tvCardFocusScale"
    )
    val shape = RoundedCornerShape(shapeRadius)
    val base = this
        .zIndex(if (isFocused) 2f else 0f)
        .scale(scale)
        .clip(shape)
        .border(
            width = if (isFocused) 2.5.dp else 0.dp,
            color = if (isFocused) Color.White else Color.Transparent,
            shape = shape
        )
        .drawWithContent {
            drawContent()
            if (isFocused) {
                drawRoundRect(
                    color = TvColors.PrimaryContainer.copy(alpha = 0.10f),
                    cornerRadius = CornerRadius(shapeRadius.toPx(), shapeRadius.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = maskAlpha),
                    cornerRadius = CornerRadius(shapeRadius.toPx(), shapeRadius.toPx())
                )
            }
        }
        .onFocusChanged { state ->
            isFocused = state.isFocused
            if (state.isFocused) {
                onFocused()
            }
        }
    if (onClick != null) {
        base.clickable { onClick() }
    } else {
        base.focusable()
    }
}

@Composable
fun TvPulseBrandLogo(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(TvColors.SurfaceContainerLowest)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(TvColors.PrimaryContainer, TvColors.Secondary)),
                shape = RoundedCornerShape(size * 0.26f)
            )
    ) {
        val w = this.size.width
        val h = this.size.height

        // TV Screen Bezel (16:9 inside icon)
        val tvLeft = w * 0.14f
        val tvTop = h * 0.20f
        val tvWidth = w * 0.72f
        val tvHeight = h * 0.48f
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(TvColors.PrimaryContainer, TvColors.Secondary)
            ),
            topLeft = Offset(tvLeft, tvTop),
            size = Size(tvWidth, tvHeight),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.055f)
        )

        // Pulse ECG Waveform inside TV screen
        val pulsePath = Path().apply {
            moveTo(w * 0.21f, h * 0.46f)
            lineTo(w * 0.34f, h * 0.46f)
            lineTo(w * 0.41f, h * 0.36f)
            lineTo(w * 0.48f, h * 0.56f)
            lineTo(w * 0.56f, h * 0.29f)
            lineTo(w * 0.64f, h * 0.51f)
            lineTo(w * 0.70f, h * 0.42f)
            lineTo(w * 0.79f, h * 0.46f)
        }
        drawPath(
            path = pulsePath,
            brush = Brush.horizontalGradient(
                listOf(TvColors.PrimaryContainer, TvColors.Primary, TvColors.Secondary)
            ),
            style = Stroke(
                width = w * 0.06f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Peak Dot
        drawCircle(
            color = TvColors.Secondary,
            radius = w * 0.045f,
            center = Offset(w * 0.56f, h * 0.29f)
        )

        // TV Stand Base
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(TvColors.PrimaryContainer, TvColors.Secondary)),
            topLeft = Offset(w * 0.34f, h * 0.76f),
            size = Size(w * 0.32f, h * 0.065f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )
    }
}

@Composable
fun TvSidebarNavigation(
    currentTab: NavTab,
    deviceModelTag: String,
    hasUsagePermission: Boolean,
    onSelectTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(192.dp)
            .fillMaxHeight()
            .background(TvColors.SurfaceContainerLowest.copy(alpha = 0.95f))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Brand Logo Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                TvPulseBrandLogo(size = 36.dp)
                Column {
                    Text(
                        text = "TV Pulse",
                        color = TvColors.Primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "电视时钟",
                        color = TvColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Navigation Items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isFocused) 1.04f else if (isSelected) 1.01f else 1f,
                        label = "navScale"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> TvColors.PrimaryContainer
                            isFocused -> TvColors.SurfaceContainerHigh
                            else -> Color.Transparent
                        },
                        label = "navBg"
                    )
                    val contentColor = when {
                        isSelected -> TvColors.OnPrimaryContainer
                        isFocused -> TvColors.Primary
                        else -> TvColors.OnSurfaceVariant
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isFocused) 1f else 0f)
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) {
                                    if (isSelected) Color.White.copy(alpha = 0.85f) else TvColors.PrimaryContainer
                                } else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .drawWithContent {
                                drawContent()
                                if (isFocused) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.12f),
                                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                    )
                                }
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Icon(
                            imageVector = tabIcon(tab),
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = tab.label,
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom Device & Telemetry Badge
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!hasUsagePermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvClickable(shapeRadius = 8.dp) { onSelectTab(NavTab.SETTINGS) }
                        .background(TvColors.ErrorContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TvColors.Error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "点击开启统计权限",
                        color = TvColors.OnErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvColors.SurfaceContainerLow)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ConnectedTv,
                    contentDescription = null,
                    tint = TvColors.Secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = deviceModelTag,
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun tabIcon(tab: NavTab): ImageVector = when (tab) {
    NavTab.DASHBOARD -> Icons.Default.Dashboard
    NavTab.APPS -> Icons.Default.GridView
    NavTab.FAMILY_LIMITS -> Icons.Default.Security
    NavTab.REPORTS -> Icons.Default.Insights
    NavTab.SETTINGS -> Icons.Default.Settings
}

@Composable
fun TvTopHeader(
    networkLabel: String,
    isNetworkOnline: Boolean,
    currentTimeText: String,
    activeProfileName: String,
    isBlueLightActive: Boolean,
    onToggleBlueLight: () -> Unit,
    onSwitchProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(TvColors.Surface.copy(alpha = 0.9f))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvPulseBrandLogo(size = 28.dp)
            Text(
                text = "TV Pulse",
                color = TvColors.OnSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isBlueLightActive) {
                Box(
                    modifier = Modifier
                        .tvClickable(shapeRadius = 999.dp, focusedScale = 1.05f) { onToggleBlueLight() }
                        .background(TvColors.TertiaryContainer.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "暖色护眼滤镜已开启",
                        color = TvColors.TertiaryFixedDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Real Wi-Fi / 5G Network Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (isNetworkOnline) TvColors.Secondary else TvColors.Error,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = networkLabel,
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Real System Clock
            Text(
                text = currentTimeText,
                color = TvColors.Primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )

            // Active Family Profile Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .tvClickable(shapeRadius = 999.dp, focusedScale = 1.05f) { onSwitchProfileClick() }
                    .background(TvColors.SurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(TvColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TvColors.OnPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = activeProfileName,
                    color = TvColors.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun AppIconBox(
    drawable: Drawable?,
    category: AppCategory,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp
) {
    val imageBitmap: ImageBitmap? = remember(drawable) {
        drawable?.toImageBitmapOrNull()
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else {
        val (iconVec, tintColor) = when (category) {
            AppCategory.VIDEO -> Icons.Default.Movie to TvColors.PrimaryContainer
            AppCategory.KIDS -> Icons.Default.ChildCare to TvColors.TertiaryFixedDim
            AppCategory.EDUCATION -> Icons.Default.School to TvColors.Secondary
            AppCategory.GAME_FITNESS -> Icons.Default.SportsEsports to TvColors.Secondary
            AppCategory.SYSTEM_CAST, AppCategory.ALL -> Icons.Default.SmartDisplay to TvColors.Primary
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(TvColors.SurfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVec,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

private fun Drawable.toImageBitmapOrNull(): ImageBitmap? {
    return try {
        if (this is BitmapDrawable && this.bitmap != null) {
            return this.bitmap.asImageBitmap()
        }
        val w = intrinsicWidth.coerceAtLeast(64).coerceAtMost(192)
        val h = intrinsicHeight.coerceAtLeast(64).coerceAtMost(192)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bmp.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/**
 * Full-screen Ambient Black Screen Overlay ("客厅静默模式 · 熄屏伴听")
 */
@Composable
fun SilentAmbientScreenOverlay(
    currentTimeText: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Nightlight,
                contentDescription = null,
                tint = TvColors.Primary.copy(alpha = 0.35f),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = currentTimeText,
                color = TvColors.OnSurface.copy(alpha = 0.28f),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "客厅静默模式（熄屏伴听中） · 轻按遥控器任意键或点击屏幕恢复亮屏",
                color = TvColors.OnSurfaceVariant.copy(alpha = 0.35f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Full-screen Curfew / Quota Lock Screen with 4-digit Parental PIN verification
 */
@Composable
fun ParentalPinLockScreenOverlay(
    reasonTitle: String,
    reasonSubtitle: String,
    currentTimeText: String,
    expectedPin: String,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5070A10))
            .clickable(enabled = true) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(TvColors.SurfaceContainer)
                .border(1.5.dp, TvColors.PrimaryContainer.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TvColors.ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TvColors.Error,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = reasonTitle,
                color = TvColors.OnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = reasonSubtitle,
                color = TvColors.OnSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                text = "当前时间 $currentTimeText · 请输入 4 位家长安全 PIN 码解锁 (默认 0000)",
                color = TvColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (filled) TvColors.PrimaryContainer.copy(alpha = 0.2f) else TvColors.SurfaceContainerLowest)
                            .border(
                                1.5.dp,
                                if (filled) TvColors.PrimaryContainer else TvColors.OutlineVariant,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filled) "●" else "",
                            color = TvColors.PrimaryContainer,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = TvColors.Error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Keypad Grid (1..9, Clear, 0, Unlock)
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("清除", "0", "解锁")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowKeys.forEach { key ->
                            Button(
                                onClick = {
                                    errorMessage = null
                                    when (key) {
                                        "清除" -> enteredPin = ""
                                        "解锁" -> {
                                            if (enteredPin == expectedPin) {
                                                onUnlocked()
                                            } else {
                                                errorMessage = "PIN 码不正确，请重试"
                                                enteredPin = ""
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == expectedPin) {
                                                        onUnlocked()
                                                    } else {
                                                        errorMessage = "PIN 码不正确，请重试"
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (key == "解锁") TvColors.PrimaryContainer else TvColors.SurfaceContainerHigh,
                                    contentColor = if (key == "解锁") TvColors.OnPrimaryContainer else TvColors.OnSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to set per-app daily limit
 */
@Composable
fun AppLimitConfigDialog(
    app: AppUsageInfo,
    onDismiss: () -> Unit,
    onSaveLimit: (Int) -> Unit
) {
    val presetMinutes = listOf(0, 30, 45, 60, 90, 120, 180)
    var selectedMin by remember { mutableIntStateOf(app.dailyLimitMinutes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TvColors.SurfaceContainer,
            border = BorderStroke(1.dp, TvColors.OutlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "设置应用每日限额 · ${app.appName}",
                    color = TvColors.OnSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "今日已观看 ${app.todayTimeMs / 60_000L} 分钟。到达设定限额后将触发防沉迷锁屏。",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetMinutes.chunked(4).forEach { chunk ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunk.forEach { min ->
                                val isSelected = selectedMin == min
                                val label = if (min == 0) "不限额" else "${min}分钟"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .tvClickable(shapeRadius = 10.dp) { selectedMin = min }
                                        .background(
                                            if (isSelected) TvColors.PrimaryContainer else TvColors.SurfaceContainerHigh
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) TvColors.OnPrimaryContainer else TvColors.OnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.SurfaceContainerHigh,
                            contentColor = TvColors.OnSurface
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSaveLimit(selectedMin) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.PrimaryContainer,
                            contentColor = TvColors.OnPrimaryContainer
                        )
                    ) {
                        Text("保存生效", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog showing 24h hourly breakdown for a single app
 */
@Composable
fun AppHourlyDetailDialog(
    app: AppUsageInfo,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TvColors.SurfaceContainer,
            border = BorderStroke(1.dp, TvColors.OutlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .width(520.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "${app.appName} · 今日分时明细",
                            color = TvColors.OnSurface,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "包名: ${app.packageName} · 今日累计 ${app.todayTimeMs / 60_000L} 分钟",
                            color = TvColors.OnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = TvColors.PrimaryContainer
                    )
                }

                val maxMin = (app.hourlyMinutesToday.maxOrNull() ?: 1).coerceAtLeast(15)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvColors.SurfaceContainerLowest)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    app.hourlyMinutesToday.forEachIndexed { hr, mins ->
                        val frac = (mins.toFloat() / maxMin).coerceIn(0.04f, 1f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        if (mins > 0) TvColors.PrimaryContainer else TvColors.SurfaceContainerHighest
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (hr % 2 == 0) {
                                Text(
                                    text = String.format("%02d", hr),
                                    color = TvColors.OnSurfaceVariant,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "今日启动次数: ${app.launchCount} 次",
                        color = TvColors.Secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.PrimaryContainer,
                            contentColor = TvColors.OnPrimaryContainer
                        )
                    ) {
                        Text("关闭", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to add a new family member profile
 */
@Composable
fun AddFamilyMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, ageTag: String, isRestricted: Boolean, weekdayMin: Int, weekendMin: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ageTag by remember { mutableStateOf("9岁") }
    var isRestricted by remember { mutableStateOf(true) }
    var weekdayMin by remember { mutableIntStateOf(90) }
    var weekendMin by remember { mutableIntStateOf(150) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TvColors.SurfaceContainer,
            border = BorderStroke(1.dp, TvColors.OutlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .width(440.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "添加家庭成员档案",
                    color = TvColors.OnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("成员称呼 (如: 二宝 / 爷爷 / 奶奶)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageTag,
                    onValueChange = { ageTag = it },
                    label = { Text("年龄或身份标签 (如: 8岁 / 长辈)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .tvClickable(shapeRadius = 10.dp) { isRestricted = true }
                            .background(if (isRestricted) TvColors.PrimaryContainer else TvColors.SurfaceContainerHigh)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "儿童/防沉迷受控",
                            color = if (isRestricted) TvColors.OnPrimaryContainer else TvColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .tvClickable(shapeRadius = 10.dp) { isRestricted = false }
                            .background(if (!isRestricted) TvColors.PrimaryContainer else TvColors.SurfaceContainerHigh)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "成人/自由观影",
                            color = if (!isRestricted) TvColors.OnPrimaryContainer else TvColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.SurfaceContainerHigh,
                            contentColor = TvColors.OnSurface
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onAdd(
                                name.ifBlank { "二宝" },
                                ageTag.ifBlank { "儿童" },
                                isRestricted,
                                if (isRestricted) weekdayMin else 0,
                                if (isRestricted) weekendMin else 0
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.PrimaryContainer,
                            contentColor = TvColors.OnPrimaryContainer
                        )
                    ) {
                        Text("确认新增", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to modify the 4-digit Parental PIN
 */
@Composable
fun ChangePinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSaveNewPin: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TvColors.SurfaceContainer,
            border = BorderStroke(1.dp, TvColors.OutlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .width(380.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "修改家长安全 PIN 码",
                    color = TvColors.OnSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "当前 PIN 码: $currentPin · 请输入新的 4 位数字密码",
                    color = TvColors.OnSurfaceVariant,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            newPin = input
                        }
                    },
                    label = { Text("新 4 位数字 PIN 码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (msg != null) {
                    Text(text = msg!!, color = TvColors.Error, fontSize = 12.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.SurfaceContainerHigh,
                            contentColor = TvColors.OnSurface
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (newPin.length == 4) {
                                onSaveNewPin(newPin)
                            } else {
                                msg = "请输入完整的 4 位数字"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvColors.PrimaryContainer,
                            contentColor = TvColors.OnPrimaryContainer
                        )
                    ) {
                        Text("保存 PIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

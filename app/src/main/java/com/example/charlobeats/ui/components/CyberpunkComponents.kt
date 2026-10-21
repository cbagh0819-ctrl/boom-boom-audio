package com.example.charlobeats.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.charlobeats.audio.model.VisualizerData
import com.example.charlobeats.ui.navigation.Screen
import com.example.charlobeats.ui.theme.*
import kotlin.math.sin

/**
 * Top Navbar matching the exact header across all 4 mockup screenshots.
 * Left: Glowing Logo + BOOM-BOOM
 * Right: Nav Pills [Home, Editor, Presets, Saved]
 */
@Composable
fun BoomBoomTopBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCanvas)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + App Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { navController.navigate(Screen.Home.route) }
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(DarkCardBgElevated, RoundedCornerShape(8.dp))
                    .border(1.dp, PurpleBrand.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PurpleNeon,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "BOOM",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextWhite
            )
        }

        // Navigation Pills
        val navigateTo: (String) -> Unit = { route ->
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavPill(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentRoute == Screen.Home.route,
                onClick = { navigateTo(Screen.Home.route) }
            )
            NavPill(
                label = "Editor",
                icon = Icons.Default.Tune,
                selected = currentRoute == Screen.AudioEditor.route,
                onClick = { navigateTo(Screen.AudioEditor.route) }
            )
            NavPill(
                label = "Presets",
                icon = Icons.Default.MusicNote,
                selected = currentRoute == Screen.Presets.route,
                onClick = { navigateTo(Screen.Presets.route) }
            )
            NavPill(
                label = "Saved",
                icon = Icons.Default.Folder,
                selected = currentRoute == Screen.SavedSongs.route,
                onClick = { navigateTo(Screen.SavedSongs.route) }
            )
        }
    }
}

@Composable
fun NavPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) PurpleBrand.copy(alpha = 0.45f)
                else Color.Transparent
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) PurpleBrand.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) TextWhite else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextWhite else TextMuted,
                maxLines = 1
            )
        }
    }
}

/**
 * Dashed Dropzone matching Image 1.
 */
@Composable
fun DashedUploadBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCardBg)
            .drawBehind {
                val stroke = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
                drawRoundRect(
                    color = Color(0xFF332A55),
                    style = stroke,
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(vertical = 36.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(DarkCardBgElevated, CircleShape)
                    .border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = PurpleNeon,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "Drop your audio file here",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = "Supports MP3, WAV, OGG, FLAC",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}

/**
 * Effect Slider Card matching Image 2.
 * Left icon + title, right value display, sleek purple slider.
 */
@Composable
fun EffectSliderCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueDisplay: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    sliderColor: Color = PurpleBrand
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                }

                Text(
                    text = valueDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = sliderColor
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = sliderColor,
                    activeTrackColor = sliderColor,
                    inactiveTrackColor = DarkCardBgElevated
                ),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            )
        }
    }
}

/**
 * Animated Spectrum Visualizer matching Image 2.
 */
@Composable
fun ModernWaveformVisualizer(
    visualizerData: VisualizerData,
    modifier: Modifier = Modifier,
    barCount: Int = 40
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = 3.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (width - totalSpacing) / barCount

            val isPlaying = visualizerData.isPlaying
            val bands = visualizerData.bands

            for (i in 0 until barCount) {
                val bandValue = if (isPlaying && bands.isNotEmpty()) {
                    val bandIndex = (i * bands.size / barCount).coerceIn(0, bands.size - 1)
                    bands[bandIndex].coerceIn(0.06f, 1.0f)
                } else {
                    ((sin(idlePhase + i * 0.28f) + 1.0f) * 0.16f + 0.06f).toFloat()
                }

                val barHeight = (bandValue * height * 0.95f).coerceAtLeast(4.dp.toPx())
                val x = i * (barWidth + spacing)
                val y = height - barHeight

                val fraction = i.toFloat() / barCount
                val color = when {
                    fraction < 0.40f -> PurpleBrand
                    fraction < 0.75f -> CyanAccent
                    else -> CyanNeon
                }

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.25f)),
                        startY = y,
                        endY = height
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Feature card used on the Home screen grid.
 */
@Composable
fun FeatureCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Glowing Gradient Button used for main CTAs (e.g. "Get Started →", "✨ Auto Enhance").
 */
@Composable
fun GlowGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    gradient: List<Color> = listOf(PurpleBrand, PinkNeon),
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    if (enabled) gradient
                    else listOf(Color(0xFF374151), Color(0xFF1F2937))
                )
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
    }
}

/**
 * Small outlined action button (e.g. [↑ Change], [↺ Reset All], [Export WAV], etc.)
 */
@Composable
fun MiniActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PurpleNeon,
    backgroundColor: Color = DarkCardBgElevated
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
        }
    }
}

/**
 * Cyberpunk badge / tag.
 */
@Composable
fun CyberBadge(
    text: String,
    color: Color = CyanNeon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.8.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}


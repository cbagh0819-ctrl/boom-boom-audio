package com.example.charlobeats.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.charlobeats.audio.player.BoomAudioEngine
import com.example.charlobeats.audio.preset.AudioPreset
import com.example.charlobeats.audio.preset.AudioPresetRepository
import com.example.charlobeats.ui.components.*
import com.example.charlobeats.ui.navigation.Screen
import com.example.charlobeats.ui.theme.*

@Composable
fun PresetSelectionScreen(
    navController: NavController,
    audioEngine: BoomAudioEngine
) {
    val effectState by audioEngine.effectState.collectAsState()
    val isPlaying by audioEngine.isPlaying.collectAsState()
    var appliedNotification by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Mockup Exact Top Bar
        BoomBoomTopBar(
            currentRoute = Screen.Presets.route,
            navController = navController
        )

        // Subheader matching Image 3: PRESETS + "Professional audio presets for every scenario" + [← Back to Editor]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PRESETS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextWhite
                )
                Text(
                    text = "Professional audio presets for every scenario",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            MiniActionButton(
                text = "Back to Editor",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                accentColor = CyanNeon,
                onClick = { navController.navigate(Screen.AudioEditor.route) }
            )
        }

        // Applied notification banner
        AnimatedVisibility(
            visible = appliedNotification != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanNeon.copy(alpha = 0.15f))
                    .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = appliedNotification ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanNeon
                )
            }
        }

        // 7 Preset Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AudioPresetRepository.presets) { preset ->
                val isActive = effectState.activePresetName == preset.title

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) DarkCardBgElevated else DarkCardBg)
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) PurpleBrand else CardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .then(
                            if (isActive) Modifier.shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = PurpleBrand, spotColor = PurpleNeon)
                            else Modifier
                        )
                        .clickable {
                            audioEngine.updateEffectState(preset.effectState)
                            if (!isPlaying) {
                                audioEngine.play()
                            }
                            appliedNotification = "✓ ${preset.title} Applied"
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Squircle Icon Container
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isActive) PurpleBrand.copy(alpha = 0.35f) else DarkCardBgElevated,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isActive) PurpleBrand else CardBorder,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = if (isActive) PurpleNeon else CyanNeon,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title + Description
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = preset.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )

                                if (isActive) {
                                    CyberBadge(text = "ACTIVE", color = CyanNeon)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = preset.description,
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }
}

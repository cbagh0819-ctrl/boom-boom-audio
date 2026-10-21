package com.example.charlobeats.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.charlobeats.audio.player.BoomAudioEngine
import com.example.charlobeats.ui.components.*
import com.example.charlobeats.ui.navigation.Screen
import com.example.charlobeats.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    audioEngine: BoomAudioEngine
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported Audio Track"
            audioEngine.playUri(uri, fileName)
            navController.navigate(Screen.AudioEditor.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Mockup Exact Top Bar
        BoomBoomTopBar(
            currentRoute = Screen.Home.route,
            navController = navController
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Section matching Image 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                // Purple Glowing App Icon
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(DarkCardBgElevated, RoundedCornerShape(16.dp))
                        .border(1.5.dp, PurpleBrand, RoundedCornerShape(16.dp))
                        .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = PurpleBrand, spotColor = PurpleNeon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PurpleNeon,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title
                Text(
                    text = "BOOM-BOOM",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextWhite
                )

                // Subtitle Badge
                Box(
                    modifier = Modifier
                        .background(CyanNeon.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AUDIO ENHANCER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = CyanNeon
                    )
                }

                // Description
                Text(
                    text = "Next-generation cyberpunk audio processing. Real-time effects, 8D audio simulation, and AI-powered enhancement right on your device.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Dashed Dropzone Box
            DashedUploadBox(
                onClick = { filePickerLauncher.launch("audio/*") }
            )

            // 4 Feature Cards (2x2 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.Bolt,
                        iconTint = CyanNeon,
                        iconBg = CyanGlow,
                        title = "Real-Time Effects",
                        description = "10 DSP effects with zero latency preview",
                        modifier = Modifier.weight(1f)
                    )

                    FeatureCard(
                        icon = Icons.Default.Equalizer,
                        iconTint = PurpleNeon,
                        iconBg = PurpleGlow,
                        title = "DJ Presets",
                        description = "7 handcrafted presets for every scenario",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.Headphones,
                        iconTint = CyanAccent,
                        iconBg = CyanGlow,
                        title = "Offline Processing",
                        description = "All audio processed locally on your device",
                        modifier = Modifier.weight(1f)
                    )

                    FeatureCard(
                        icon = Icons.Default.Download,
                        iconTint = PinkNeon,
                        iconBg = PinkGlow,
                        title = "Export & Save",
                        description = "Export in MP3, WAV, or 24-bit FLAC",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom CTA "Get Started →"
            GlowGradientButton(
                text = "Get Started →",
                onClick = {
                    if (audioEngine.exoPlayer.mediaItemCount == 0) {
                        audioEngine.loadAndPlayDemoTrack()
                    }
                    navController.navigate(Screen.AudioEditor.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp).navigationBarsPadding())
        }
    }
}

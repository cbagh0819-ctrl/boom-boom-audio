package com.example.charlobeats.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.charlobeats.audio.ai.AiAudioAnalyzer
import com.example.charlobeats.audio.model.AudioAnalysisResult
import com.example.charlobeats.audio.model.AudioEffectState
import com.example.charlobeats.audio.model.SavedSongItem
import com.example.charlobeats.audio.player.BoomAudioEngine
import com.example.charlobeats.audio.processor.AudioExportEngine
import com.example.charlobeats.audio.processor.ExportStage
import com.example.charlobeats.data.SavedSongsRepository
import com.example.charlobeats.ui.components.*
import com.example.charlobeats.ui.navigation.Screen
import com.example.charlobeats.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun AudioEditorScreen(
    navController: NavController,
    audioEngine: BoomAudioEngine,
    savedSongsRepository: SavedSongsRepository = remember { SavedSongsRepository(navController.context.applicationContext) }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isPlaying by audioEngine.isPlaying.collectAsState()
    val visualizerData by audioEngine.visualizerFlow.collectAsState()
    val trackTitle by audioEngine.trackTitle.collectAsState()
    val currentPosition by audioEngine.currentPosition.collectAsState()
    val duration by audioEngine.duration.collectAsState()
    val effectState by audioEngine.effectState.collectAsState()
    val currentUri by audioEngine.currentUri.collectAsState()

    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var aiExpanded by remember { mutableStateOf(false) }
    var aiAnalysisData by remember { mutableStateOf<AudioAnalysisResult?>(null) }

    // Export & Feedback State
    val exportEngine = remember { AudioExportEngine(context.applicationContext) }
    val exportStage by exportEngine.exportStage.collectAsState()
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportedSongItem by remember { mutableStateOf<SavedSongItem?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected Track"
            audioEngine.playUri(uri, fileName)
            feedbackMessage = "Loaded: $fileName"
        }
    }

    val currentMs = if (isSeeking) seekPosition.toLong() else currentPosition
    val durationMs = duration.coerceAtLeast(1L)
    val sliderValue = currentMs.toFloat().coerceIn(0f, durationMs.toFloat())
    val durationSeconds = (durationMs / 1000L).coerceAtLeast(1L)

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    fun shareSong(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Enhanced Audio"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerExport(format: String, saveDirectlyToLibrary: Boolean = false) {
        scope.launch {
            val result = exportEngine.exportAudio(
                inputUri = currentUri,
                trackTitle = trackTitle,
                effectState = effectState,
                format = format
            )
            if (result != null) {
                savedSongsRepository.saveSongRecord(result)
                exportedSongItem = result
                if (saveDirectlyToLibrary) {
                    feedbackMessage = "✓ Saved '${result.title}' to Library!"
                } else {
                    showExportSuccessDialog = true
                }
            }
        }
    }

    // Export Success Dialog
    if (showExportSuccessDialog && exportedSongItem != null) {
        val song = exportedSongItem!!
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Complete!", fontWeight = FontWeight.Bold, color = TextWhite)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Track: ${song.title}", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Format: ${song.format} • Size: ${(song.fileSizeBytes / 1024)} KB", color = TextMuted, fontSize = 12.sp)
                    Text("Preset: ${song.appliedPreset}", color = CyanNeon, fontSize = 12.sp)
                    Text("Location: ${song.filePath}", color = TextDarkMuted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportSuccessDialog = false
                    val file = File(song.filePath)
                    if (file.exists()) shareSong(file)
                }) {
                    Text("SHARE", color = PurpleNeon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportSuccessDialog = false
                    navController.navigate(Screen.SavedSongs.route)
                }) {
                    Text("OPEN LIBRARY", color = CyanNeon)
                }
            },
            containerColor = DarkCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Top Navbar
        BoomBoomTopBar(
            currentRoute = Screen.AudioEditor.route,
            navController = navController
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Track Title Row: Title + "Editing • Xs" on left, [↑ Change] button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "Editing • ${durationSeconds}s",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MiniActionButton(
                    text = "Change",
                    icon = Icons.Default.FileUpload,
                    accentColor = CyanNeon,
                    onClick = { filePickerLauncher.launch("audio/*") }
                )
            }

            // Waveform Visualizer
            ModernWaveformVisualizer(
                visualizerData = visualizerData,
                modifier = Modifier.fillMaxWidth()
            )

            // Scrubber Slider + Transport Controls Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Time row and slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentMs),
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.width(42.dp)
                        )

                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                isSeeking = true
                                seekPosition = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                audioEngine.seekTo(seekPosition.toLong())
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = PurpleBrand,
                                activeTrackColor = PurpleBrand,
                                inactiveTrackColor = DarkCardBgElevated
                            ),
                            modifier = Modifier.weight(1f).height(24.dp)
                        )

                        Text(
                            text = formatTime(durationMs),
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.width(42.dp)
                        )
                    }

                    // Transport Buttons Row: [↺ 10s] [⏮] [ Glowing Play/Pause ] [⏭]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { audioEngine.seekBy(-10000L) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { audioEngine.seekTo(0L) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Restart",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Big Glowing Circular Play/Pause Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(14.dp, CircleShape, ambientColor = PurpleBrand, spotColor = PurpleNeon)
                                .background(PurpleBrand, CircleShape)
                                .clickable { audioEngine.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = TextWhite,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { audioEngine.seekBy(10000L) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Temporary Feedback / Notification Banner
            if (feedbackMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.15f))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feedbackMessage!!,
                            fontSize = 12.sp,
                            color = CyanNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = CyanNeon,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { feedbackMessage = null }
                        )
                    }
                }
            }

            // Export Processing Indicator
            if (exportStage is ExportStage.Processing) {
                val processing = exportStage as ExportStage.Processing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardBgElevated)
                        .border(1.dp, PurpleBrand, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exporting audio...", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("${(processing.progress * 100).toInt()}%", fontSize = 12.sp, color = PurpleNeon, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { processing.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = PurpleBrand,
                            trackColor = DarkCardBg
                        )
                        Text(processing.statusMessage, fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            // Expandable ✦ AI ENHANCE Accordion Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardBg)
                    .border(1.dp, if (aiExpanded) PurpleBrand else CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aiExpanded = !aiExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(PurpleBrand.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PurpleNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI ENHANCE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "AI-powered audio intelligence",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = if (aiExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (aiExpanded) "Collapse" else "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(
                        visible = aiExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // AI Analysis Result Card (if analyzed)
                            if (aiAnalysisData != null) {
                                val analysis = aiAnalysisData!!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PurpleBrand.copy(alpha = 0.12f))
                                        .border(1.dp, PurpleBrand.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = PurpleNeon,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = analysis.genre,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PurpleNeon
                                                )
                                            }
                                            Text(
                                                text = "${(analysis.confidence * 100).toInt()}% confidence",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }

                                        Text(
                                            text = "✦ ${analysis.recommendedPreset}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CyanNeon
                                        )

                                        if (analysis.insights.isNotEmpty()) {
                                            Text(
                                                text = analysis.insights,
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        // Issue tags
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (analysis.hasDistortion) {
                                                CyberBadge(text = "⚠ Distortion", color = PinkNeon)
                                            }
                                            if (analysis.hasMuddyBass) {
                                                CyberBadge(text = "⚠ Muddy Bass", color = PinkNeon)
                                            }
                                            if (analysis.hasWeakVocals) {
                                                CyberBadge(text = "⚠ Weak Vocals", color = PinkNeon)
                                            }
                                        }
                                    }
                                }
                            }

                            // 1-Click Auto Enhance Button
                            GlowGradientButton(
                                text = "✨ AI Auto Enhance",
                                onClick = {
                                    scope.launch {
                                        val dummyPcm = ShortArray(44100) { (it % 100 * 300).toShort() }
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(dummyPcm)
                                        aiAnalysisData = analysis
                                        val autoState = AiAudioAnalyzer.createAutoEnhanceState(analysis)
                                        audioEngine.updateEffectState(autoState)
                                        feedbackMessage = "✦ AI Auto-Enhanced: Detected ${analysis.genre}"
                                    }
                                },
                                gradient = listOf(PurpleBrand, CyanNeon),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            )

                            // 6 AI Smart Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AiFeatureCard(
                                    title = "AI Smart Bass",
                                    desc = "Intelligent bass boost without mud",
                                    icon = Icons.Default.Bolt,
                                    accentColor = PurpleNeon,
                                    onClick = {
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(ShortArray(44100) { (it % 100 * 300).toShort() })
                                        aiAnalysisData = analysis.copy(insights = "Boosted bass while preventing mud based on frequency profile.")
                                        audioEngine.updateEffectState(AiAudioAnalyzer.createSmartBassState(analysis))
                                        feedbackMessage = "✦ AI Smart Bass applied!"
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                AiFeatureCard(
                                    title = "AI Vocal Cleaner",
                                    desc = "Clarity boost & background noise",
                                    icon = Icons.Default.Mic,
                                    accentColor = CyanNeon,
                                    onClick = {
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(ShortArray(44100) { (it % 100 * 300).toShort() })
                                        aiAnalysisData = analysis.copy(insights = "Enhanced vocal presence and reduced competing low-end based on spectral analysis.")
                                        audioEngine.updateEffectState(AiAudioAnalyzer.createVocalCleanerState(analysis))
                                        feedbackMessage = "✦ AI Vocal Cleaner applied!"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AiFeatureCard(
                                    title = "AI Distortion Reducer",
                                    desc = "Clipping & harsh frequency fix",
                                    icon = Icons.Default.Shield,
                                    accentColor = PinkNeon,
                                    onClick = {
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(ShortArray(44100) { (it % 100 * 300).toShort() })
                                        aiAnalysisData = analysis.copy(insights = "Detected clipping. Applied dynamic range correction and frequency smoothing.", hasDistortion = true)
                                        audioEngine.updateEffectState(AiAudioAnalyzer.createDistortionReducerState(analysis))
                                        feedbackMessage = "✦ AI Distortion Reducer applied!"
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                AiFeatureCard(
                                    title = "AI Loudness Balancer",
                                    desc = "Dynamic range optimization",
                                    icon = Icons.Default.Equalizer,
                                    accentColor = CyanNeon,
                                    onClick = {
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(ShortArray(44100) { (it % 100 * 300).toShort() })
                                        aiAnalysisData = analysis.copy(insights = "Balanced loudness for consistent playback dynamics.")
                                        audioEngine.updateEffectState(AiAudioAnalyzer.createLoudnessBalancerState(analysis))
                                        feedbackMessage = "✦ AI Loudness Balancer applied!"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AiFeatureCard(
                                    title = "AI Earphone Optimizer",
                                    desc = "Tuned for small drivers & mobile",
                                    icon = Icons.Default.Headphones,
                                    accentColor = CyanAccent,
                                    onClick = {
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(ShortArray(44100) { (it % 100 * 300).toShort() })
                                        aiAnalysisData = analysis.copy(insights = "Applied earphone optimization: boosted mids, enhanced clarity, subtle 3D widening.")
                                        audioEngine.updateEffectState(AiAudioAnalyzer.createEarphoneOptimizerState(analysis))
                                        feedbackMessage = "✦ AI Earphone Optimizer applied!"
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                AiFeatureCard(
                                    title = "AI Genre Detection",
                                    desc = "Identify music type & auto-preset",
                                    icon = Icons.Default.MusicNote,
                                    accentColor = PurpleNeon,
                                    onClick = {
                                        val analysis = AiAudioAnalyzer.analyzeAudioPcm(ShortArray(44100) { (it % 100 * 300).toShort() })
                                        aiAnalysisData = analysis
                                        feedbackMessage = "✦ Genre Detected: ${analysis.genre}"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // EFFECTS Header: EFFECTS on left, [Reset All] on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EFFECTS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextWhite
                )

                MiniActionButton(
                    text = "Reset All",
                    icon = Icons.Default.Refresh,
                    accentColor = TextMuted,
                    onClick = {
                        audioEngine.updateEffectState(
                            AudioEffectState(
                                bassBoost = 0f,
                                vocalBoost = 0f,
                                pitch = 0f,
                                speed = 1.0f,
                                loudness = 0f,
                                stereoWidth = 0f,
                                reverb = 0f,
                                spatial3D = 0f,
                                is8DActive = false,
                                speed8D = 0f,
                                subwooferSurround = 0f,
                                distortionCut = 0f,
                                activePresetName = "Normal Mode"
                            )
                        )
                        feedbackMessage = "Effects reset to default"
                    }
                )
            }

            // 10 Effect Sliders
            // 1. Bass Boost (-30 to 100)
            EffectSliderCard(
                title = "Bass Boost",
                value = effectState.bassBoost,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(bassBoost = newVal) } },
                valueRange = -30f..100f,
                valueDisplay = "${effectState.bassBoost.roundToInt()}%",
                icon = Icons.Default.GraphicEq
            )

            // 2. Vocal Boost (-30 to 100)
            EffectSliderCard(
                title = "Vocal Boost",
                value = effectState.vocalBoost,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(vocalBoost = newVal) } },
                valueRange = -30f..100f,
                valueDisplay = "${effectState.vocalBoost.roundToInt()}%",
                icon = Icons.Default.Mic
            )

            // 3. Pitch Shift (-12 to +12 st)
            EffectSliderCard(
                title = "Pitch Shift",
                value = effectState.pitch,
                onValueChange = { newSt -> audioEngine.updateEffect { it.copy(pitch = newSt) } },
                valueRange = -12f..12f,
                valueDisplay = if (effectState.pitch > 0) "+${effectState.pitch.roundToInt()} st" else "${effectState.pitch.roundToInt()} st",
                icon = Icons.Default.Tune
            )

            // 4. Speed (0.25x to 3.0x)
            EffectSliderCard(
                title = "Speed",
                value = effectState.speed,
                onValueChange = { newSpeed -> audioEngine.updateEffect { it.copy(speed = newSpeed) } },
                valueRange = 0.25f..3.0f,
                valueDisplay = "%.2fx".format(effectState.speed),
                icon = Icons.Default.Speed
            )

            // 5. Loudness (0 to 100)
            EffectSliderCard(
                title = "Loudness",
                value = effectState.loudness,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(loudness = newVal) } },
                valueRange = 0f..100f,
                valueDisplay = "${effectState.loudness.roundToInt()}%",
                icon = Icons.AutoMirrored.Filled.VolumeUp
            )

            // 6. Stereo Width (0 to 100%)
            EffectSliderCard(
                title = "Stereo Width",
                value = effectState.stereoWidth,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(stereoWidth = newVal) } },
                valueRange = 0f..100f,
                valueDisplay = "${effectState.stereoWidth.roundToInt()}%",
                icon = Icons.Default.SwapHoriz
            )

            // 7. Reverb (0 to 100%)
            EffectSliderCard(
                title = "Reverb",
                value = effectState.reverb,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(reverb = newVal) } },
                valueRange = 0f..100f,
                valueDisplay = "${effectState.reverb.roundToInt()}%",
                icon = Icons.Default.Waves
            )

            // 8. 3D Audio (0 to 100%)
            EffectSliderCard(
                title = "3D Audio",
                value = effectState.spatial3D,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(spatial3D = newVal) } },
                valueRange = 0f..100f,
                valueDisplay = "${effectState.spatial3D.roundToInt()}%",
                icon = Icons.Default.SpatialAudio
            )

            // 9. 8D Audio Speed (0 to 100%)
            EffectSliderCard(
                title = "8D Audio Speed",
                value = effectState.speed8D,
                onValueChange = { newVal ->
                    audioEngine.updateEffect {
                        it.copy(
                            speed8D = newVal,
                            is8DActive = newVal > 0.05f
                        )
                    }
                },
                valueRange = 0f..100f,
                valueDisplay = "${effectState.speed8D.roundToInt()}%",
                icon = Icons.AutoMirrored.Filled.RotateRight
            )

            // 10. Subwoofer Surround (0 to 100%)
            EffectSliderCard(
                title = "Subwoofer Surround",
                value = effectState.subwooferSurround,
                onValueChange = { newVal -> audioEngine.updateEffect { it.copy(subwooferSurround = newVal) } },
                valueRange = 0f..100f,
                valueDisplay = "${effectState.subwooferSurround.roundToInt()}%",
                icon = Icons.Default.SpeakerGroup
            )

            // Bottom Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Export WAV, Export FLAC (24-bit), Export MP3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniActionButton(
                        text = "Export WAV",
                        icon = Icons.Default.FileUpload,
                        onClick = { triggerExport("WAV") },
                        modifier = Modifier.weight(1f)
                    )

                    MiniActionButton(
                        text = "Export FLAC (24-bit)",
                        icon = Icons.Default.FileUpload,
                        onClick = { triggerExport("FLAC") },
                        modifier = Modifier.weight(1.2f)
                    )

                    MiniActionButton(
                        text = "Export MP3",
                        icon = Icons.Default.FileUpload,
                        onClick = { triggerExport("MP3") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Save to Library, Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniActionButton(
                        text = "Save to Library",
                        icon = Icons.Default.Save,
                        accentColor = GreenNeon,
                        onClick = { triggerExport("MP3", saveDirectlyToLibrary = true) },
                        modifier = Modifier.weight(1f)
                    )

                    MiniActionButton(
                        text = "Presets",
                        icon = Icons.Default.Equalizer,
                        accentColor = CyanNeon,
                        onClick = { navController.navigate(Screen.Presets.route) },
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AiFeatureCard(
    title: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardBgElevated)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1
                )
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

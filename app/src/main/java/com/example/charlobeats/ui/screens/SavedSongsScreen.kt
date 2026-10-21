package com.example.charlobeats.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.charlobeats.audio.model.SavedSongItem
import com.example.charlobeats.audio.player.BoomAudioEngine
import com.example.charlobeats.data.SavedSongsRepository
import com.example.charlobeats.ui.components.*
import com.example.charlobeats.ui.navigation.Screen
import com.example.charlobeats.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SavedSongsScreen(
    navController: NavController,
    audioEngine: BoomAudioEngine,
    savedSongsRepository: SavedSongsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedSongs by savedSongsRepository.savedSongs.collectAsState()
    val trackTitle by audioEngine.trackTitle.collectAsState()
    val currentUri by audioEngine.currentUri.collectAsState()
    val isPlaying by audioEngine.isPlaying.collectAsState()

    var songToDelete by remember { mutableStateOf<SavedSongItem?>(null) }

    LaunchedEffect(Unit) {
        savedSongsRepository.loadSavedSongs()
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
            context.startActivity(Intent.createChooser(intent, "Share Enhanced Song"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    // Delete Confirmation Dialog
    if (songToDelete != null) {
        val song = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Delete Track", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = { Text("Are you sure you want to delete '${song.title}' from your saved library?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        savedSongsRepository.deleteSong(song)
                    }
                    songToDelete = null
                }) {
                    Text("DELETE", color = PinkNeon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("CANCEL", color = TextMuted)
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
        // Mockup Exact Top Bar
        BoomBoomTopBar(
            currentRoute = Screen.SavedSongs.route,
            navController = navController
        )

        // Subheader matching Image 4: SAVED SONGS + "Your enhanced audio library"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "SAVED SONGS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextWhite
            )
            Text(
                text = "Your enhanced audio library (${savedSongs.size} tracks)",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        if (savedSongs.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(DarkCardBgElevated, CircleShape)
                            .border(1.dp, CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = PurpleNeon,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "No saved tracks yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Text(
                        text = "Go to the Editor to enhance audio with 8D, Bass Boost and export directly to your library.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    GlowGradientButton(
                        text = "Go to Editor →",
                        onClick = { navController.navigate(Screen.AudioEditor.route) },
                        modifier = Modifier.width(180.dp).height(44.dp)
                    )
                }
            }
        } else {
            // Track List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedSongs) { song ->
                    val isCurrentlyLoaded = currentUri?.path == song.filePath || currentUri?.toString() == Uri.fromFile(File(song.filePath)).toString()
                    val isCurrentPlaying = isCurrentlyLoaded && isPlaying

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkCardBg)
                            .border(
                                width = 1.dp,
                                color = if (isCurrentlyLoaded) PurpleBrand else CardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left squircle icon with MusicNote
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(DarkCardBgElevated, RoundedCornerShape(12.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        val file = File(song.filePath)
                                        if (file.exists()) {
                                            if (isCurrentPlaying) {
                                                audioEngine.pause()
                                            } else {
                                                audioEngine.playUri(Uri.fromFile(file), song.title)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCurrentPlaying) GreenNeon else PurpleNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Track Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "⏱ ${formatDuration(song.durationMs)}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )

                                    CyberBadge(
                                        text = song.format,
                                        color = CyanNeon
                                    )

                                    if (song.appliedPreset.isNotEmpty()) {
                                        CyberBadge(
                                            text = "🎛 ${song.appliedPreset}",
                                            color = PurpleNeon
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Actions: Share & Delete
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val file = File(song.filePath)
                                        if (file.exists()) shareSong(file)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { songToDelete = song },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = PinkNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
}

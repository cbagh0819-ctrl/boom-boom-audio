package com.example.charlobeats.ui.navigation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.charlobeats.audio.player.BoomAudioEngine
import com.example.charlobeats.data.SavedSongsRepository
import com.example.charlobeats.ui.screens.*
import com.example.charlobeats.ui.theme.*

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object AudioEditor : Screen("editor", "Editor", Icons.Default.Tune)
    object Presets : Screen("presets", "Presets", Icons.Default.MusicNote)
    object SavedSongs : Screen("saved_songs", "Saved", Icons.Default.Folder)
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val audioEngine = remember { BoomAudioEngine(context.applicationContext) }
    val savedSongsRepository = remember { SavedSongsRepository(context.applicationContext) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPlaying by audioEngine.isPlaying.collectAsState()
    val trackTitle by audioEngine.trackTitle.collectAsState()
    val effectState by audioEngine.effectState.collectAsState()

    // Show mini-player only if music is active and user is outside AudioEditor
    val showMiniPlayer = currentRoute != Screen.AudioEditor.route && currentRoute != Screen.Splash.route && isPlaying

    Scaffold(
        containerColor = DarkCanvas,
        bottomBar = {
            AnimatedVisibility(visible = showMiniPlayer) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCardBgElevated)
                        .border(1.dp, PurpleBrand.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable {
                            navController.navigate(Screen.AudioEditor.route)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(PurpleBrand.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PurpleNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = trackTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Playing • ${effectState.activePresetName}",
                                    fontSize = 11.sp,
                                    color = CyanNeon
                                )
                            }
                        }

                        IconButton(
                            onClick = { audioEngine.togglePlayPause() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(PurpleBrand, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(navController)
            }
            composable(Screen.Home.route) {
                HomeScreen(navController, audioEngine)
            }
            composable(Screen.AudioEditor.route) {
                AudioEditorScreen(navController, audioEngine, savedSongsRepository)
            }
            composable(Screen.Presets.route) {
                PresetSelectionScreen(navController, audioEngine)
            }
            composable(Screen.SavedSongs.route) {
                SavedSongsScreen(navController, audioEngine, savedSongsRepository)
            }
        }
    }
}

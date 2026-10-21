package com.example.charlobeats.audio.preset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.charlobeats.audio.model.AudioEffectState

data class AudioPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val effectState: AudioEffectState,
    val tags: List<String>
)

object AudioPresetRepository {

    val presets: List<AudioPreset> = listOf(
        AudioPreset(
            id = "night_bass",
            title = "Night Bass Mode",
            subtitle = "Deep Sub & Ambient Warmth",
            description = "Deep bass for late-night sessions",
            icon = Icons.Default.Bedtime,
            effectState = AudioEffectState(
                bassBoost = 85f,
                vocalBoost = -10f,
                pitch = -2f,
                speed = 1.0f,
                loudness = 20f,
                stereoWidth = 30f,
                reverb = 15f,
                spatial3D = 0f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 20f,
                activePresetName = "Night Bass Mode"
            ),
            tags = listOf("Deep Bass", "Late Night", "Warm")
        ),
        AudioPreset(
            id = "dj_punch",
            title = "DJ Punch Mode",
            subtitle = "Club Kick & Dynamic Drive",
            description = "Hard-hitting club-ready sound",
            icon = Icons.Default.Album,
            effectState = AudioEffectState(
                bassBoost = 70f,
                vocalBoost = 10f,
                pitch = 0f,
                speed = 1.0f,
                loudness = 40f,
                stereoWidth = 50f,
                reverb = 10f,
                spatial3D = 20f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 50f,
                activePresetName = "DJ Punch Mode"
            ),
            tags = listOf("Club", "Punchy", "EDM")
        ),
        AudioPreset(
            id = "concert_mode",
            title = "Concert Mode",
            subtitle = "Stadium Hall & Spatial Immersion",
            description = "Live concert hall experience",
            icon = Icons.Default.MusicNote,
            effectState = AudioEffectState(
                bassBoost = 30f,
                vocalBoost = 20f,
                pitch = 0f,
                speed = 1.0f,
                loudness = 15f,
                stereoWidth = 60f,
                reverb = 55f,
                spatial3D = 40f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 25f,
                activePresetName = "Concert Mode"
            ),
            tags = listOf("Live Hall", "Wide Stage", "Atmospheric")
        ),
        AudioPreset(
            id = "anime_bass",
            title = "Anime Bass Preset",
            subtitle = "Crisp J-Pop Vocals & Kick",
            description = "Anime OP/ED bass enhancement",
            icon = Icons.Default.FlashOn,
            effectState = AudioEffectState(
                bassBoost = 60f,
                vocalBoost = 30f,
                pitch = 1f,
                speed = 1.0f,
                loudness = 25f,
                stereoWidth = 40f,
                reverb = 20f,
                spatial3D = 10f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 20f,
                activePresetName = "Anime Bass Preset"
            ),
            tags = listOf("Crisp Air", "J-Pop", "Sparkle")
        ),
        AudioPreset(
            id = "cheap_earphone",
            title = "Cheap Earphone Enhancer",
            subtitle = "Hardware Curve Compensation",
            description = "Make cheap earphones sound premium",
            icon = Icons.Default.Headphones,
            effectState = AudioEffectState(
                bassBoost = 50f,
                vocalBoost = 25f,
                pitch = 0f,
                speed = 1.0f,
                loudness = 35f,
                stereoWidth = 20f,
                reverb = 5f,
                spatial3D = 15f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 30f,
                activePresetName = "Cheap Earphone Enhancer"
            ),
            tags = listOf("Earbuds", "Soundstage", "Clarity")
        ),
        AudioPreset(
            id = "car_speaker",
            title = "Car Speaker Optimization",
            subtitle = "Subwoofer Cabin Tuning",
            description = "Optimized for car audio systems",
            icon = Icons.Default.DirectionsCar,
            effectState = AudioEffectState(
                bassBoost = 55f,
                vocalBoost = 15f,
                pitch = 0f,
                speed = 1.0f,
                loudness = 30f,
                stereoWidth = 45f,
                reverb = 8f,
                spatial3D = 25f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 60f,
                activePresetName = "Car Speaker Optimization"
            ),
            tags = listOf("Subwoofer", "Car Audio", "Road Noise")
        ),
        AudioPreset(
            id = "youtube_vocal",
            title = "YouTube Vocal Cleaner",
            subtitle = "Dialogue Isolation & De-Muffle",
            description = "Crystal clear vocals for voice content",
            icon = Icons.Default.Mic,
            effectState = AudioEffectState(
                bassBoost = -20f,
                vocalBoost = 70f,
                pitch = 0f,
                speed = 1.0f,
                loudness = 20f,
                stereoWidth = 10f,
                reverb = 3f,
                spatial3D = 0f,
                is8DActive = false,
                speed8D = 0f,
                subwooferSurround = 0f,
                activePresetName = "YouTube Vocal Cleaner"
            ),
            tags = listOf("Speech", "Clarity", "Vocal")
        )
    )
}

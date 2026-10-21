package com.example.charlobeats.audio.model

data class AudioEffectState(
    val bassBoost: Float = 0f,           // -30f..100f (Low-shelf 150Hz boost)
    val vocalBoost: Float = 0f,          // -30f..100f (Peaking 2500Hz boost)
    val pitch: Float = 0f,               // -12f..12f (Playback pitch in semitones)
    val speed: Float = 1.0f,             // 0.25f..3.0f (Playback speed rate)
    val loudness: Float = 0f,            // 0f..100f (Compressor & makeup gain)
    val stereoWidth: Float = 0f,         // 0f..100f (Mid-side stereo separation)
    val reverb: Float = 0f,              // 0f..100f (Reverb wet amount %)
    val spatial3D: Float = 0f,           // 0f..100f (3D spatial positioning)
    val is8DActive: Boolean = false,     // Continuous 8D rotating audio
    val speed8D: Float = 0f,             // 0f..100f (8D rotation speed)
    val subwooferSurround: Float = 0f,   // 0f..100f (Subwoofer shelf & surround field)
    val distortionCut: Float = 0f,       // 0f..100f (Harshness and clipping reducer)
    val activePresetName: String = "Normal Mode"
)

data class VisualizerData(
    val bands: FloatArray = FloatArray(32) { 0.05f },
    val rmsLevel: Float = 0f,
    val peakLevel: Float = 0f,
    val isPlaying: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualizerData
        return bands.contentEquals(other.bands) && rmsLevel == other.rmsLevel && peakLevel == other.peakLevel && isPlaying == other.isPlaying
    }

    override fun hashCode(): Int {
        var result = bands.contentHashCode()
        result = 31 * result + rmsLevel.hashCode()
        result = 31 * result + peakLevel.hashCode()
        result = 31 * result + isPlaying.hashCode()
        return result
    }
}

data class AudioAnalysisResult(
    val genre: String,
    val confidence: Float = 0.92f,
    val tempoBpm: Int = 128,
    val bassRatio: Float = 0.40f,
    val midRatio: Float = 0.35f,
    val trebleRatio: Float = 0.25f,
    val dynamicRangeDb: Float = 14f,
    val clippingRatio: Float = 0f,
    val summary: String = "",
    val recommendedPreset: String = "DJ Punch Mode",
    val insights: String = "",
    val hasDistortion: Boolean = false,
    val hasMuddyBass: Boolean = false,
    val hasWeakVocals: Boolean = false,
    val isLoudnessImbalanced: Boolean = false,
    val isNarrowStereo: Boolean = false
)

data class SavedSongItem(
    val id: String,
    val title: String,
    val artist: String = "Boom-Boom Audio",
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val format: String, // MP3, WAV, FLAC
    val appliedPreset: String,
    val createdAt: Long = System.currentTimeMillis()
)

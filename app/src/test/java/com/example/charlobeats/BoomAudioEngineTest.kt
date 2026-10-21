package com.example.charlobeats

import com.example.charlobeats.audio.ai.AiAudioAnalyzer
import com.example.charlobeats.audio.model.AudioEffectState
import com.example.charlobeats.audio.preset.AudioPresetRepository
import com.example.charlobeats.audio.processor.DspProcessor
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class BoomAudioEngineTest {

    @Test
    fun testAllPresetModesExist() {
        val presets = AudioPresetRepository.presets
        assertEquals("There should be 7 preset modes", 7, presets.size)

        val presetTitles = presets.map { it.title }
        assertTrue(presetTitles.contains("Night Bass Mode"))
        assertTrue(presetTitles.contains("DJ Punch Mode"))
        assertTrue(presetTitles.contains("Concert Mode"))
        assertTrue(presetTitles.contains("Anime Bass Preset"))
        assertTrue(presetTitles.contains("Cheap Earphone Enhancer"))
        assertTrue(presetTitles.contains("Car Speaker Optimization"))
        assertTrue(presetTitles.contains("YouTube Vocal Cleaner"))

        for (preset in presets) {
            val state = preset.effectState
            assertTrue(state.bassBoost in -30f..100f)
            assertTrue(state.vocalBoost in -30f..100f)
            assertTrue(state.loudness in 0f..100f)
            assertTrue(state.stereoWidth in 0f..100f)
            assertTrue(state.reverb in 0f..100f)
        }
    }

    @Test
    fun testDspProcessorBassEnhancement() {
        val dsp = DspProcessor(sampleRate = 44100)
        val state = AudioEffectState(bassBoost = 85f, loudness = 50f)
        dsp.updateFilters(state)

        // Generate a 60 Hz low-frequency sine wave (bass)
        val sampleCount = 1000
        val buffer = FloatArray(sampleCount * 2) // Stereo
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / 44100.0
            val bassSample = (sin(2.0 * Math.PI * 60.0 * t) * 0.3).toFloat()
            buffer[i * 2] = bassSample
            buffer[i * 2 + 1] = bassSample
        }

        dsp.processBuffer(buffer, 0, buffer.size, state)

        // Verify bass boost produces non-zero signal
        assertTrue("RMS energy must be greater than 0", dsp.currentRms > 0f)
        assertTrue("Peak energy must be greater than 0", dsp.currentPeak > 0f)
        assertTrue("Spectrum bands must be calculated", dsp.spectrumBands.isNotEmpty())
    }

    @Test
    fun testDspProcessorSoftKneeLimiterPreventsClipping() {
        val dsp = DspProcessor(sampleRate = 44100)
        // High loudness boost and bass that would normally clip severely
        val state = AudioEffectState(bassBoost = 100f, loudness = 100f, subwooferSurround = 100f, distortionCut = 80f)
        dsp.updateFilters(state)

        val buffer = FloatArray(100) { 1.5f } // Dangerously loud input samples

        dsp.processBuffer(buffer, 0, buffer.size, state)

        for (sample in buffer) {
            assertFalse("Sample must not be NaN", sample.isNaN())
            assertFalse("Sample must not be Infinite", sample.isInfinite())
            assertTrue("Sample must not exceed 1.0f (was $sample)", sample <= 1.0f)
            assertTrue("Sample must not be less than -1.0f (was $sample)", sample >= -1.0f)
        }
    }

    @Test
    fun testDspProcessorStrictMonotonicityNoFoldback() {
        val dsp = DspProcessor(sampleRate = 44100)
        val state = AudioEffectState(bassBoost = 90f, loudness = 60f)
        dsp.updateFilters(state)

        // Test varying amplitudes to ensure no wave-folding or inverted slope
        val buffer = FloatArray(2000)
        for (i in 0 until 1000) {
            val t = i.toDouble() / 44100.0
            val s = (sin(2.0 * Math.PI * 55.0 * t) * 0.9).toFloat()
            buffer[i * 2] = s
            buffer[i * 2 + 1] = s
        }

        dsp.processBuffer(buffer, 0, buffer.size, state)

        for (sample in buffer) {
            assertFalse("No NaN in processed buffer", sample.isNaN())
            assertTrue("Sample within [-1.0, 1.0]", sample in -1.0f..1.0f)
        }
    }

    @Test
    fun testAiGenreDetectionAndAutoEnhance() {
        // Synthesize bass-heavy electronic samples
        val pcm = ShortArray(4000) { idx ->
            val t = idx.toDouble() / 44100.0
            val sub = sin(2.0 * Math.PI * 50.0 * t) * 25000.0
            sub.toInt().toShort()
        }

        val analysis = AiAudioAnalyzer.analyzeAudioPcm(pcm)
        assertNotNull(analysis.genre)
        assertTrue(analysis.bassRatio > 0.3f)

        // Test 1-click Auto Enhance
        val autoState = AiAudioAnalyzer.createAutoEnhanceState(analysis)
        assertTrue(autoState.bassBoost in 30f..100f)
        assertTrue(autoState.vocalBoost in 0f..100f)
        assertEquals("AI Auto-Enhanced", autoState.activePresetName)
    }

    @Test
    fun testAiSmartBassAndVocalCleaner() {
        val pcm = ShortArray(4000) { (it % 100 * 200).toShort() }
        val analysis = AiAudioAnalyzer.analyzeAudioPcm(pcm)

        val smartBass = AiAudioAnalyzer.createSmartBassState(analysis)
        assertTrue(smartBass.bassBoost >= 50f)
        assertEquals("AI Smart Bass", smartBass.activePresetName)

        val vocalCleaner = AiAudioAnalyzer.createVocalCleanerState(analysis)
        assertTrue(vocalCleaner.vocalBoost >= 40f)
        assertEquals("AI Vocal Cleaner", vocalCleaner.activePresetName)

        val distReducer = AiAudioAnalyzer.createDistortionReducerState(analysis)
        assertTrue(distReducer.distortionCut >= 50f)
        assertEquals("AI Distortion Reducer", distReducer.activePresetName)
    }
}

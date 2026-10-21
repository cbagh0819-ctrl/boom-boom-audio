package com.example.charlobeats.audio.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.charlobeats.audio.model.AudioEffectState
import com.example.charlobeats.audio.model.VisualizerData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class BoomAudioProcessor : BaseAudioProcessor() {

    private var dspProcessor: DspProcessor? = null
    var effectState: AudioEffectState = AudioEffectState()
        set(value) {
            field = value
            dspProcessor?.updateFilters(value)
        }

    private val _visualizerFlow = MutableStateFlow(VisualizerData())
    val visualizerFlow: StateFlow<VisualizerData> = _visualizerFlow

    private var floatBuffer = FloatArray(4096)
    private var lastVisualizerUpdateTime = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        val channels = inputAudioFormat.channelCount.coerceIn(1, 2)
        // We output standard 16-bit stereo PCM
        val outputFormat = AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
        dspProcessor = DspProcessor(inputAudioFormat.sampleRate).apply {
            updateFilters(effectState)
        }
        return outputFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        val inputChannelCount = inputAudioFormat.channelCount
        val sampleCount = remainingBytes / 2 // 16-bit PCM
        val frameCount = sampleCount / inputChannelCount
        val stereoSampleCount = frameCount * 2

        if (floatBuffer.size < stereoSampleCount) {
            floatBuffer = FloatArray(stereoSampleCount)
        }

        // Convert 16-bit input PCM into stereo normalized floats [-1.0f..1.0f]
        var floatIdx = 0
        if (inputChannelCount == 1) {
            while (inputBuffer.hasRemaining()) {
                val sampleShort = inputBuffer.short
                val sampleFloat = sampleShort / 32768.0f
                floatBuffer[floatIdx++] = sampleFloat // Left
                floatBuffer[floatIdx++] = sampleFloat // Right
            }
        } else {
            while (inputBuffer.hasRemaining()) {
                val left = inputBuffer.short / 32768.0f
                val right = if (inputBuffer.hasRemaining()) inputBuffer.short / 32768.0f else left
                floatBuffer[floatIdx++] = left
                floatBuffer[floatIdx++] = right
            }
        }

        // Apply Real-Time DSP Audio Processing
        val dsp = dspProcessor
        if (dsp != null) {
            dsp.processBuffer(floatBuffer, 0, stereoSampleCount, effectState)

            // Emit live visualizer data ~60fps
            val now = System.currentTimeMillis()
            if (now - lastVisualizerUpdateTime > 16) {
                lastVisualizerUpdateTime = now
                _visualizerFlow.value = VisualizerData(
                    bands = dsp.spectrumBands.clone(),
                    rmsLevel = dsp.currentRms,
                    peakLevel = dsp.currentPeak,
                    isPlaying = true
                )
            }
        }

        // Write processed stereo float audio back to 16-bit PCM output buffer
        val outputBytes = stereoSampleCount * 2
        val outBuffer = replaceOutputBuffer(outputBytes)
        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until stereoSampleCount) {
            val sampleInt = (floatBuffer[i] * 32767.0f).toInt().coerceIn(-32768, 32767)
            outBuffer.putShort(sampleInt.toShort())
        }
        outBuffer.flip()
    }

    fun notifyPlaybackStopped() {
        _visualizerFlow.value = VisualizerData(
            bands = FloatArray(32) { 0.05f },
            rmsLevel = 0f,
            peakLevel = 0f,
            isPlaying = false
        )
    }
}

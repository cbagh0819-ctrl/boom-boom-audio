package com.example.charlobeats.audio.processor

import com.example.charlobeats.audio.model.AudioEffectState
import kotlin.math.*

/**
 * High-performance digital signal processing (DSP) filters for Boom-Boom Audio.
 * Implements Biquad filters, low-noise Schroeder-Moorer reverberator, 8D binaural panner,
 * energy-normalized stereo widener, DC-offset blocker, and a dynamic peak mastering limiter
 * that prevents noise, crackling, and distortion even at maximum bass boosts.
 */
class DspProcessor(val sampleRate: Int = 44100) {

    // Biquad filter for Low-End / Bass Boost
    private val bassFilterL = BiquadFilter()
    private val bassFilterR = BiquadFilter()

    // Biquad filter for Vocal / Mid-High boost
    private val vocalFilterL = BiquadFilter()
    private val vocalFilterR = BiquadFilter()

    // Subwoofer filters (Sub shelf, punch, mud cut)
    private val subShelfL = BiquadFilter()
    private val subShelfR = BiquadFilter()
    private val punchFilterL = BiquadFilter()
    private val punchFilterR = BiquadFilter()
    private val mudCutFilterL = BiquadFilter()
    private val mudCutFilterR = BiquadFilter()

    // DC-offset blocker filters (10 Hz High-Pass)
    private var dcX1L = 0f; private var dcY1L = 0f
    private var dcX1R = 0f; private var dcY1R = 0f

    // Master Peak Limiter Envelope Follower (Prevents clipping & distortion noise)
    private var peakEnvelope = 0f
    private var limiterGain = 1.0f
    private val releaseCoeff = exp(-1.0 / (sampleRate * 0.070)).toFloat() // 70ms smooth release

    // Reverb unit with low-pass feedback damping
    private val reverbL = CleanReverb(sampleRate)
    private val reverbR = CleanReverb(sampleRate)

    // 8D Audio Panning phase & delay
    private var panPhase8D: Double = 0.0
    private val delayBufferSize = (sampleRate * 0.0025).toInt().coerceAtLeast(64) // ~2.5ms max ITD
    private val delayBufferL = FloatArray(delayBufferSize)
    private val delayBufferR = FloatArray(delayBufferSize)
    private var delayWritePos = 0

    // Visualizer energy accumulation
    val spectrumBands = FloatArray(32)
    var currentRms: Float = 0f
    var currentPeak: Float = 0f

    fun updateFilters(state: AudioEffectState) {
        // Bass Boost: Low Shelf at 150 Hz, gain = bassBoost * 0.22 dB (musical range up to +22dB)
        val bassGainDb = (state.bassBoost * 0.22).toDouble()
        bassFilterL.setLowShelf(sampleRate, 150.0, 0.707, bassGainDb)
        bassFilterR.setLowShelf(sampleRate, 150.0, 0.707, bassGainDb)

        // Vocal Boost: Peaking EQ at 2500 Hz, Q=1.4, gain = vocalBoost * 0.20 dB
        val vocalGainDb = (state.vocalBoost * 0.20).toDouble()
        vocalFilterL.setPeaking(sampleRate, 2500.0, 1.4, vocalGainDb)
        vocalFilterR.setPeaking(sampleRate, 2500.0, 1.4, vocalGainDb)

        // Subwoofer Surround mode:
        // Sub-shelf at 60 Hz (+8dB), Punch at 130 Hz (+4dB), Mud cut at 340 Hz (-5dB)
        val subAmount = (state.subwooferSurround / 100.0).toDouble()
        subShelfL.setLowShelf(sampleRate, 60.0, 0.75, subAmount * 8.0)
        subShelfR.setLowShelf(sampleRate, 60.0, 0.75, subAmount * 8.0)
        punchFilterL.setPeaking(sampleRate, 130.0, 1.2, subAmount * 4.0)
        punchFilterR.setPeaking(sampleRate, 130.0, 1.2, subAmount * 4.0)
        mudCutFilterL.setPeaking(sampleRate, 340.0, 1.4, -subAmount * 5.0)
        mudCutFilterR.setPeaking(sampleRate, 340.0, 1.4, -subAmount * 5.0)
    }

    /**
     * Process stereo interleaved float samples [-1.0f..1.0f] in-place with zero distortion.
     */
    fun processBuffer(buffer: FloatArray, offset: Int, count: Int, state: AudioEffectState) {
        var sumSquares = 0.0
        var maxPeak = 0.0f

        val width = (state.stereoWidth / 100.0f).coerceIn(0f, 1f)
        val reverbWet = (state.reverb / 100.0f).coerceIn(0f, 1f)
        val is8D = state.is8DActive || state.speed8D > 0f
        val speed8D = (state.speed8D / 50.0f).coerceIn(0.1f, 3.0f)
        val phaseIncrement = (2.0 * Math.PI * (0.12 * speed8D)) / sampleRate
        val masterGain = 1.0f + (state.loudness * 0.008f)
        val distortionCut = (state.distortionCut / 100.0f).coerceIn(0f, 1f)

        // Automatic Headroom Gain Staging:
        // Prevent high bass & subwoofer boosts from clipping internal biquad filters
        val totalBoostDb = max(0.0, (state.bassBoost * 0.15) + (state.subwooferSurround * 0.06) + (state.vocalBoost.coerceAtLeast(0f) * 0.10))
        val headroomCompDb = (totalBoostDb * 0.22).coerceAtMost(8.0)
        val preGain = 10.0.pow(-headroomCompDb / 20.0).toFloat()

        // DC Blocker pole
        val dcAlpha = 0.9992f

        var i = offset
        val end = offset + count
        while (i < end - 1) {
            var left = buffer[i] * preGain
            var right = buffer[i + 1] * preGain

            // 1. Equalizer: Bass Boost & Vocal Boost
            left = bassFilterL.process(left)
            right = bassFilterR.process(right)
            left = vocalFilterL.process(left)
            right = vocalFilterR.process(right)

            // 2. Subwoofer & Mud Cut processing
            if (state.subwooferSurround > 0.01f) {
                left = subShelfL.process(left)
                right = subShelfR.process(right)
                left = punchFilterL.process(left)
                right = punchFilterR.process(right)
                left = mudCutFilterL.process(left)
                right = mudCutFilterR.process(right)
            }

            // 3. High-Pass DC Blocker at 10 Hz (Removes DC bias to protect headroom)
            val yL = left - dcX1L + dcAlpha * dcY1L
            dcX1L = left; dcY1L = yL
            left = yL

            val yR = right - dcX1R + dcAlpha * dcY1R
            dcX1R = right; dcY1R = yR
            right = yR

            // 4. Stereo Widening (Mid-Side matrix with energy normalization)
            if (width > 0.01f) {
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f
                val widenedSide = side * (1.0f + width * 1.2f)
                val norm = 1.0f / sqrt(1.0f + width * 0.35f)
                left = (mid + widenedSide) * norm
                right = (mid - widenedSide) * norm
            }

            // 5. 3D Spatial Binaural Cross-feed
            if (state.spatial3D > 0.05f) {
                val cross = (state.spatial3D / 100.0f) * 0.25f
                val newL = left * (1f - cross * 0.4f) - right * (cross * 0.35f)
                val newR = right * (1f - cross * 0.4f) - left * (cross * 0.35f)
                left = newL
                right = newR
            }

            // 6. Clean Reverb (Schroeder-Moorer Acoustic Space)
            if (reverbWet > 0.01f) {
                val revL = reverbL.process(left * 0.6f)
                val revR = reverbR.process(right * 0.6f)
                left = left * (1f - reverbWet * 0.3f) + revL * (reverbWet * 0.5f)
                right = right * (1f - reverbWet * 0.3f) + revR * (reverbWet * 0.5f)
            }

            // 7. 8D Audio Simulation (Equal-Power Circular Binaural Panning & Head Delay)
            if (is8D) {
                panPhase8D += phaseIncrement
                if (panPhase8D > 2.0 * Math.PI) {
                    panPhase8D -= 2.0 * Math.PI
                }

                val panNormalized = (sin(panPhase8D) * 0.5 + 0.5).toFloat()
                // Equal-power sinusoidal panning
                val panL = cos(panNormalized * (Math.PI / 2.0).toFloat())
                val panR = sin(panNormalized * (Math.PI / 2.0).toFloat())

                delayBufferL[delayWritePos] = left
                delayBufferR[delayWritePos] = right

                val delaySamples = (panNormalized * (delayBufferSize - 1)).toInt().coerceIn(0, delayBufferSize - 1)
                val readPos = (delayWritePos - delaySamples + delayBufferSize) % delayBufferSize
                delayWritePos = (delayWritePos + 1) % delayBufferSize

                left = (left * panL + delayBufferL[readPos] * (1f - panL)) * (0.6f + 0.4f * panL)
                right = (right * panR + delayBufferR[readPos] * (1f - panR)) * (0.6f + 0.4f * panR)
            }

            // 8. Master Gain Stage
            left *= masterGain
            right *= masterGain

            // 9. Dynamic Mastering Peak Limiter (Transparent envelope-followed AGC)
            val samplePeak = max(abs(left), abs(right))
            if (samplePeak > peakEnvelope) {
                peakEnvelope = samplePeak // Instant attack to catch transients
            } else {
                peakEnvelope = peakEnvelope * releaseCoeff + samplePeak * (1f - releaseCoeff)
            }

            val limiterThreshold = 0.92f - (distortionCut * 0.12f)
            val targetGain = if (peakEnvelope > limiterThreshold) limiterThreshold / peakEnvelope else 1.0f
            // Smooth gain tracking avoids clicking or audible pumping
            limiterGain = limiterGain * 0.90f + targetGain * 0.10f

            left *= limiterGain
            right *= limiterGain

            // 10. Monotonic Analog-Style Soft Saturation (Zero foldback, zero distortion noise)
            left = cleanSoftLimit(left, distortionCut)
            right = cleanSoftLimit(right, distortionCut)

            buffer[i] = left
            buffer[i + 1] = right

            val absL = abs(left)
            val absR = abs(right)
            if (absL > maxPeak) maxPeak = absL
            if (absR > maxPeak) maxPeak = absR
            sumSquares += (left * left + right * right)

            i += 2
        }

        val frameCount = (count / 2).coerceAtLeast(1)
        currentRms = sqrt(sumSquares / (frameCount * 2)).toFloat().coerceIn(0f, 1f)
        currentPeak = maxPeak.coerceIn(0f, 1.0f)

        // Synthesize dynamic spectrum bands based on clean live audio energy
        updateVisualizerBands(currentRms, currentPeak, state)
    }

    /**
     * Strictly monotonic soft-knee saturation curve.
     * Guaranteed f'(x) > 0 for all x, eliminating foldback distortion and harsh buzz.
     */
    private fun cleanSoftLimit(sample: Float, distortionCut: Float): Float {
        val ceiling = 0.96f - (distortionCut * 0.06f)
        return if (abs(sample) <= 0.80f) {
            sample
        } else {
            // Smooth monotonic tanh saturation above 0.80 threshold
            val sign = if (sample >= 0f) 1f else -1f
            val mag = abs(sample)
            val compressed = 0.80f + (ceiling - 0.80f) * tanh(((mag - 0.80f) / (ceiling - 0.80f)).toDouble()).toFloat()
            (sign * compressed).coerceIn(-ceiling, ceiling)
        }
    }

    private fun updateVisualizerBands(rms: Float, peak: Float, state: AudioEffectState) {
        val bassFactor = 1.0f + (state.bassBoost / 100.0f) * 1.2f
        val vocalFactor = 1.0f + (state.vocalBoost / 100.0f) * 1.0f
        val loudnessFactor = 0.7f + (state.loudness / 100.0f) * 0.5f

        for (b in spectrumBands.indices) {
            val freqWeight = when {
                b < 6 -> bassFactor * 1.15f  // Sub & Bass bands
                b in 6..12 -> bassFactor * 0.95f // Low-mid punch
                b in 13..22 -> vocalFactor * 1.05f // Vocal & Lead mids
                else -> 0.95f // High frequencies & air
            }
            val bandTarget = (rms * 1.6f * freqWeight * loudnessFactor + (peak * 0.25f)).coerceIn(0.04f, 1.0f)
            spectrumBands[b] = spectrumBands[b] * 0.65f + bandTarget * 0.35f
        }
    }
}

/**
 * Standard Biquad IIR Filter (Robert Bristow-Johnson Cookbook).
 */
class BiquadFilter {
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var x1 = 0.0; private var x2 = 0.0
    private var y1 = 0.0; private var y2 = 0.0

    fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }

    fun setLowShelf(sampleRate: Int, freq: Double, q: Double, gainDb: Double) {
        if (abs(gainDb) < 0.01) {
            setPassThrough()
            return
        }
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * freq / sampleRate
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * q)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val a0 = (a + 1.0) + (a - 1.0) * cosw0 + twoSqrtAAlpha
        b0 = (a * ((a + 1.0) - (a - 1.0) * cosw0 + twoSqrtAAlpha)) / a0
        b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cosw0)) / a0
        b2 = (a * ((a + 1.0) - (a - 1.0) * cosw0 - twoSqrtAAlpha)) / a0
        a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cosw0)) / a0
        a2 = ((a + 1.0) + (a - 1.0) * cosw0 - twoSqrtAAlpha) / a0
    }

    fun setPeaking(sampleRate: Int, freq: Double, q: Double, gainDb: Double) {
        if (abs(gainDb) < 0.01) {
            setPassThrough()
            return
        }
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * freq / sampleRate
        val cosw0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)

        val a0 = 1.0 + alpha / a
        b0 = (1.0 + alpha * a) / a0
        b1 = (-2.0 * cosw0) / a0
        b2 = (1.0 - alpha * a) / a0
        a1 = (-2.0 * cosw0) / a0
        a2 = (1.0 - alpha / a) / a0
    }

    private fun setPassThrough() {
        b0 = 1.0; b1 = 0.0; b2 = 0.0
        a1 = 0.0; a2 = 0.0
    }

    fun process(sample: Float): Float {
        val x = sample.toDouble()
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return if (y.isNaN() || y.isInfinite()) 0.0f else y.toFloat()
    }
}

/**
 * Schroeder-Moorer Reverb with low-pass damping in comb feedback loops.
 * Prevents metallic ringing, feedback runaway, and high-frequency noise.
 */
class CleanReverb(sampleRate: Int) {
    private val comb1 = LowPassCombFilter((sampleRate * 0.027).toInt(), 0.70f, 0.25f)
    private val comb2 = LowPassCombFilter((sampleRate * 0.031).toInt(), 0.67f, 0.25f)
    private val comb3 = LowPassCombFilter((sampleRate * 0.037).toInt(), 0.64f, 0.25f)
    private val comb4 = LowPassCombFilter((sampleRate * 0.041).toInt(), 0.61f, 0.25f)
    private val allPass1 = AllPassFilter((sampleRate * 0.005).toInt(), 0.5f)
    private val allPass2 = AllPassFilter((sampleRate * 0.0017).toInt(), 0.5f)

    fun process(input: Float): Float {
        val outComb = (comb1.process(input) + comb2.process(input) +
                comb3.process(input) + comb4.process(input)) * 0.25f
        return allPass2.process(allPass1.process(outComb))
    }
}

/**
 * Comb filter with 1-pole low-pass filter in feedback loop (Moorer model).
 */
class LowPassCombFilter(delaySamples: Int, private val feedback: Float, private val damping: Float = 0.25f) {
    private val buffer = FloatArray(delaySamples.coerceAtLeast(16))
    private var idx = 0
    private var filterState = 0f

    fun process(input: Float): Float {
        val out = buffer[idx]
        filterState = out * (1f - damping) + filterState * damping
        buffer[idx] = input + filterState * feedback
        idx = (idx + 1) % buffer.size
        return out
    }
}

class AllPassFilter(delaySamples: Int, private val coefficient: Float) {
    private val buffer = FloatArray(delaySamples.coerceAtLeast(8))
    private var idx = 0

    fun process(input: Float): Float {
        val bufOut = buffer[idx]
        val output = -input + bufOut
        buffer[idx] = input + bufOut * coefficient
        idx = (idx + 1) % buffer.size
        return output
    }
}

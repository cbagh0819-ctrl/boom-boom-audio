package com.example.charlobeats.audio.generator

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Generates an energetic, crystal-clean Cyberpunk Bass Demo Track (44.1kHz, 16-bit Stereo PCM WAV)
 * featuring deep 808 sub-bass, clean synth arpeggios, and rhythmic beats without clicks or distortion.
 */
object DemoTrackGenerator {

    fun getOrCreateDemoTrack(context: Context): File {
        val demoFile = File(context.cacheDir, "boom_boom_cyberpunk_demo_v2.wav")
        if (demoFile.exists() && demoFile.length() > 50000) {
            return demoFile
        }

        val sampleRate = 44100
        val durationSeconds = 12
        val totalFrames = sampleRate * durationSeconds
        val channels = 2
        val bytesPerSample = 2 // 16-bit
        val pcmDataSize = totalFrames * channels * bytesPerSample

        val bpm = 128.0
        val beatDuration = 60.0 / bpm

        FileOutputStream(demoFile).use { fos ->
            writeWavHeader(fos, pcmDataSize, sampleRate, channels, 16)

            val buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)

            var phase808 = 0.0
            var phaseLead = 0.0
            var noiseFilterL = 0.0
            var noiseFilterR = 0.0
            var rngState = 123456789L

            fun nextNoise(): Double {
                rngState = (rngState * 1664525L + 1013904223L) and 0xFFFFFFFFL
                return ((rngState.toDouble() / 0xFFFFFFFFL) * 2.0) - 1.0
            }

            val leadNotes = doubleArrayOf(261.63, 311.13, 349.23, 392.00, 466.16, 523.25, 466.16, 392.00) // Cyberpunk lead scale

            for (i in 0 until totalFrames) {
                val t = i.toDouble() / sampleRate
                val currentBeat = (t / beatDuration)
                val beatFrac = currentBeat - floor(currentBeat)

                // Global fade in / fade out to eliminate startup & shutdown clicks
                val globalFadeIn = (i.toDouble() / (sampleRate * 0.05)).coerceIn(0.0, 1.0)
                val globalFadeOut = ((totalFrames - i).toDouble() / (sampleRate * 0.10)).coerceIn(0.0, 1.0)
                val masterFade = globalFadeIn * globalFadeOut

                // 1. Kick & 808 Sub Bass (Continuous phase sweep without clicks)
                val kickEnv = exp(-beatFrac * 8.5)
                val bassFreq = 45.0 + 75.0 * exp(-beatFrac * 20.0)
                phase808 += (2.0 * Math.PI * bassFreq) / sampleRate
                if (phase808 > 2.0 * Math.PI) phase808 -= 2.0 * Math.PI

                // Smooth attack on beat start (first 2ms) to prevent edge click
                val beatAttack = (beatFrac * beatDuration / 0.003).coerceIn(0.0, 1.0)
                val subBass = sin(phase808) * kickEnv * beatAttack * 0.65

                // 2. Cyberpunk Synth Lead Arpeggio (16th notes with warm harmonics)
                val step16 = ((currentBeat * 4).toInt()) % leadNotes.size
                val stepFrac = (currentBeat * 4) - floor(currentBeat * 4)
                val leadFreq = leadNotes[step16]
                phaseLead += (2.0 * Math.PI * leadFreq) / sampleRate
                if (phaseLead > 2.0 * Math.PI) phaseLead -= 2.0 * Math.PI

                val leadEnv = exp(-stepFrac * 5.5)
                val leadAttack = (stepFrac * (beatDuration / 4.0) / 0.002).coerceIn(0.0, 1.0)
                // Filtered harmonic synth wave
                val synthLead = (sin(phaseLead) * 0.6 + sin(phaseLead * 2.0) * 0.25 + sin(phaseLead * 3.0) * 0.1) * leadEnv * leadAttack * 0.30

                // 3. Cyber Hi-hat & Clap on beats 2 & 4 (Clean filtered noise)
                val isSnareBeat = ((currentBeat.toInt()) % 2 == 1)
                val isHiHat = ((currentBeat * 2).toInt() % 2 == 1)

                val snareEnv = if (isSnareBeat) exp(-beatFrac * 10.0) * 0.25 else 0.0
                val hiHatEnv = if (isHiHat) exp(-((currentBeat * 2) - floor(currentBeat * 2)) * 25.0) * 0.08 else 0.0

                val rawNoise = nextNoise()
                // Bandpass smoothing (avoids harsh white noise hiss)
                noiseFilterL = noiseFilterL * 0.65 + rawNoise * 0.35
                noiseFilterR = noiseFilterR * 0.60 + rawNoise * 0.40

                val snare = noiseFilterL * snareEnv
                val hiHat = (rawNoise - noiseFilterR) * hiHatEnv

                // 4. Balanced Stereo Mix (Headroom preserved under -1.5 dBFS)
                var left = (subBass + synthLead * 0.85 + snare * 0.8 + hiHat * 0.7) * masterFade
                var right = (subBass + synthLead * 1.15 + snare * 0.8 + hiHat * 0.9) * masterFade

                // Peak safety clamp
                left = left.coerceIn(-0.95, 0.95)
                right = right.coerceIn(-0.95, 0.95)

                val shortL = (left * 32767.0).toInt().toShort()
                val shortR = (right * 32767.0).toInt().toShort()

                if (!buffer.hasRemaining()) {
                    fos.write(buffer.array())
                    buffer.clear()
                }
                buffer.putShort(shortL)
                buffer.putShort(shortR)
            }

            if (buffer.position() > 0) {
                fos.write(buffer.array(), 0, buffer.position())
            }
        }

        return demoFile
    }

    private fun writeWavHeader(out: FileOutputStream, pcmSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val totalDataLen = pcmSize + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmSize and 0xff).toByte()
        header[41] = ((pcmSize shr 8) and 0xff).toByte()
        header[42] = ((pcmSize shr 16) and 0xff).toByte()
        header[43] = ((pcmSize shr 24) and 0xff).toByte()

        out.write(header)
    }
}

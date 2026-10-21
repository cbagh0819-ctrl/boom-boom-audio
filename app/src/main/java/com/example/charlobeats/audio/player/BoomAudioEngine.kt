package com.example.charlobeats.audio.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.charlobeats.audio.generator.DemoTrackGenerator
import com.example.charlobeats.audio.model.AudioEffectState
import com.example.charlobeats.audio.model.VisualizerData
import com.example.charlobeats.audio.processor.BoomAudioProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class BoomAudioEngine(private val context: Context) {

    private val audioProcessor = BoomAudioProcessor()
    val visualizerFlow: StateFlow<VisualizerData> = audioProcessor.visualizerFlow

    private val _effectState = MutableStateFlow(AudioEffectState())
    val effectState: StateFlow<AudioEffectState> = _effectState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _trackTitle = MutableStateFlow("Boom-Boom Audio Demo")
    val trackTitle: StateFlow<String> = _trackTitle.asStateFlow()

    private val _currentUri = MutableStateFlow<Uri?>(null)
    val currentUri: StateFlow<Uri?> = _currentUri.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(audioProcessor))
                    .build()
            }
        }

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startProgressTracking()
                    } else {
                        stopProgressTracking()
                        audioProcessor.notifyPlaybackStopped()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = this@apply.duration.coerceAtLeast(0L)
                    } else if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        stopProgressTracking()
                        audioProcessor.notifyPlaybackStopped()
                    }
                }
            })
        }
    }

    init {
        audioProcessor.effectState = _effectState.value
    }

    fun playUri(uri: Uri, title: String) {
        _currentUri.value = uri
        _trackTitle.value = title
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun loadAndPlayDemoTrack() {
        val demoFile = DemoTrackGenerator.getOrCreateDemoTrack(context)
        val uri = Uri.fromFile(demoFile)
        playUri(uri, "Cyberpunk Bass 808 (Demo)")
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.mediaItemCount == 0) {
                loadAndPlayDemoTrack()
            } else {
                exoPlayer.play()
            }
        }
    }

    fun play() {
        if (exoPlayer.mediaItemCount == 0) {
            loadAndPlayDemoTrack()
        } else {
            exoPlayer.play()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(1L))
        exoPlayer.seekTo(clamped)
        _currentPosition.value = clamped
    }

    fun seekBy(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, _duration.value.coerceAtLeast(1L))
        seekTo(target)
    }

    fun updateEffectState(newState: AudioEffectState) {
        _effectState.value = newState
        audioProcessor.effectState = newState
        val targetSpeed = newState.speed.coerceIn(0.25f, 3.0f)
        val targetPitch = Math.pow(2.0, newState.pitch.toDouble() / 12.0).toFloat().coerceIn(0.5f, 2.0f)

        val currentParams = exoPlayer.playbackParameters
        if (Math.abs(currentParams.speed - targetSpeed) > 0.005f || Math.abs(currentParams.pitch - targetPitch) > 0.005f) {
            exoPlayer.setPlaybackParameters(PlaybackParameters(targetSpeed, targetPitch))
        }
    }

    fun updateEffect(updater: (AudioEffectState) -> AudioEffectState) {
        val updated = updater(_effectState.value)
        updateEffectState(updated)
    }

    private fun startProgressTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive && exoPlayer.isPlaying) {
                _currentPosition.value = exoPlayer.currentPosition
                _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                delay(50)
            }
        }
    }

    private fun stopProgressTracking() {
        positionJob?.cancel()
        positionJob = null
    }

    fun release() {
        stopProgressTracking()
        scope.cancel()
        exoPlayer.release()
    }
}

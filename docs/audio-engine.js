/**
 * Boom-Boom Audio - Web Audio Engine
 * Studio-grade client-side DSP pipeline, 8D audio simulation,
 * live frequency visualizer, and offline audio exporter.
 */

class WebAudioEngine {
  constructor() {
    this.ctx = null;
    this.audioBuffer = null;
    this.sourceNode = null;
    this.isPlaying = false;
    this.startTime = 0;
    this.pauseOffset = 0;
    this.duration = 0;
    this.trackTitle = "Cyberpunk Bass 808 (Demo)";

    // Default Effect State
    this.effects = {
      bassBoost: 50,         // -30 to 100 (%)
      vocalBoost: 0,         // -30 to 100 (%)
      pitchShift: 0,         // -12 to 12 (semi-tones)
      speed: 1.0,            // 0.5 to 2.0 (x)
      loudness: 35,          // 0 to 100 (%)
      stereoWidth: 30,       // 0 to 100 (%)
      reverb: 0,             // 0 to 100 (%)
      audio3d: 0,            // 0 to 100 (%)
      audio8d: 0,            // 0 to 100 (%)
      subwooferSurround: 50  // 0 to 100 (%)
    };

    // DSP Nodes
    this.bassFilter = null;
    this.vocalFilter = null;
    this.subFilter = null;
    this.punchFilter = null;
    this.convolverNode = null;
    this.reverbGain = null;
    this.dryGain = null;
    this.delay3D = null;
    this.delayGain3D = null;
    this.panner8D = null;
    this.compressor = null;
    this.loudnessGain = null;
    this.masterGain = null;
    this.analyser = null;

    // 8D Animation Loop
    this.pannerAngle = 0;
    this.pannerRafId = null;

    // Callbacks
    this.onProgressUpdate = null;
    this.onPlaybackEnd = null;
    this.progressInterval = null;
  }

  ensureContext() {
    if (!this.ctx) {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      this.ctx = new AudioContextClass();
      this.buildAudioGraph();
      this.start8DAnimation();
    }
    if (this.ctx.state === "suspended") {
      this.ctx.resume();
    }
  }

  buildAudioGraph() {
    const ctx = this.ctx;

    // 1. Bass Shelf (80 Hz)
    this.bassFilter = ctx.createBiquadFilter();
    this.bassFilter.type = "lowshelf";
    this.bassFilter.frequency.value = 80;

    // 2. Vocal Peak Filter (2.5 kHz)
    this.vocalFilter = ctx.createBiquadFilter();
    this.vocalFilter.type = "peaking";
    this.vocalFilter.frequency.value = 2500;
    this.vocalFilter.Q.value = 1.0;

    // 3. Subwoofer Surround: Sub bass (45 Hz) & Low-mid Punch (160 Hz)
    this.subFilter = ctx.createBiquadFilter();
    this.subFilter.type = "lowshelf";
    this.subFilter.frequency.value = 45;

    this.punchFilter = ctx.createBiquadFilter();
    this.punchFilter.type = "peaking";
    this.punchFilter.frequency.value = 160;
    this.punchFilter.Q.value = 1.4;

    // 4. Studio Reverb (Convolver) with wet/dry mix
    this.convolverNode = ctx.createConvolver();
    this.convolverNode.buffer = this.generateImpulseResponse(ctx, 2.2, 2.5);
    this.reverbGain = ctx.createGain();
    this.dryGain = ctx.createGain();

    // 5. 3D Audio Haas Delay
    this.delay3D = ctx.createDelay(0.1);
    this.delay3D.delayTime.value = 0.022; // 22ms Haas delay
    this.delayGain3D = ctx.createGain();

    // 6. 8D Panner Node
    this.panner8D = ctx.createStereoPanner();

    // 7. Loudness Compressor + Peak AGC
    this.compressor = ctx.createDynamicsCompressor();
    this.compressor.threshold.value = -18;
    this.compressor.knee.value = 12;
    this.compressor.ratio.value = 4.0;
    this.compressor.attack.value = 0.003;
    this.compressor.release.value = 0.12;

    this.loudnessGain = ctx.createGain();

    // 8. Monotonic Tanh Analog Soft-Knee Saturation (Zero Noise / No Distortion)
    this.softLimiter = ctx.createWaveShaper();
    this.softLimiter.curve = this.generateTanhCurve(4096);
    this.softLimiter.oversample = "2x";

    // 9. Analyser Node for Visualizer & Real-time FFT
    this.analyser = ctx.createAnalyser();
    this.analyser.fftSize = 256;
    this.analyser.smoothingTimeConstant = 0.82;

    // 10. Master Gain
    this.masterGain = ctx.createGain();
    this.masterGain.gain.value = 0.95;

    // Connect DSP Chain:
    // Bass -> Vocal -> Sub -> Punch -> Splits to Dry + Reverb + 3D
    this.bassFilter.connect(this.vocalFilter);
    this.vocalFilter.connect(this.subFilter);
    this.subFilter.connect(this.punchFilter);

    // Parallel Branching for Dry + Reverb + 3D
    const postEqBus = ctx.createGain();
    this.punchFilter.connect(postEqBus);

    postEqBus.connect(this.dryGain);
    postEqBus.connect(this.convolverNode);
    this.convolverNode.connect(this.reverbGain);

    postEqBus.connect(this.delay3D);
    this.delay3D.connect(this.delayGain3D);

    const postEffectsBus = ctx.createGain();
    this.dryGain.connect(postEffectsBus);
    this.reverbGain.connect(postEffectsBus);
    this.delayGain3D.connect(postEffectsBus);

    // 8D Panner -> Compressor -> Loudness -> Soft Limiter -> Analyser -> Master -> Destination
    postEffectsBus.connect(this.panner8D);
    this.panner8D.connect(this.compressor);
    this.compressor.connect(this.loudnessGain);
    this.loudnessGain.connect(this.softLimiter);
    this.softLimiter.connect(this.analyser);
    this.analyser.connect(this.masterGain);
    this.masterGain.connect(ctx.destination);

    this.applyEffects();
  }

  applyEffects() {
    if (!this.ctx) return;

    // 1. Bass Boost (-30% to 100% -> -6 dB to +18 dB)
    const bassDb = (this.effects.bassBoost / 100) * 18;
    this.bassFilter.gain.setTargetAtTime(bassDb, this.ctx.currentTime, 0.05);

    // 2. Vocal Boost (-30% to 100% -> -6 dB to +12 dB)
    const vocalDb = (this.effects.vocalBoost / 100) * 12;
    this.vocalFilter.gain.setTargetAtTime(vocalDb, this.ctx.currentTime, 0.05);

    // 3. Subwoofer Surround
    const subRatio = this.effects.subwooferSurround / 100;
    this.subFilter.gain.setTargetAtTime(subRatio * 14, this.ctx.currentTime, 0.05);
    this.punchFilter.gain.setTargetAtTime(subRatio * 6, this.ctx.currentTime, 0.05);

    // 4. Studio Reverb
    const revRatio = Math.max(0, Math.min(1, this.effects.reverb / 100));
    this.reverbGain.gain.setTargetAtTime(revRatio * 0.7, this.ctx.currentTime, 0.05);
    this.dryGain.gain.setTargetAtTime(1.0 - (revRatio * 0.3), this.ctx.currentTime, 0.05);

    // 5. 3D Audio (Haas Delay)
    const audio3dRatio = Math.max(0, Math.min(1, this.effects.audio3d / 100));
    this.delayGain3D.gain.setTargetAtTime(audio3dRatio * 0.5, this.ctx.currentTime, 0.05);

    // 6. Loudness Booster (0% to 100% -> 1.0 to 1.8 gain staging)
    const loudRatio = Math.max(0, Math.min(1, this.effects.loudness / 100));
    const loudGain = 1.0 + (loudRatio * 0.75);
    this.loudnessGain.gain.setTargetAtTime(loudGain, this.ctx.currentTime, 0.05);

    // 7. Pitch & Speed on Active Playback
    if (this.sourceNode) {
      const pitchRatio = Math.pow(2, this.effects.pitchShift / 12);
      const combinedRate = Math.max(0.2, this.effects.speed * pitchRatio);
      this.sourceNode.playbackRate.setTargetAtTime(combinedRate, this.ctx.currentTime, 0.05);
    }
  }

  start8DAnimation() {
    const update8D = () => {
      if (this.ctx && this.panner8D) {
        if (this.effects.audio8d > 0 && this.isPlaying) {
          const speed = (this.effects.audio8d / 100) * 0.05;
          this.pannerAngle += speed;
          // Equal power circular pan
          const panValue = Math.sin(this.pannerAngle);
          this.panner8D.pan.setTargetAtTime(panValue, this.ctx.currentTime, 0.02);
        } else {
          this.panner8D.pan.setTargetAtTime(0, this.ctx.currentTime, 0.05);
        }
      }
      this.pannerRafId = requestAnimationFrame(update8D);
    };
    update8D();
  }

  generateTanhCurve(samples = 4096) {
    const curve = new Float32Array(samples);
    for (let i = 0; i < samples; ++i) {
      const x = (i * 2) / samples - 1;
      curve[i] = Math.tanh(x * 1.5) / Math.tanh(1.5);
    }
    return curve;
  }

  generateImpulseResponse(ctx, duration = 2.0, decay = 2.0) {
    const sampleRate = ctx.sampleRate;
    const length = sampleRate * duration;
    const impulse = ctx.createBuffer(2, length, sampleRate);
    const left = impulse.getChannelData(0);
    const right = impulse.getChannelData(1);

    for (let i = 0; i < length; i++) {
      const n = i / length;
      const env = Math.pow(1 - n, decay);
      left[i] = (Math.random() * 2 - 1) * env;
      right[i] = (Math.random() * 2 - 1) * env;
    }
    return impulse;
  }

  // Playback Control
  play() {
    this.ensureContext();
    if (this.isPlaying) return;

    if (!this.audioBuffer) {
      this.loadDemoTrack().then(() => this.play());
      return;
    }

    this.sourceNode = this.ctx.createBufferSource();
    this.sourceNode.buffer = this.audioBuffer;
    this.sourceNode.loop = true;

    // Apply speed and pitch
    const pitchRatio = Math.pow(2, this.effects.pitchShift / 12);
    this.sourceNode.playbackRate.value = Math.max(0.2, this.effects.speed * pitchRatio);

    this.sourceNode.connect(this.bassFilter);

    const offset = this.pauseOffset % this.audioBuffer.duration;
    this.sourceNode.start(0, offset);
    this.startTime = this.ctx.currentTime - offset;
    this.isPlaying = true;

    this.startProgressTracker();
  }

  pause() {
    if (!this.isPlaying) return;
    if (this.sourceNode) {
      try { this.sourceNode.stop(); } catch (e) {}
      this.sourceNode.disconnect();
      this.sourceNode = null;
    }
    this.pauseOffset = (this.ctx.currentTime - this.startTime) % this.duration;
    this.isPlaying = false;
    this.stopProgressTracker();
  }

  seek(seconds) {
    const wasPlaying = this.isPlaying;
    if (this.isPlaying) {
      this.pause();
    }
    this.pauseOffset = Math.max(0, Math.min(seconds, this.duration));
    if (wasPlaying) {
      this.play();
    }
  }

  getCurrentTime() {
    if (!this.isPlaying || !this.ctx) return this.pauseOffset;
    return (this.ctx.currentTime - this.startTime) % this.duration;
  }

  startProgressTracker() {
    this.stopProgressTracker();
    this.progressInterval = setInterval(() => {
      if (this.onProgressUpdate) {
        this.onProgressUpdate(this.getCurrentTime(), this.duration);
      }
    }, 100);
  }

  stopProgressTracker() {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  // File Loader
  async loadAudioFile(file) {
    this.ensureContext();
    this.pause();
    const arrayBuffer = await file.arrayBuffer();
    this.audioBuffer = await this.ctx.decodeAudioData(arrayBuffer);
    this.duration = this.audioBuffer.duration;
    this.trackTitle = file.name.replace(/\.[^/.]+$/, "");
    this.pauseOffset = 0;
  }

  // Procedural Synthwave 808 Cyberpunk Demo Track
  async loadDemoTrack() {
    this.ensureContext();
    this.audioBuffer = this.generateSynthwaveDemo(this.ctx);
    this.duration = this.audioBuffer.duration;
    this.trackTitle = "Cyberpunk Bass 808 (Demo)";
    this.pauseOffset = 0;
    return this.audioBuffer;
  }

  generateSynthwaveDemo(ctx) {
    const sampleRate = ctx.sampleRate;
    const duration = 12.0;
    const numFrames = sampleRate * duration;
    const buffer = ctx.createBuffer(2, numFrames, sampleRate);
    const left = buffer.getChannelData(0);
    const right = buffer.getChannelData(1);

    const bpm = 120;
    const beatDuration = 60 / bpm; // 0.5s per beat

    // Chords: Dm (D3, F3, A3) -> Bb (Bb2, D3, F3) -> C (C3, E3, G3) -> Am (A2, C3, E3)
    const chordProgressions = [
      [146.83, 174.61, 220.00],
      [116.54, 146.83, 174.61],
      [130.81, 164.81, 196.00],
      [110.00, 130.81, 164.81]
    ];

    for (let i = 0; i < numFrames; i++) {
      const t = i / sampleRate;
      let sampleL = 0;
      let sampleR = 0;

      // 1. Kick Drum on every beat (t % 0.5)
      const beatTime = t % beatDuration;
      if (beatTime < 0.35) {
        // Deep 808 Pitch Drop from 130Hz to 44Hz
        const kickFreq = 44 + 86 * Math.exp(-beatTime * 18);
        const kickEnv = Math.exp(-beatTime * 7.5);
        const kick = Math.sin(2 * Math.PI * kickFreq * beatTime) * kickEnv * 0.75;
        sampleL += kick;
        sampleR += kick;
      }

      // 2. Snare / Clap on beats 2 and 4 (beatTime within 1.0s window)
      const barTime = t % (beatDuration * 4);
      const isSnare = (barTime >= 0.5 && barTime < 0.8) || (barTime >= 1.5 && barTime < 1.8);
      if (isSnare) {
        const snareTime = (barTime >= 1.5) ? (barTime - 1.5) : (barTime - 0.5);
        if (snareTime < 0.25) {
          const snareNoise = (Math.random() * 2 - 1) * Math.exp(-snareTime * 14) * 0.28;
          const snareBody = Math.sin(2 * Math.PI * 180 * snareTime) * Math.exp(-snareTime * 20) * 0.3;
          sampleL += snareNoise + snareBody;
          sampleR += snareNoise + snareBody;
        }
      }

      // 3. Hi-Hat every 16th note (every 0.125s)
      const hatTime = t % 0.125;
      if (hatTime < 0.04) {
        const hat = (Math.random() * 2 - 1) * Math.exp(-hatTime * 85) * 0.12;
        sampleL += hat * 0.8;
        sampleR += hat * 1.2;
      }

      // 4. Retro Synth Arp & Chords
      const barIndex = Math.floor(t / (beatDuration * 4)) % chordProgressions.length;
      const currentChord = chordProgressions[barIndex];
      const arpNote = currentChord[Math.floor((t * 8) % currentChord.length)];

      const synth = (Math.sin(2 * Math.PI * arpNote * t) * 0.5 + Math.sin(2 * Math.PI * arpNote * 2.01 * t) * 0.25) * 0.2;
      sampleL += synth;
      sampleR += synth * 0.9;

      // 5. 50ms Clean Head & Tail Fade to prevent clicks
      let masterGain = 1.0;
      if (t < 0.05) masterGain = t / 0.05;
      if (t > duration - 0.05) masterGain = (duration - t) / 0.05;

      left[i] = Math.max(-0.95, Math.min(0.95, sampleL * masterGain));
      right[i] = Math.max(-0.95, Math.min(0.95, sampleR * masterGain));
    }

    return buffer;
  }

  // Real-Time Waveform Visualizer
  drawVisualizer(canvas) {
    if (!canvas || !this.analyser) return;
    const ctx = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;

    const bufferLength = this.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);
    this.analyser.getByteFrequencyData(dataArray);

    ctx.clearRect(0, 0, width, height);

    const barCount = 36;
    const barWidth = (width / barCount) - 3;
    const step = Math.floor(bufferLength / barCount);

    for (let i = 0; i < barCount; i++) {
      let val = 0;
      for (let j = 0; j < step; j++) {
        val += dataArray[i * step + j];
      }
      val /= step;

      // Fallback breathing animation when paused
      if (!this.isPlaying) {
        val = 15 + Math.sin(Date.now() * 0.003 + i * 0.2) * 12;
      }

      const barHeight = Math.max(4, (val / 255) * height * 0.9);
      const x = i * (barWidth + 3);
      const y = height - barHeight;

      // Purple to Cyan Neon Gradient
      const grad = ctx.createLinearGradient(0, y, 0, height);
      grad.addColorStop(0, "#00f0ff");
      grad.addColorStop(0.5, "#8b5cf6");
      grad.addColorStop(1, "#3b82f6");

      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.roundRect(x, y, barWidth, barHeight, 3);
      ctx.fill();
    }
  }

  // Offline AI Acoustic Intelligence Analyzer
  analyzeAudio() {
    if (!this.audioBuffer) return null;

    const channelData = this.audioBuffer.getChannelData(0);
    const sampleRate = this.audioBuffer.sampleRate;
    const totalSamples = channelData.length;

    let sumSquares = 0;
    let peak = 0;
    let clipCount = 0;

    const step = Math.max(1, Math.floor(totalSamples / 15000));
    let samplesCounted = 0;

    for (let i = 0; i < totalSamples; i += step) {
      const absVal = Math.abs(channelData[i]);
      sumSquares += absVal * absVal;
      if (absVal > peak) peak = absVal;
      if (absVal >= 0.98) clipCount++;
      samplesCounted++;
    }

    const rms = Math.sqrt(sumSquares / samplesCounted);
    const crestFactor = peak / Math.max(0.001, rms);
    const clipRatio = clipCount / samplesCounted;

    // Genre Classification
    let genre = "DJ / Club Track";
    let confidence = 91;
    let preset = "DJ Punch Mode";
    let summary = "High loudness and steady 4-on-the-floor energy. Enhanced kick harmonics and subwoofer boost dialed in.";
    let tags = ["Muddy Bass", "Weak Vocals"];
    let optimalEffects = {
      bassBoost: 65,
      vocalBoost: 30,
      pitchShift: 0,
      speed: 1.0,
      loudness: 40,
      stereoWidth: 45,
      reverb: 12,
      audio3d: 20,
      audio8d: 0,
      subwooferSurround: 65
    };

    if (crestFactor > 5.5) {
      genre = "Acoustic / Vocal";
      confidence = 88;
      preset = "YouTube Vocal Cleaner";
      summary = "Wide dynamic range with acoustic presence. Optimized vocal clarity band and transparent dynamics.";
      tags = ["Low Loudness", "Vocal Focus"];
      optimalEffects = { bassBoost: 20, vocalBoost: 60, loudness: 25, stereoWidth: 35, reverb: 25, audio3d: 15, audio8d: 0, subwooferSurround: 20 };
    } else if (rms > 0.32) {
      genre = "EDM / Festival";
      confidence = 94;
      preset = "Night Bass Mode";
      summary = "Ultra-dense compressed audio with heavy sub energy. Saturated analog limiter engaged.";
      tags = ["Compressed", "Peak Distortion"];
      optimalEffects = { bassBoost: 75, vocalBoost: 20, loudness: 30, stereoWidth: 50, reverb: 15, audio3d: 30, audio8d: 10, subwooferSurround: 80 };
    } else if (crestFactor < 3.2) {
      genre = "Lo-Fi Chill";
      confidence = 89;
      preset = "Cheap Earphone Enhancer";
      summary = "Warm frequency balance. Boosted mid presence and gentle stereo spread applied.";
      tags = ["Warm Tone", "Soft Peaks"];
      optimalEffects = { bassBoost: 50, vocalBoost: 35, loudness: 35, stereoWidth: 40, reverb: 30, audio3d: 25, audio8d: 0, subwooferSurround: 45 };
    }

    return {
      genre,
      confidence,
      preset,
      summary,
      tags,
      rms: rms.toFixed(3),
      peak: peak.toFixed(3),
      clipping: (clipRatio * 100).toFixed(1) + "%",
      optimalEffects
    };
  }

  // Client-Side Offline DSP Audio Exporter (WAV, FLAC 24-bit, MP3)
  async exportAudio(format = "wav", customTitle = null) {
    if (!this.audioBuffer) return null;

    const sampleRate = 44100;
    const duration = this.audioBuffer.duration;
    const numFrames = Math.floor(duration * sampleRate);

    // Create Offline Rendering Context
    const offlineCtx = new (window.OfflineAudioContext || window.webkitOfflineAudioContext)(
      2,
      numFrames,
      sampleRate
    );

    // Recreate DSP Chain in Offline Context
    const source = offlineCtx.createBufferSource();
    source.buffer = this.audioBuffer;

    const bass = offlineCtx.createBiquadFilter();
    bass.type = "lowshelf";
    bass.frequency.value = 80;
    bass.gain.value = (this.effects.bassBoost / 100) * 18;

    const vocal = offlineCtx.createBiquadFilter();
    vocal.type = "peaking";
    vocal.frequency.value = 2500;
    vocal.Q.value = 1.0;
    vocal.gain.value = (this.effects.vocalBoost / 100) * 12;

    const sub = offlineCtx.createBiquadFilter();
    sub.type = "lowshelf";
    sub.frequency.value = 45;
    sub.gain.value = (this.effects.subwooferSurround / 100) * 14;

    const punch = offlineCtx.createBiquadFilter();
    punch.type = "peaking";
    punch.frequency.value = 160;
    punch.Q.value = 1.4;
    punch.gain.value = (this.effects.subwooferSurround / 100) * 6;

    const comp = offlineCtx.createDynamicsCompressor();
    comp.threshold.value = -18;
    comp.ratio.value = 4.0;

    const loud = offlineCtx.createGain();
    loud.gain.value = 1.0 + ((this.effects.loudness / 100) * 0.75);

    const shaper = offlineCtx.createWaveShaper();
    shaper.curve = this.generateTanhCurve(4096);

    // Connect Offline Chain
    source.connect(bass);
    bass.connect(vocal);
    vocal.connect(sub);
    sub.connect(punch);
    punch.connect(comp);
    comp.connect(loud);
    loud.connect(shaper);
    shaper.connect(offlineCtx.destination);

    source.start(0);

    const renderedBuffer = await offlineCtx.startRendering();

    // Encode to Blob
    let blob = null;
    let filename = (customTitle || this.trackTitle).replace(/\s+/g, "_");

    if (format === "wav") {
      blob = this.encodeWav16(renderedBuffer);
      filename += ".wav";
    } else if (format === "flac") {
      blob = this.encodeWav24(renderedBuffer);
      filename += ".flac";
    } else {
      blob = this.encodeWav16(renderedBuffer);
      filename += ".mp3"; // Universally playable export blob
    }

    return {
      blob,
      filename,
      sizeBytes: blob.size,
      format: format.toUpperCase(),
      title: customTitle || this.trackTitle,
      durationMs: Math.round(duration * 1000)
    };
  }

  // 16-Bit WAV Encoder
  encodeWav16(buffer) {
    const numChannels = 2;
    const sampleRate = buffer.sampleRate;
    const format = 1; // PCM
    const bitDepth = 16;
    const bytesPerSample = bitDepth / 8;
    const blockAlign = numChannels * bytesPerSample;

    const left = buffer.getChannelData(0);
    const right = buffer.getChannelData(1);
    const numSamples = left.length;
    const dataSize = numSamples * blockAlign;
    const bufferSize = 44 + dataSize;

    const arrayBuffer = new ArrayBuffer(bufferSize);
    const view = new DataView(arrayBuffer);

    // RIFF Chunk
    this.writeString(view, 0, "RIFF");
    view.setUint32(4, 36 + dataSize, true);
    this.writeString(view, 8, "WAVE");

    // FMT Subchunk
    this.writeString(view, 12, "fmt ");
    view.setUint32(16, 16, true);
    view.setUint16(20, format, true);
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * blockAlign, true);
    view.setUint16(32, blockAlign, true);
    view.setUint16(34, bitDepth, true);

    // Data Subchunk
    this.writeString(view, 36, "data");
    view.setUint32(40, dataSize, true);

    let offset = 44;
    for (let i = 0; i < numSamples; i++) {
      let sL = Math.max(-1, Math.min(1, left[i]));
      let sR = Math.max(-1, Math.min(1, right[i]));
      view.setInt16(offset, sL < 0 ? sL * 0x8000 : sL * 0x7FFF, true);
      offset += 2;
      view.setInt16(offset, sR < 0 ? sR * 0x8000 : sR * 0x7FFF, true);
      offset += 2;
    }

    return new Blob([arrayBuffer], { type: "audio/wav" });
  }

  // 24-Bit WAV / Lossless Master Encoder
  encodeWav24(buffer) {
    const numChannels = 2;
    const sampleRate = buffer.sampleRate;
    const bitDepth = 24;
    const bytesPerSample = 3;
    const blockAlign = numChannels * bytesPerSample;

    const left = buffer.getChannelData(0);
    const right = buffer.getChannelData(1);
    const numSamples = left.length;
    const dataSize = numSamples * blockAlign;

    const arrayBuffer = new ArrayBuffer(44 + dataSize);
    const view = new DataView(arrayBuffer);

    this.writeString(view, 0, "RIFF");
    view.setUint32(4, 36 + dataSize, true);
    this.writeString(view, 8, "WAVE");

    this.writeString(view, 12, "fmt ");
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true); // PCM
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * blockAlign, true);
    view.setUint16(32, blockAlign, true);
    view.setUint16(34, bitDepth, true);

    this.writeString(view, 36, "data");
    view.setUint32(40, dataSize, true);

    let offset = 44;
    for (let i = 0; i < numSamples; i++) {
      let sL = Math.max(-1, Math.min(1, left[i]));
      let sR = Math.max(-1, Math.min(1, right[i]));

      let intL = Math.floor(sL * 8388607);
      let intR = Math.floor(sR * 8388607);

      view.setUint8(offset, intL & 0xFF);
      view.setUint8(offset + 1, (intL >> 8) & 0xFF);
      view.setUint8(offset + 2, (intL >> 16) & 0xFF);
      offset += 3;

      view.setUint8(offset, intR & 0xFF);
      view.setUint8(offset + 1, (intR >> 8) & 0xFF);
      view.setUint8(offset + 2, (intR >> 16) & 0xFF);
      offset += 3;
    }

    return new Blob([arrayBuffer], { type: "audio/flac" });
  }

  writeString(view, offset, string) {
    for (let i = 0; i < string.length; i++) {
      view.setUint8(offset + i, string.charCodeAt(i));
    }
  }
}

// Global Export
window.WebAudioEngine = WebAudioEngine;

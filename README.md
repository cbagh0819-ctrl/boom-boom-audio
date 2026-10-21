# 🎧 Boom-Boom Audio Enhancer

[![Live Web App](https://img.shields.io/badge/Live%20Web%20App-Try%20Boom--Boom%20Audio-00F0FF?style=for-the-badge&logo=googlechrome&logoColor=black)](https://cbagh0819-ctrl.github.io/boom-boom-audio/)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Audio-Media3%20ExoPlayer%20%2B%20DSP-cyan.svg)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-pink.svg)](LICENSE)

> 🌐 **Experience Boom-Boom Audio Live in Your Browser:**  
> **[https://cbagh0819-ctrl.github.io/boom-boom-audio/](https://cbagh0819-ctrl.github.io/boom-boom-audio/)**

**Boom-Boom Audio** is a modern, high-performance music enhancer available as both a native Android app and a Web Audio edition. Powered by Kotlin, Jetpack Compose, and custom digital signal processing (DSP), it delivers studio-grade real-time audio effects, 8D binaural rotation, offline AI acoustic intelligence, and multi-format audio export.

---

## ✨ Features

### 🎛️ Real-Time Audio DSP Effects
- **Bass Boost:** Deep low-shelf filter tuned at 80 Hz with gain staging up to +18 dB.
- **Vocal Boost / Cleaner:** Peaking parametric filter targeting clarity frequencies (1.5 kHz – 3.2 kHz).
- **Pitch Shift:** Real-time semi-tone pitch shifting without affecting tempo.
- **Speed Control:** Variable speed stretching (0.5× to 2.0×) powered by Sonic time-stretch algorithms.
- **Loudness Enhancement:** Psychoacoustic harmonic loudness booster with automated headroom staging.
- **Stereo Widener:** Mid-side spatial widening matrix.
- **Studio Reverb:** Schroeder-Moorer 4-comb, 2-allpass algorithmic reverberator with low-pass feedback damping.
- **3D Audio:** Haas delay effect creating perceived binaural depth.
- **8D Audio Simulation:** Sinusoidal equal-power ($L = \cos\theta, R = \sin\theta$) 360° orbital spatial rotation.
- **Subwoofer Surround:** 45 Hz sub-bass enhancement + low-mid punch + cross-channel delay matrix optimized for subwoofers and car audio systems.

### 🧠 Offline AI Audio Intelligence
*100% offline analysis with zero external API calls or latency:*
- **AI Auto Enhance:** Automatically detects audio acoustic profile, diagnoses clipping, muddy bass, and weak vocals, and applies optimal DSP enhancement.
- **AI Smart Bass:** Intelligent dynamic low-end boost that prevents muddy acoustic build-up.
- **AI Vocal Cleaner:** Identifies harshness and low-mid resonance to bring vocals forward.
- **AI Distortion Reducer:** Detects digital clipping and applies soft-knee analog saturation and AGC limiting.
- **AI Loudness Balancer:** Evens out dynamic range using RMS crest factor analysis.
- **AI Earphone Optimizer:** Enhances mid frequencies and adds spatial widening tailored for small drivers and budget earphones.
- **AI Genre Detection:** Real-time acoustic classification (DJ / Club, EDM, Lo-Fi, Anime / Pop, Classical, Acoustic).

### 🎚️ 7 Pro Presets
- 🌙 **Night Bass Mode:** Deep late-night bass with soft loudness and subtle reverb.
- 🎛️ **DJ Punch Mode:** Club-ready punch with aggressive kick harmonics and mid-side widening.
- 🎵 **Concert Mode:** Expansive hall reverb, 3D room simulation, and wide soundstage.
- ⚡ **Anime Bass Preset:** Crisp vocal presence, enhanced sub punch, and pitch brightness (+5%).
- 🎧 **Cheap Earphone Enhancer:** Mid boost, de-mud filter, and driver compensation.
- 🚗 **Car Speaker Optimization:** Heavy 80 Hz sub boost, 60% subwoofer surround, and loudness control.
- 🎙️ **YouTube Vocal Cleaner:** De-muds low end and boosts speech clarity for voice and podcast content.

### 💾 Multi-Format Audio Export & Library
- **Export MP3:** Compressed 192 kbps audio via LAME / FFmpeg.
- **Export WAV:** Uncompressed 16-bit PCM studio master.
- **Export FLAC (24-bit):** Lossless 24-bit high-resolution audio.
- **Saved Songs Library:** Offline library with inline preview playback, share sheet integration, and track management.

---

## 🎨 UI & Design System

- **Dark Cyberpunk Canvas:** Deep obsidian background (`#090710`) with dark purple card surfaces (`#130F22`).
- **Neon Accents:** Purple neon glow (`#8B5CF6`), Electric Cyan (`#00F0FF`), Neon Pink (`#EC4899`), and Signal Green (`#10B981`).
- **Live Waveform Visualizer:** Animated gradient frequency bars reacting to playback and track metrics.
- **Edge-to-Edge Navigation:** Proper Android 14+ navigation bar padding and gesture navigation compatibility.

---

## 🏗️ Architecture & Tech Stack

- **UI:** Jetpack Compose, Material 3, Compose Navigation
- **Audio Playback:** AndroidX Media3 ExoPlayer `1.11.1`
- **Audio Engine:** Custom `AudioProcessor` pipeline with zero-latency buffer-level DSP in raw 16-bit / float PCM
- **Limiter & Saturation:** Monotonic analog $\tanh$ soft-knee saturation + instant-attack smooth-release AGC peak limiter
- **Asynchronous Operations:** Kotlin Coroutines & StateFlow architecture
- **Storage:** Android Context scoped external files storage + Room / JSON persistence

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35+
- JDK 17+ (or bundled Android Studio JBR)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/cbagh0819-ctrl/boom-boom-audio.git
cd boom-boom-audio

# Build debug APK
./gradlew assembleDebug

# Install on a connected emulator or device
./gradlew installDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is licensed under the MIT License.

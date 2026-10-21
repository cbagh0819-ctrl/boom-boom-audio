/**
 * Boom-Boom Audio - Cyberpunk Web Application Controller
 */

document.addEventListener("DOMContentLoaded", () => {
  const engine = new WebAudioEngine();

  // Presets Definition
  const PRESETS = {
    "night-bass": {
      name: "Night Bass Mode",
      desc: "Deep bass for late-night sessions",
      icon: "🌙",
      effects: { bassBoost: 85, vocalBoost: 0, loudness: 30, stereoWidth: 40, reverb: 20, audio3d: 15, audio8d: 0, subwooferSurround: 50, speed: 1.0, pitchShift: 0 }
    },
    "dj-punch": {
      name: "DJ Punch Mode",
      desc: "Hard-hitting club-ready sound",
      icon: "🎛️",
      effects: { bassBoost: 90, vocalBoost: 25, loudness: 45, stereoWidth: 60, reverb: 10, audio3d: 30, audio8d: 0, subwooferSurround: 70, speed: 1.0, pitchShift: 0 }
    },
    "concert": {
      name: "Concert Mode",
      desc: "Live concert hall experience",
      icon: "🎵",
      effects: { bassBoost: 40, vocalBoost: 15, loudness: 20, stereoWidth: 75, reverb: 65, audio3d: 60, audio8d: 0, subwooferSurround: 40, speed: 1.0, pitchShift: 0 }
    },
    "anime-bass": {
      name: "Anime Bass Preset",
      desc: "Anime OP/ED bass enhancement",
      icon: "⚡",
      effects: { bassBoost: 75, vocalBoost: 40, loudness: 35, stereoWidth: 35, reverb: 15, audio3d: 20, audio8d: 0, subwooferSurround: 60, speed: 1.05, pitchShift: 1 }
    },
    "cheap-earphones": {
      name: "Cheap Earphone Enhancer",
      desc: "Make cheap earphones sound premium",
      icon: "🎧",
      effects: { bassBoost: 50, vocalBoost: 45, loudness: 35, stereoWidth: 50, reverb: 10, audio3d: 30, audio8d: 0, subwooferSurround: 40, speed: 1.0, pitchShift: 0 }
    },
    "car-speakers": {
      name: "Car Speaker Optimization",
      desc: "Optimized for car audio systems",
      icon: "🚗",
      effects: { bassBoost: 80, vocalBoost: 15, loudness: 40, stereoWidth: 45, reverb: 8, audio3d: 25, audio8d: 0, subwooferSurround: 80, speed: 1.0, pitchShift: 0 }
    },
    "youtube-vocals": {
      name: "YouTube Vocal Cleaner",
      desc: "Crystal clear vocals for voice content",
      icon: "🎙️",
      effects: { bassBoost: -10, vocalBoost: 70, loudness: 25, stereoWidth: 20, reverb: 5, audio3d: 0, audio8d: 0, subwooferSurround: 0, speed: 1.0, pitchShift: 0 }
    }
  };

  // State
  let currentPresetId = null;
  let savedSongs = JSON.parse(localStorage.getItem("boomboom_saved_songs") || "[]");

  // DOM Elements
  const navBtns = document.querySelectorAll(".nav-btn");
  const pages = {
    home: document.getElementById("page-home"),
    editor: document.getElementById("page-editor"),
    presets: document.getElementById("page-presets"),
    saved: document.getElementById("page-saved")
  };

  const canvas = document.getElementById("visualizer-canvas");
  const playPauseBtn = document.getElementById("play-pause-btn");
  const playPauseIcon = document.getElementById("play-pause-icon");
  const seekSlider = document.getElementById("seek-slider");
  const timeCurrent = document.getElementById("time-current");
  const timeTotal = document.getElementById("time-total");
  const trackTitleDisplay = document.getElementById("track-title-display");

  const aiCard = document.getElementById("ai-card");
  const aiCardHeader = document.getElementById("ai-card-header");
  const aiAutoBtn = document.getElementById("ai-auto-btn");
  const notifBanner = document.getElementById("notif-banner");
  const notifText = document.getElementById("notif-text");
  const notifClose = document.getElementById("notif-close");

  const exportModal = document.getElementById("export-modal");
  const modalTrack = document.getElementById("modal-track");
  const modalFormat = document.getElementById("modal-format");
  const modalSize = document.getElementById("modal-size");
  const modalPreset = document.getElementById("modal-preset");
  const modalClose = document.getElementById("modal-close");
  const modalDownload = document.getElementById("modal-download");
  const modalOpenSaved = document.getElementById("modal-open-saved");

  let latestExportBlob = null;
  let latestExportFilename = "enhanced_audio.wav";

  // Setup Canvas Resolution
  function resizeCanvas() {
    if (canvas) {
      canvas.width = canvas.parentElement.clientWidth * window.devicePixelRatio;
      canvas.height = 90 * window.devicePixelRatio;
    }
  }
  window.addEventListener("resize", resizeCanvas);
  resizeCanvas();

  // Navigation Logic
  function navigateTo(targetPage) {
    Object.keys(pages).forEach(key => {
      if (pages[key]) {
        pages[key].classList.toggle("active", key === targetPage);
      }
    });

    navBtns.forEach(btn => {
      btn.classList.toggle("active", btn.dataset.page === targetPage);
    });

    if (targetPage === "editor") {
      resizeCanvas();
    }
    if (targetPage === "saved") {
      renderSavedSongs();
    }
  }

  navBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      navigateTo(btn.dataset.page);
    });
  });

  document.querySelectorAll("[data-nav]").forEach(el => {
    el.addEventListener("click", (e) => {
      e.preventDefault();
      navigateTo(el.dataset.nav);
    });
  });

  // Slider Binds
  const sliderConfigs = [
    { id: "slider-bass", key: "bassBoost", suffix: "%", min: -30, max: 100 },
    { id: "slider-vocal", key: "vocalBoost", suffix: "%", min: -30, max: 100 },
    { id: "slider-pitch", key: "pitchShift", suffix: " st", min: -12, max: 12 },
    { id: "slider-speed", key: "speed", suffix: "x", min: 0.5, max: 2.0, step: 0.05 },
    { id: "slider-loudness", key: "loudness", suffix: "%", min: 0, max: 100 },
    { id: "slider-stereo", key: "stereoWidth", suffix: "%", min: 0, max: 100 },
    { id: "slider-reverb", key: "reverb", suffix: "%", min: 0, max: 100 },
    { id: "slider-3d", key: "audio3d", suffix: "%", min: 0, max: 100 },
    { id: "slider-8d", key: "audio8d", suffix: "%", min: 0, max: 100 },
    { id: "slider-subwoofer", key: "subwooferSurround", suffix: "%", min: 0, max: 100 }
  ];

  sliderConfigs.forEach(cfg => {
    const el = document.getElementById(cfg.id);
    const valEl = document.getElementById(`${cfg.id}-val`);
    if (el && valEl) {
      el.addEventListener("input", () => {
        const numVal = parseFloat(el.value);
        engine.effects[cfg.key] = numVal;
        valEl.textContent = `${numVal > 0 && cfg.suffix === " st" ? "+" : ""}${numVal}${cfg.suffix}`;
        engine.applyEffects();
      });
    }
  });

  function syncSlidersToEngine() {
    sliderConfigs.forEach(cfg => {
      const el = document.getElementById(cfg.id);
      const valEl = document.getElementById(`${cfg.id}-val`);
      if (el && valEl) {
        const val = engine.effects[cfg.key];
        el.value = val;
        valEl.textContent = `${val > 0 && cfg.suffix === " st" ? "+" : ""}${val}${cfg.suffix}`;
      }
    });
  }

  // Reset All Button
  document.getElementById("btn-reset-all").addEventListener("click", () => {
    engine.effects = {
      bassBoost: 50,
      vocalBoost: 0,
      pitchShift: 0,
      speed: 1.0,
      loudness: 35,
      stereoWidth: 30,
      reverb: 0,
      audio3d: 0,
      audio8d: 0,
      subwooferSurround: 50
    };
    currentPresetId = null;
    syncSlidersToEngine();
    engine.applyEffects();
    updatePresetsListUI();
    showNotification("Effects reset to default");
  });

  // Play / Pause Controls
  playPauseBtn.addEventListener("click", () => {
    if (engine.isPlaying) {
      engine.pause();
      playPauseIcon.textContent = "▶";
    } else {
      engine.play();
      playPauseIcon.textContent = "⏸";
    }
  });

  document.getElementById("btn-rewind").addEventListener("click", () => {
    engine.seek(engine.getCurrentTime() - 10);
  });

  document.getElementById("btn-restart").addEventListener("click", () => {
    engine.seek(0);
  });

  document.getElementById("btn-forward").addEventListener("click", () => {
    engine.seek(engine.getCurrentTime() + 10);
  });

  seekSlider.addEventListener("input", () => {
    const percent = parseFloat(seekSlider.value);
    const seekTime = (percent / 100) * engine.duration;
    engine.seek(seekTime);
  });

  engine.onProgressUpdate = (current, total) => {
    if (total > 0) {
      const pct = (current / total) * 100;
      seekSlider.value = pct;
      timeCurrent.textContent = formatTime(current);
      timeTotal.textContent = formatTime(total);
    }
  };

  function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  // Visualizer Animation Loop
  function visualizerLoop() {
    engine.drawVisualizer(canvas);
    requestAnimationFrame(visualizerLoop);
  }
  requestAnimationFrame(visualizerLoop);

  // File Upload Handling
  const fileInput = document.getElementById("file-upload-input");
  const dropzones = [document.getElementById("home-dropzone")];

  dropzones.forEach(dz => {
    if (dz) {
      dz.addEventListener("click", () => fileInput.click());
      dz.addEventListener("dragover", (e) => {
        e.preventDefault();
        dz.classList.add("dragover");
      });
      dz.addEventListener("dragleave", () => dz.classList.remove("dragover"));
      dz.addEventListener("drop", (e) => {
        e.preventDefault();
        dz.classList.remove("dragover");
        if (e.dataTransfer.files.length > 0) {
          handleFile(e.dataTransfer.files[0]);
        }
      });
    }
  });

  fileInput.addEventListener("change", () => {
    if (fileInput.files.length > 0) {
      handleFile(fileInput.files[0]);
    }
  });

  document.getElementById("btn-change-track").addEventListener("click", () => {
    fileInput.click();
  });

  document.getElementById("btn-start-demo").addEventListener("click", async () => {
    await engine.loadDemoTrack();
    trackTitleDisplay.textContent = engine.trackTitle;
    navigateTo("editor");
    engine.play();
    playPauseIcon.textContent = "⏸";
  });

  async function handleFile(file) {
    showNotification(`Loading "${file.name}"...`);
    await engine.loadAudioFile(file);
    trackTitleDisplay.textContent = engine.trackTitle;
    navigateTo("editor");
    engine.play();
    playPauseIcon.textContent = "⏸";
    showNotification(`Ready: ${file.name}`);
  }

  // AI Expandable Card Toggle
  aiCardHeader.addEventListener("click", () => {
    aiCard.classList.toggle("expanded");
  });

  // AI Auto Enhance Button
  aiAutoBtn.addEventListener("click", () => {
    const diagnosis = engine.analyzeAudio();
    if (diagnosis) {
      document.getElementById("diag-genre").textContent = diagnosis.genre;
      document.getElementById("diag-confidence").textContent = `${diagnosis.confidence}% confidence`;
      document.getElementById("diag-preset").textContent = `✦ ${diagnosis.preset}`;
      document.getElementById("diag-summary").textContent = diagnosis.summary;

      const tagsContainer = document.getElementById("diag-tags");
      tagsContainer.innerHTML = "";
      diagnosis.tags.forEach(t => {
        const tagSpan = document.createElement("span");
        tagSpan.className = "diagnosis-tag";
        tagSpan.textContent = `⚠ ${t}`;
        tagsContainer.appendChild(tagSpan);
      });

      document.getElementById("ai-diagnosis-box").style.display = "block";

      // Apply settings
      Object.assign(engine.effects, diagnosis.optimalEffects);
      syncSlidersToEngine();
      engine.applyEffects();
      showNotification(`✦ AI Auto-Enhanced: Detected ${diagnosis.genre}`);
    }
  });

  // 6 AI Smart Buttons
  document.getElementById("btn-ai-smart-bass").addEventListener("click", () => {
    engine.effects.bassBoost = 75;
    engine.effects.subwooferSurround = 65;
    syncSlidersToEngine();
    engine.applyEffects();
    showNotification("⚡ AI Smart Bass: Optimized low frequencies and punch");
  });

  document.getElementById("btn-ai-vocal-cleaner").addEventListener("click", () => {
    engine.effects.vocalBoost = 45;
    engine.effects.bassBoost = 35;
    syncSlidersToEngine();
    engine.applyEffects();
    showNotification("🎤 AI Vocal Cleaner: Clarity boosted, mud cut at 300Hz");
  });

  document.getElementById("btn-ai-distortion").addEventListener("click", () => {
    engine.effects.loudness = 25;
    syncSlidersToEngine();
    engine.applyEffects();
    showNotification("🛡 AI Distortion: Applied soft limiter & peak compression");
  });

  document.getElementById("btn-ai-loudness").addEventListener("click", () => {
    engine.effects.loudness = 50;
    syncSlidersToEngine();
    engine.applyEffects();
    showNotification("📊 AI Loudness Balancer: Dynamic range normalized");
  });

  document.getElementById("btn-ai-earphone").addEventListener("click", () => {
    engine.effects.bassBoost = 50;
    engine.effects.vocalBoost = 30;
    engine.effects.stereoWidth = 50;
    syncSlidersToEngine();
    engine.applyEffects();
    showNotification("🎧 AI Earphone: Optimized for small driver frequency curves");
  });

  document.getElementById("btn-ai-genre").addEventListener("click", () => {
    const diag = engine.analyzeAudio();
    if (diag) {
      showNotification(`🎵 AI Genre Detected: ${diag.genre} (${diag.confidence}%)`);
    }
  });

  // Notification Banner
  function showNotification(msg) {
    notifText.textContent = msg;
    notifBanner.classList.remove("hidden");
  }

  notifClose.addEventListener("click", () => {
    notifBanner.classList.add("hidden");
  });

  // Presets Rendering & Selection
  const presetsListContainer = document.getElementById("presets-list");

  function renderPresets() {
    presetsListContainer.innerHTML = "";
    Object.keys(PRESETS).forEach(id => {
      const p = PRESETS[id];
      const card = document.createElement("div");
      card.className = `preset-card ${currentPresetId === id ? "active" : ""}`;
      card.innerHTML = `
        <div class="preset-left">
          <div class="preset-icon-box">${p.icon}</div>
          <div>
            <div class="preset-name">${p.name}</div>
            <div class="preset-desc">${p.desc}</div>
          </div>
        </div>
        ${currentPresetId === id ? '<span class="active-badge">ACTIVE</span>' : ''}
      `;
      card.addEventListener("click", () => {
        applyPreset(id);
      });
      presetsListContainer.appendChild(card);
    });
  }

  function applyPreset(id) {
    const p = PRESETS[id];
    if (!p) return;
    currentPresetId = id;
    Object.assign(engine.effects, p.effects);
    syncSlidersToEngine();
    engine.applyEffects();
    updatePresetsListUI();
    showNotification(`✓ ${p.name} Applied`);
    navigateTo("editor");
  }

  function updatePresetsListUI() {
    renderPresets();
  }
  renderPresets();

  // Export Buttons
  async function triggerExport(format) {
    showNotification(`Rendering ${format.toUpperCase()} export...`);
    const result = await engine.exportAudio(format);
    if (!result) return;

    latestExportBlob = result.blob;
    latestExportFilename = result.filename;

    // Show Export Modal
    modalTrack.textContent = result.title;
    modalFormat.textContent = result.format;
    modalSize.textContent = `${Math.round(result.sizeBytes / 1024)} KB`;
    modalPreset.textContent = currentPresetId ? PRESETS[currentPresetId].name : "Custom Enhanced";
    exportModal.classList.add("active");

    // Automatically trigger browser download
    downloadBlob(latestExportBlob, latestExportFilename);

    // Save to Saved Songs Library
    const songRecord = {
      id: Date.now().toString(),
      title: result.title,
      format: result.format,
      sizeBytes: result.sizeBytes,
      preset: currentPresetId ? PRESETS[currentPresetId].name : "Custom",
      durationMs: result.durationMs,
      date: new Date().toLocaleDateString()
    };
    savedSongs.unshift(songRecord);
    localStorage.setItem("boomboom_saved_songs", JSON.stringify(savedSongs));
  }

  document.getElementById("btn-export-wav").addEventListener("click", () => triggerExport("wav"));
  document.getElementById("btn-export-flac").addEventListener("click", () => triggerExport("flac"));
  document.getElementById("btn-export-mp3").addEventListener("click", () => triggerExport("mp3"));

  document.getElementById("btn-save-library").addEventListener("click", async () => {
    await triggerExport("mp3");
  });

  // Modal Controls
  modalClose.addEventListener("click", () => {
    exportModal.classList.remove("active");
  });

  modalDownload.addEventListener("click", () => {
    if (latestExportBlob) {
      downloadBlob(latestExportBlob, latestExportFilename);
    }
  });

  modalOpenSaved.addEventListener("click", () => {
    exportModal.classList.remove("active");
    navigateTo("saved");
  });

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // Saved Songs View
  const savedListContainer = document.getElementById("saved-list");
  const savedCount = document.getElementById("saved-count");

  function renderSavedSongs() {
    savedSongs = JSON.parse(localStorage.getItem("boomboom_saved_songs") || "[]");
    savedCount.textContent = `(${savedSongs.length} tracks)`;

    if (savedSongs.length === 0) {
      savedListContainer.innerHTML = `
        <div style="text-align: center; padding: 40px 20px; color: var(--text-muted);">
          <div style="font-size: 36px; margin-bottom: 12px;">🎵</div>
          <div style="font-weight: 700; color: #fff; margin-bottom: 6px;">No saved tracks yet</div>
          <div style="font-size: 13px;">Enhance and export audio to build your cyberpunk audio library.</div>
        </div>
      `;
      return;
    }

    savedListContainer.innerHTML = "";
    savedSongs.forEach(song => {
      const card = document.createElement("div");
      card.className = "saved-track-card";
      card.innerHTML = `
        <div class="saved-left">
          <button class="saved-play-btn" title="Play Track">▶</button>
          <div class="saved-track-details">
            <div class="saved-track-name">${song.title}</div>
            <div class="saved-meta-row">
              <span class="saved-badge">${song.format}</span>
              <span class="saved-preset-badge">🎛 ${song.preset}</span>
              <span style="font-size: 11px; color: var(--text-dim);">${song.date}</span>
            </div>
          </div>
        </div>
        <div class="saved-actions">
          <button class="saved-icon-btn delete" title="Delete Track">🗑</button>
        </div>
      `;

      card.querySelector(".saved-play-btn").addEventListener("click", () => {
        navigateTo("editor");
        engine.play();
        playPauseIcon.textContent = "⏸";
        showNotification(`Playing "${song.title}"`);
      });

      card.querySelector(".delete").addEventListener("click", () => {
        if (confirm(`Delete "${song.title}" from saved library?`)) {
          savedSongs = savedSongs.filter(s => s.id !== song.id);
          localStorage.setItem("boomboom_saved_songs", JSON.stringify(savedSongs));
          renderSavedSongs();
          showNotification("Track deleted from library");
        }
      });

      savedListContainer.appendChild(card);
    });
  }

  // Preload Demo Track
  engine.loadDemoTrack().then(() => {
    trackTitleDisplay.textContent = engine.trackTitle;
    syncSlidersToEngine();
  });
});

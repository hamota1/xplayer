// ========================================================
// XPLAYER (VIDEO PLAYER ALL FORMAT) - RECONSTRUCTED CLONE
// ========================================================

// --- INITIAL STATE ---
let videos = [
  {
    id: "vid-1",
    title: "Big Buck Bunny (4K Ultra HD)",
    duration: "09:56",
    durationSec: 596,
    resolution: "4K UHD",
    format: "MP4",
    size: "185 MB",
    folder: "Movies",
    date: "2026-10-20",
    url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    thumbnail: "assets/video_sample_thumb1.webp",
    isFavorite: true,
    isVault: false,
    isTrash: false,
  },
  {
    id: "vid-2",
    title: "Tears of Steel (Sci-Fi VFX)",
    duration: "12:14",
    durationSec: 734,
    resolution: "1080p FHD",
    format: "MKV",
    size: "340 MB",
    folder: "Movies",
    date: "2026-10-22",
    url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    thumbnail: "assets/video_sample_thumb2.webp",
    isFavorite: false,
    isVault: false,
    isTrash: false,
  },
  {
    id: "vid-3",
    title: "Sintel Open Animation Movie",
    duration: "00:52",
    durationSec: 52,
    resolution: "1080p FHD",
    format: "MP4",
    size: "45 MB",
    folder: "Downloads",
    date: "2026-10-25",
    url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
    thumbnail: "assets/video_sample_thumb3.webp",
    isFavorite: true,
    isVault: false,
    isTrash: false,
  },
  {
    id: "vid-4",
    title: "We Are Going On Bullrun (Automotive)",
    duration: "00:47",
    durationSec: 47,
    resolution: "720p HD",
    format: "WEBM",
    size: "22 MB",
    folder: "Camera",
    date: "2026-10-28",
    url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
    thumbnail: "assets/splash1.png",
    isFavorite: false,
    isVault: false,
    isTrash: false,
  }
];

let playlists = [
  { id: "pl-1", title: "Favorite Movies", videoIds: ["vid-1", "vid-3"] },
  { id: "pl-2", title: "Action & Sci-Fi", videoIds: ["vid-2"] }
];

let currentVideo = null;
let currentVideoIndex = 0;
let isControlsLocked = false;
let controlsTimeout = null;
let isNightMode = false;
let currentAspectRatioIndex = 0;
const aspectRatios = ['fit', '16-9', '4-3', 'fill'];
const aspectRatioLabels = ['Fit', '16:9', '4:3', 'Fill'];

// Vault PIN state
const VAULT_PIN = "1234";
let enteredPin = "";
let isVaultUnlocked = false;

// Audio Context & Equalizer
let audioCtx = null;
let audioSource = null;
let biquadFilters = [];
let bassBoostNode = null;
let currentBrightness = 100; // 0% - 100%

// Subtitles
let parsedSubtitles = [];

// DOM Elements
const videoEl = document.getElementById("mainVideo");
const playerModal = document.getElementById("playerModal");
const playerTopBar = document.getElementById("playerTopBar");
const playerBottomBar = document.getElementById("playerBottomBar");
const seekBar = document.getElementById("playerSeekBar");
const currentTimeEl = document.getElementById("playerCurrentTime");
const durationEl = document.getElementById("playerDuration");
const playPauseBtn = document.getElementById("playerPlayPauseBtn");
const gestureIndicator = document.getElementById("gestureIndicator");
const gestureIcon = document.getElementById("gestureIcon");
const gestureText = document.getElementById("gestureText");
const gestureProgress = document.getElementById("gestureProgressBar");

// ========================================================
// INITIALIZATION
// ========================================================
document.addEventListener("DOMContentLoaded", () => {
  renderVideos();
  setupVideoEventListeners();
});

// ========================================================
// TAB SWITCHING
// ========================================================
function switchTab(tabId) {
  document.querySelectorAll(".tab-panel").forEach(p => p.classList.add("hidden"));
  document.querySelectorAll(".nav-item").forEach(b => {
    b.classList.remove("bg-brand/10", "text-brand", "border", "border-brand/20");
    b.classList.add("text-gray-400");
  });

  const activeBtn = document.getElementById(`tab-btn-${tabId}`);
  if (activeBtn) {
    activeBtn.classList.remove("text-gray-400");
    activeBtn.classList.add("bg-brand/10", "text-brand", "border", "border-brand/20");
  }

  const activePanel = document.getElementById(`tab-${tabId}`);
  if (activePanel) {
    activePanel.classList.remove("hidden");
  }

  if (tabId === "folders") renderFolders();
  if (tabId === "playlists") renderPlaylists();
  if (tabId === "vault") {
    if (!isVaultUnlocked) {
      document.getElementById("vaultLockedScreen").classList.remove("hidden");
      document.getElementById("vaultUnlockedScreen").classList.add("hidden");
      clearPin();
    } else {
      renderVault();
    }
  }
  if (tabId === "trash") renderTrash();
}

// ========================================================
// RENDER VIDEO LIBRARY
// ========================================================
function renderVideos(filteredList = null) {
  const container = document.getElementById("videoGrid");
  container.innerHTML = "";
  const list = filteredList || videos.filter(v => !v.isVault && !v.isTrash);

  document.getElementById("videoCountBadge").textContent = `${list.length} videos`;

  if (list.length === 0) {
    container.innerHTML = `
      <div class="col-span-full py-16 text-center text-gray-500">
        <i class="ph ph-film-strip text-5xl mb-3 inline-block"></i>
        <p class="text-sm font-semibold">No video files found</p>
        <p class="text-xs text-gray-600 mt-1">Open a local file or drag videos onto this screen</p>
      </div>
    `;
    return;
  }

  list.forEach(vid => {
    const card = document.createElement("div");
    card.className = "bg-darkCard border border-darkBorder rounded-2xl overflow-hidden group hover:border-brand/40 transition-all hover:shadow-xl hover:shadow-brand/5 flex flex-col justify-between";
    card.innerHTML = `
      <div class="relative aspect-video bg-[#181818] overflow-hidden cursor-pointer" onclick="openPlayer('${vid.id}')">
        <img src="${vid.thumbnail}" alt="${vid.title}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300">
        <div class="absolute inset-0 bg-black/20 group-hover:bg-transparent transition-colors"></div>
        
        <!-- Duration badge -->
        <span class="absolute bottom-2.5 right-2.5 bg-black/80 backdrop-blur-sm text-white text-[11px] font-bold px-2 py-0.5 rounded-md">
          ${vid.duration}
        </span>

        <!-- Resolution badge -->
        <span class="absolute top-2.5 left-2.5 bg-brand text-darkBg text-[10px] font-black px-1.5 py-0.5 rounded shadow">
          ${vid.resolution}
        </span>

        <!-- Play overlay icon -->
        <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <div class="w-12 h-12 rounded-full bg-brand text-darkBg flex items-center justify-center shadow-lg shadow-brand/40 transform scale-75 group-hover:scale-100 transition-transform">
            <i class="ph-bold ph-play text-xl ml-0.5"></i>
          </div>
        </div>
      </div>

      <div class="p-3.5 flex items-start justify-between">
        <div class="flex-1 min-w-0 pr-2">
          <h4 class="font-bold text-sm text-gray-200 truncate group-hover:text-brand transition-colors cursor-pointer" onclick="openPlayer('${vid.id}')">
            ${vid.title}
          </h4>
          <p class="text-[11px] text-gray-400 mt-1 flex items-center gap-1.5">
            <span>${vid.format}</span> · <span>${vid.size}</span> · <span>${vid.folder}</span>
          </p>
        </div>

        <div class="flex items-center space-x-1">
          <button onclick="toggleFavorite('${vid.id}')" title="Favorite" class="p-1.5 rounded-lg hover:bg-[#282828] text-gray-400 hover:text-brand transition">
            <i class="${vid.isFavorite ? 'ph-fill ph-heart text-red-500' : 'ph ph-heart'} text-base"></i>
          </button>
          <button onclick="moveToVault('${vid.id}')" title="Move to Private Vault" class="p-1.5 rounded-lg hover:bg-[#282828] text-gray-400 hover:text-brand transition">
            <i class="ph ph-lock-key text-base"></i>
          </button>
          <button onclick="deleteToTrash('${vid.id}')" title="Delete" class="p-1.5 rounded-lg hover:bg-[#282828] text-gray-400 hover:text-red-400 transition">
            <i class="ph ph-trash text-base"></i>
          </button>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

// ========================================================
// FOLDERS & PLAYLISTS VIEW
// ========================================================
function renderFolders() {
  const container = document.getElementById("foldersGrid");
  container.innerHTML = "";

  const foldersMap = {};
  videos.filter(v => !v.isVault && !v.isTrash).forEach(v => {
    foldersMap[v.folder] = (foldersMap[v.folder] || 0) + 1;
  });

  Object.entries(foldersMap).forEach(([folderName, count]) => {
    const card = document.createElement("div");
    card.className = "bg-darkCard border border-darkBorder rounded-2xl p-4 flex items-center space-x-4 cursor-pointer hover:border-brand/40 transition";
    card.onclick = () => {
      const filtered = videos.filter(v => v.folder === folderName && !v.isVault && !v.isTrash);
      switchTab("videos");
      renderVideos(filtered);
      showToast(`Showing folder: ${folderName}`);
    };
    card.innerHTML = `
      <div class="w-12 h-12 rounded-xl bg-brand/10 border border-brand/20 flex items-center justify-center text-brand text-2xl">
        <i class="ph-bold ph-folder"></i>
      </div>
      <div>
        <h4 class="font-bold text-sm text-white">${folderName}</h4>
        <p class="text-xs text-gray-400 mt-0.5">${count} videos</p>
      </div>
    `;
    container.appendChild(card);
  });
}

function renderPlaylists() {
  const container = document.getElementById("playlistsGrid");
  container.innerHTML = "";

  playlists.forEach(pl => {
    const card = document.createElement("div");
    card.className = "bg-darkCard border border-darkBorder rounded-2xl p-4 flex items-center justify-between cursor-pointer hover:border-brand/40 transition";
    card.innerHTML = `
      <div class="flex items-center space-x-4" onclick="openPlaylist('${pl.id}')">
        <div class="w-12 h-12 rounded-xl bg-brand/10 border border-brand/20 flex items-center justify-center text-brand text-2xl">
          <i class="ph-bold ph-playlist"></i>
        </div>
        <div>
          <h4 class="font-bold text-sm text-white">${pl.title}</h4>
          <p class="text-xs text-gray-400 mt-0.5">${pl.videoIds.length} videos</p>
        </div>
      </div>
      <button onclick="playPlaylistDirect('${pl.id}')" class="w-10 h-10 rounded-full bg-brand/10 hover:bg-brand text-brand hover:text-darkBg flex items-center justify-center transition">
        <i class="ph-bold ph-play text-lg"></i>
      </button>
    `;
    container.appendChild(card);
  });
}

function openPlaylist(plId) {
  const pl = playlists.find(p => p.id === plId);
  if (!pl) return;
  const filtered = videos.filter(v => pl.videoIds.includes(v.id));
  switchTab("videos");
  renderVideos(filtered);
  showToast(`Loaded playlist: ${pl.title}`);
}

function playPlaylistDirect(plId) {
  const pl = playlists.find(p => p.id === plId);
  if (pl && pl.videoIds.length > 0) {
    openPlayer(pl.videoIds[0]);
  }
}

// ========================================================
// PRIVATE VAULT (PIN SECURITY)
// ========================================================
function enterPinDigit(digit) {
  if (enteredPin.length < 4) {
    enteredPin += digit;
    updatePinDots();
  }
  if (enteredPin.length === 4) {
    setTimeout(verifyPin, 150);
  }
}

function clearPin() {
  enteredPin = "";
  updatePinDots();
  document.getElementById("pinErrorMsg").textContent = "";
}

function backspacePin() {
  enteredPin = enteredPin.slice(0, -1);
  updatePinDots();
}

function updatePinDots() {
  for (let i = 1; i <= 4; i++) {
    const dot = document.getElementById(`pinDot${i}`);
    if (i <= enteredPin.length) {
      dot.classList.add("bg-brand", "border-brand");
      dot.classList.remove("border-gray-600");
    } else {
      dot.classList.remove("bg-brand", "border-brand");
      dot.classList.add("border-gray-600");
    }
  }
}

function verifyPin() {
  if (enteredPin === VAULT_PIN) {
    isVaultUnlocked = true;
    document.getElementById("vaultLockedScreen").classList.add("hidden");
    document.getElementById("vaultUnlockedScreen").classList.remove("hidden");
    renderVault();
    showToast("Vault Unlocked");
  } else {
    document.getElementById("pinErrorMsg").textContent = "Incorrect PIN! Default is 1234";
    clearPin();
  }
}

function lockVault() {
  isVaultUnlocked = false;
  document.getElementById("vaultUnlockedScreen").classList.add("hidden");
  document.getElementById("vaultLockedScreen").classList.remove("hidden");
  clearPin();
  showToast("Vault Locked");
}

function renderVault() {
  const container = document.getElementById("vaultGrid");
  container.innerHTML = "";
  const list = videos.filter(v => v.isVault && !v.isTrash);

  if (list.length === 0) {
    container.innerHTML = `
      <div class="col-span-full py-16 text-center text-gray-500">
        <i class="ph ph-shield-check text-5xl mb-3 inline-block"></i>
        <p class="text-sm font-semibold">Your Private Vault is empty</p>
        <p class="text-xs text-gray-600 mt-1">Click the lock icon on any video in All Videos to move it here</p>
      </div>
    `;
    return;
  }

  list.forEach(vid => {
    const card = document.createElement("div");
    card.className = "bg-darkCard border border-darkBorder rounded-2xl overflow-hidden";
    card.innerHTML = `
      <div class="relative aspect-video bg-[#181818] cursor-pointer" onclick="openPlayer('${vid.id}')">
        <img src="${vid.thumbnail}" class="w-full h-full object-cover">
        <span class="absolute bottom-2.5 right-2.5 bg-black/80 text-white text-[11px] font-bold px-2 py-0.5 rounded">
          ${vid.duration}
        </span>
      </div>
      <div class="p-3.5 flex items-center justify-between">
        <div class="truncate">
          <h4 class="font-bold text-sm text-white truncate">${vid.title}</h4>
          <span class="text-xs text-brand">Private</span>
        </div>
        <button onclick="restoreFromVault('${vid.id}')" title="Move back to Public Library" class="p-1.5 rounded-lg bg-[#282828] text-gray-300 hover:text-brand transition text-xs px-2.5 py-1">
          Unhide
        </button>
      </div>
    `;
    container.appendChild(card);
  });
}

function moveToVault(id) {
  const v = videos.find(x => x.id === id);
  if (v) {
    v.isVault = true;
    renderVideos();
    showToast(`"${v.title}" moved to Private Vault`);
  }
}

function restoreFromVault(id) {
  const v = videos.find(x => x.id === id);
  if (v) {
    v.isVault = false;
    renderVault();
    renderVideos();
    showToast(`"${v.title}" restored to Public Library`);
  }
}

// ========================================================
// RECYCLE BIN / TRASH
// ========================================================
function deleteToTrash(id) {
  const v = videos.find(x => x.id === id);
  if (v) {
    v.isTrash = true;
    renderVideos();
    showToast(`"${v.title}" moved to Recycle Bin`);
  }
}

function renderTrash() {
  const container = document.getElementById("trashGrid");
  container.innerHTML = "";
  const list = videos.filter(v => v.isTrash);

  if (list.length === 0) {
    container.innerHTML = `
      <div class="col-span-full py-16 text-center text-gray-500">
        <i class="ph ph-trash text-5xl mb-3 inline-block"></i>
        <p class="text-sm font-semibold">Recycle Bin is empty</p>
      </div>
    `;
    return;
  }

  list.forEach(vid => {
    const card = document.createElement("div");
    card.className = "bg-darkCard border border-darkBorder rounded-2xl overflow-hidden p-3.5 flex items-center justify-between";
    card.innerHTML = `
      <div class="flex items-center space-x-3 truncate">
        <img src="${vid.thumbnail}" class="w-16 h-10 object-cover rounded-lg bg-[#181818]">
        <div class="truncate">
          <h4 class="font-bold text-sm text-white truncate">${vid.title}</h4>
          <span class="text-xs text-gray-400">${vid.size}</span>
        </div>
      </div>
      <div class="flex items-center space-x-2">
        <button onclick="restoreFromTrash('${vid.id}')" title="Restore" class="p-2 rounded-lg bg-[#282828] text-gray-300 hover:text-green-400 transition">
          <i class="ph-bold ph-arrow-counter-clockwise"></i>
        </button>
        <button onclick="permanentDelete('${vid.id}')" title="Delete Permanently" class="p-2 rounded-lg bg-[#282828] text-gray-300 hover:text-red-400 transition">
          <i class="ph-bold ph-x"></i>
        </button>
      </div>
    `;
    container.appendChild(card);
  });
}

function restoreFromTrash(id) {
  const v = videos.find(x => x.id === id);
  if (v) {
    v.isTrash = false;
    renderTrash();
    renderVideos();
    showToast(`"${v.title}" restored`);
  }
}

function permanentDelete(id) {
  videos = videos.filter(x => x.id !== id);
  renderTrash();
  showToast("Permanently deleted");
}

function emptyTrashPrompt() {
  if (confirm("Are you sure you want to permanently delete all items in the Recycle Bin?")) {
    videos = videos.filter(x => !x.isTrash);
    renderTrash();
    showToast("Recycle Bin emptied");
  }
}

// ========================================================
// LOCAL FILE SELECTION & DRAG DROP
// ========================================================
function handleLocalFileSelect(event) {
  const file = event.target.files[0];
  if (file) {
    addAndPlayLocalVideo(file);
  }
}

function handleFileDrop(event) {
  event.preventDefault();
  document.getElementById("dropZone").classList.remove("border-brand", "bg-brand/5");
  const file = event.dataTransfer.files[0];
  if (file && file.type.startsWith("video/")) {
    addAndPlayLocalVideo(file);
  } else {
    showToast("Please drop a valid video file!");
  }
}

function addAndPlayLocalVideo(file) {
  const objectUrl = URL.createObjectURL(file);
  const newVid = {
    id: "local-" + Date.now(),
    title: file.name.replace(/\.[^/.]+$/, ""),
    duration: "Local",
    durationSec: 0,
    resolution: "HD",
    format: file.name.split('.').pop().toUpperCase(),
    size: (file.size / (1024 * 1024)).toFixed(1) + " MB",
    folder: "Local Files",
    date: new Date().toISOString().split('T')[0],
    url: objectUrl,
    thumbnail: "assets/splash2.png",
    isFavorite: false,
    isVault: false,
    isTrash: false
  };

  videos.unshift(newVid);
  renderVideos();
  openPlayer(newVid.id);
  showToast(`Loaded: ${newVid.title}`);
}

// ========================================================
// VIDEO PLAYER CORE ENGINE
// ========================================================
function openPlayer(videoId) {
  const index = videos.findIndex(v => v.id === videoId);
  if (index === -1) return;

  currentVideoIndex = index;
  currentVideo = videos[index];

  document.getElementById("playerVideoTitle").textContent = currentVideo.title;
  document.getElementById("playerVideoInfo").textContent = `${currentVideo.resolution} · ${currentVideo.format} · Hardware Decoded`;

  // Update Music background tab metadata as well
  document.getElementById("musicTrackTitle").textContent = currentVideo.title;
  document.getElementById("musicTrackArtist").textContent = `${currentVideo.folder} · ${currentVideo.format}`;

  videoEl.src = currentVideo.url;
  playerModal.classList.remove("hidden");
  videoEl.play().catch(() => {});
  playPauseBtn.innerHTML = '<i class="ph-bold ph-pause text-xl"></i>';

  setupAudioEqualizer();
  resetControlsTimeout();
}

function closePlayer() {
  videoEl.pause();
  playerModal.classList.add("hidden");
  clearTimeout(controlsTimeout);
}

function toggleVideoPlay() {
  if (videoEl.paused) {
    videoEl.play();
    playPauseBtn.innerHTML = '<i class="ph-bold ph-pause text-xl"></i>';
    document.getElementById("musicPlayBtn").innerHTML = '<i class="ph-bold ph-pause"></i>';
  } else {
    videoEl.pause();
    playPauseBtn.innerHTML = '<i class="ph-bold ph-play text-xl"></i>';
    document.getElementById("musicPlayBtn").innerHTML = '<i class="ph-bold ph-play"></i>';
  }
}

function playPrevVideo() {
  if (currentVideoIndex > 0) {
    openPlayer(videos[currentVideoIndex - 1].id);
  } else {
    openPlayer(videos[videos.length - 1].id);
  }
}

function playNextVideo() {
  if (currentVideoIndex < videos.length - 1) {
    openPlayer(videos[currentVideoIndex + 1].id);
  } else {
    openPlayer(videos[0].id);
  }
}

function skipSeconds(sec) {
  videoEl.currentTime = Math.max(0, Math.min(videoEl.duration, videoEl.currentTime + sec));
  showGestureHud(sec > 0 ? "ph-arrow-clockwise" : "ph-arrow-counter-clockwise", `${Math.abs(sec)}s`);
}

function setupVideoEventListeners() {
  videoEl.addEventListener("timeupdate", () => {
    if (!isNaN(videoEl.duration)) {
      const progress = (videoEl.currentTime / videoEl.duration) * 100;
      seekBar.value = progress;
      currentTimeEl.textContent = formatTime(videoEl.currentTime);
      durationEl.textContent = formatTime(videoEl.duration);
    }
  });

  videoEl.addEventListener("loadedmetadata", () => {
    durationEl.textContent = formatTime(videoEl.duration);
  });

  videoEl.addEventListener("ended", () => {
    playNextVideo();
  });
}

function handleSeekInput(val) {
  if (!isNaN(videoEl.duration)) {
    videoEl.currentTime = (val / 100) * videoEl.duration;
  }
}

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;
}

// ========================================================
// CONTROLS AUTO-HIDE & LOCK
// ========================================================
function handlePlayerMouseMove() {
  if (isControlsLocked) return;
  showControls();
  resetControlsTimeout();
}

function showControls() {
  playerTopBar.classList.remove("-translate-y-full", "opacity-0");
  playerBottomBar.classList.remove("translate-y-full", "opacity-0");
}

function hideControls() {
  if (videoEl.paused) return; // keep controls when paused
  playerTopBar.classList.add("-translate-y-full", "opacity-0");
  playerBottomBar.classList.add("translate-y-full", "opacity-0");
}

function resetControlsTimeout() {
  clearTimeout(controlsTimeout);
  controlsTimeout = setTimeout(() => {
    hideControls();
  }, 3500);
}

function toggleScreenLock(e) {
  e.stopPropagation();
  isControlsLocked = !isControlsLocked;
  const icon = document.getElementById("screenLockIcon");
  const lockIndicator = document.getElementById("lockedIndicator");

  if (isControlsLocked) {
    icon.className = "ph-bold ph-lock-simple text-xl text-brand";
    lockIndicator.classList.remove("hidden");
    hideControls();
    showToast("Screen Controls Locked");
  } else {
    icon.className = "ph-bold ph-lock-simple-open text-xl text-gray-200";
    lockIndicator.classList.add("hidden");
    showControls();
    resetControlsTimeout();
    showToast("Screen Controls Unlocked");
  }
}

// ========================================================
// GESTURE ENGINE (BRIGHTNESS, VOLUME, SEEK)
// ========================================================
let gestureStartX = 0;
let gestureStartY = 0;
let isDraggingGesture = false;
let gestureType = null; // 'brightness' | 'volume' | 'seek'

function startGesture(e) {
  if (isControlsLocked) return;
  gestureStartX = e.clientX;
  gestureStartY = e.clientY;
  isDraggingGesture = true;
  gestureType = null;
}

function moveGesture(e) {
  if (!isDraggingGesture || isControlsLocked) return;
  const deltaX = e.clientX - gestureStartX;
  const deltaY = gestureStartY - e.clientY; // up is positive

  if (!gestureType) {
    if (Math.abs(deltaY) > 15) {
      const isLeftSide = gestureStartX < (window.innerWidth / 2);
      gestureType = isLeftSide ? "brightness" : "volume";
    } else if (Math.abs(deltaX) > 25) {
      gestureType = "seek";
    }
  }

  if (gestureType === "brightness") {
    currentBrightness = Math.max(15, Math.min(100, currentBrightness + (deltaY > 0 ? 1 : -1)));
    videoEl.style.filter = `brightness(${currentBrightness}%)`;
    showGestureHud("ph-sun", `${currentBrightness}%`, currentBrightness);
  } else if (gestureType === "volume") {
    const newVol = Math.max(0, Math.min(1, videoEl.volume + (deltaY > 0 ? 0.02 : -0.02)));
    setPlayerVolume(newVol);
    showGestureHud(newVol === 0 ? "ph-speaker-x" : "ph-speaker-high", `${Math.round(newVol * 100)}%`, newVol * 100);
  } else if (gestureType === "seek") {
    const seekStep = deltaX > 0 ? 1 : -1;
    videoEl.currentTime = Math.max(0, Math.min(videoEl.duration, videoEl.currentTime + seekStep));
    showGestureHud(deltaX > 0 ? "ph-fast-forward" : "ph-rewind", formatTime(videoEl.currentTime));
  }
}

function endGesture() {
  isDraggingGesture = false;
  setTimeout(() => {
    gestureIndicator.style.opacity = "0";
  }, 700);
}

// Touch event wrappers
function startTouchGesture(e) {
  if (e.touches.length === 1) {
    startGesture({ clientX: e.touches[0].clientX, clientY: e.touches[0].clientY });
  }
}

function moveTouchGesture(e) {
  if (e.touches.length === 1) {
    moveGesture({ clientX: e.touches[0].clientX, clientY: e.touches[0].clientY });
  }
}

function handlePlayerDblClick(e) {
  if (isControlsLocked) return;
  const isLeftSide = e.clientX < (window.innerWidth / 2);
  skipSeconds(isLeftSide ? -10 : 10);
}

function showGestureHud(iconClass, text, percent = null) {
  gestureIcon.className = `ph-bold ${iconClass} text-4xl text-brand mb-2`;
  gestureText.textContent = text;
  if (percent !== null) {
    gestureProgress.parentElement.classList.remove("hidden");
    gestureProgress.style.width = `${percent}%`;
  } else {
    gestureProgress.parentElement.classList.add("hidden");
  }
  gestureIndicator.style.opacity = "1";
}

// Volume & Mute
function setPlayerVolume(val) {
  videoEl.volume = val;
  document.getElementById("volumeSlider").value = val;
}

function toggleMute() {
  videoEl.muted = !videoEl.muted;
  document.getElementById("muteBtn").innerHTML = videoEl.muted 
    ? '<i class="ph-bold ph-speaker-slash text-lg text-red-400"></i>' 
    : '<i class="ph-bold ph-speaker-high text-lg"></i>';
  showToast(videoEl.muted ? "Muted" : "Unmuted");
}

// ========================================================
// ASPECT RATIO, SPEED, PIP, SCREENSHOT
// ========================================================
function cycleAspectRatio() {
  currentAspectRatioIndex = (currentAspectRatioIndex + 1) % aspectRatios.length;
  const ratio = aspectRatios[currentAspectRatioIndex];
  videoEl.className = `w-full h-full video-aspect-${ratio} transition-all duration-300`;
  document.getElementById("aspectBtn").textContent = aspectRatioLabels[currentAspectRatioIndex];
  showToast(`Aspect Ratio: ${aspectRatioLabels[currentAspectRatioIndex]}`);
}

function toggleSpeedMenu() {
  const menu = document.getElementById("speedMenu");
  menu.classList.toggle("hidden");
}

function setPlaybackSpeed(speed) {
  videoEl.playbackRate = speed;
  document.getElementById("speedBtn").textContent = `${speed}x`;
  document.getElementById("speedMenu").classList.add("hidden");
  showToast(`Playback Speed: ${speed}x`);
}

function togglePiP() {
  if (document.pictureInPictureElement) {
    document.exitPictureInPicture();
  } else if (document.pictureInPictureEnabled) {
    videoEl.requestPictureInPicture().catch(() => {
      showToast("Picture-in-Picture not supported on this video");
    });
  }
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    playerModal.requestFullscreen().catch(() => {});
  } else {
    document.exitFullscreen().catch(() => {});
  }
}

function toggleNightMode() {
  isNightMode = !isNightMode;
  if (isNightMode) {
    videoEl.classList.add("night-filter");
    document.getElementById("nightModeBtn").classList.add("text-brand");
    showToast("Night Mode Filter: ON");
  } else {
    videoEl.classList.remove("night-filter");
    document.getElementById("nightModeBtn").classList.remove("text-brand");
    showToast("Night Mode Filter: OFF");
  }
}

function captureVideoFrame() {
  const canvas = document.createElement("canvas");
  canvas.width = videoEl.videoWidth || 1920;
  canvas.height = videoEl.videoHeight || 1080;
  const ctx = canvas.getContext("2d");
  ctx.drawImage(videoEl, 0, 0, canvas.width, canvas.height);
  
  const link = document.createElement("a");
  link.download = `XPlayer_Snapshot_${Date.now()}.png`;
  link.href = canvas.toDataURL("image/png");
  link.click();
  showToast("Snapshot saved to downloads!");
}

// ========================================================
// 5-BAND AUDIO EQUALIZER (WEB AUDIO API)
// ========================================================
function setupAudioEqualizer() {
  if (audioCtx) return;
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    audioCtx = new AudioContext();
    audioSource = audioCtx.createMediaElementSource(videoEl);

    // 5 peaking frequency bands: 60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz
    const freqs = [60, 230, 910, 3600, 14000];
    biquadFilters = freqs.map(freq => {
      const filter = audioCtx.createBiquadFilter();
      filter.type = "peaking";
      filter.frequency.value = freq;
      filter.gain.value = 0;
      return filter;
    });

    bassBoostNode = audioCtx.createBiquadFilter();
    bassBoostNode.type = "lowshelf";
    bassBoostNode.frequency.value = 100;
    bassBoostNode.gain.value = 0;

    // Chain nodes: source -> filter1 -> ... -> filter5 -> bassBoost -> destination
    let lastNode = audioSource;
    biquadFilters.forEach(f => {
      lastNode.connect(f);
      lastNode = f;
    });
    lastNode.connect(bassBoostNode);
    bassBoostNode.connect(audioCtx.destination);
  } catch (err) {
    console.warn("Audio Context init deferred:", err);
  }
}

function openEqualizerModal() {
  document.getElementById("equalizerModal").classList.remove("hidden");
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
}

function closeEqualizerModal() {
  document.getElementById("equalizerModal").classList.add("hidden");
}

function updateEqBand(bandIndex, gainVal) {
  if (biquadFilters[bandIndex]) {
    biquadFilters[bandIndex].gain.value = parseFloat(gainVal);
  }
}

function toggleBassBoost(enabled) {
  if (bassBoostNode) {
    bassBoostNode.gain.value = enabled ? 10 : 0;
    showToast(enabled ? "Bass Boost 200% Active" : "Bass Boost Off");
  }
}

function applyEqPreset(preset) {
  const presets = {
    flat: [0, 0, 0, 0, 0],
    bass: [8, 5, 1, 0, -1],
    rock: [5, 3, -1, 3, 6],
    vocal: [-2, 1, 5, 4, 1],
    movie: [4, 2, -2, 3, 5]
  };
  const vals = presets[preset] || presets.flat;
  vals.forEach((v, idx) => updateEqBand(idx, v));
  showToast(`Equalizer Preset: ${preset.toUpperCase()}`);
}

// ========================================================
// SUBTITLE LOADER (.SRT / .VTT)
// ========================================================
function loadCustomSubtitle(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (e) => {
    const text = e.target.result;
    parseSubtitles(text);
    showToast(`Loaded Subtitle: ${file.name}`);
  };
  reader.readAsText(file);
}

function parseSubtitles(raw) {
  // Simple SRT / VTT parser into timestamp cues
  parsedSubtitles = [];
  const normalized = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const blocks = normalized.split('\n\n');

  blocks.forEach(block => {
    const lines = block.trim().split('\n');
    if (lines.length >= 2) {
      const timeLine = lines.find(l => l.includes('-->'));
      if (timeLine) {
        const [startStr, endStr] = timeLine.split('-->').map(s => s.trim());
        const textLines = lines.slice(lines.indexOf(timeLine) + 1).join(' ');
        parsedSubtitles.push({
          start: parseSubTime(startStr),
          end: parseSubTime(endStr),
          text: textLines
        });
      }
    }
  });

  const subDisplay = document.getElementById("customSubtitleDisplay");
  videoEl.addEventListener("timeupdate", () => {
    const t = videoEl.currentTime;
    const match = parsedSubtitles.find(s => t >= s.start && t <= s.end);
    if (match) {
      subDisplay.textContent = match.text;
      subDisplay.classList.remove("hidden");
    } else {
      subDisplay.classList.add("hidden");
    }
  });
}

function parseSubTime(str) {
  const parts = str.replace(',', '.').split(':');
  if (parts.length === 3) {
    return parseFloat(parts[0]) * 3600 + parseFloat(parts[1]) * 60 + parseFloat(parts[2]);
  }
  return 0;
}

// ========================================================
// UTILITIES & TOASTS
// ========================================================
function handleSearch(query) {
  const q = query.toLowerCase().trim();
  if (!q) {
    renderVideos();
    return;
  }
  const filtered = videos.filter(v => 
    !v.isVault && !v.isTrash && (
      v.title.toLowerCase().includes(q) ||
      v.format.toLowerCase().includes(q) ||
      v.folder.toLowerCase().includes(q)
    )
  );
  renderVideos(filtered);
}

function handleSort(criteria) {
  if (criteria === "name") {
    videos.sort((a, b) => a.title.localeCompare(b.title));
  } else if (criteria === "duration") {
    videos.sort((a, b) => b.durationSec - a.durationSec);
  } else if (criteria === "size") {
    videos.sort((a, b) => parseFloat(b.size) - parseFloat(a.size));
  } else if (criteria === "date") {
    videos.sort((a, b) => b.date.localeCompare(a.date));
  }
  renderVideos();
}

function toggleTheme() {
  const themes = ['#ff9800', '#00e5ff', '#00e676', '#e040fb'];
  const randomColor = themes[Math.floor(Math.random() * themes.length)];
  document.documentElement.style.setProperty('--brand-color', randomColor);
  showToast("Theme Accent Updated");
}

function toggleSleepTimerPrompt() {
  const mins = prompt("Set Sleep Timer (minutes):", "30");
  if (mins && !isNaN(mins)) {
    setTimeout(() => {
      videoEl.pause();
      showToast("Sleep Timer: Video Paused");
    }, parseInt(mins) * 60 * 1000);
    showToast(`Sleep Timer active: ${mins} minutes`);
  }
}

function showToast(msg) {
  const toast = document.getElementById("toastNotification");
  document.getElementById("toastMsg").textContent = msg;
  toast.classList.remove("opacity-0", "pointer-events-none");
  setTimeout(() => {
    toast.classList.add("opacity-0", "pointer-events-none");
  }, 2500);
}

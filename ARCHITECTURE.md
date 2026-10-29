# REA Architecture & Reverse-Engineering Report: XPlayer (Video Player All Format)

**Analyzed Target:** `com.inshot.xplayer` (Release 1.6.3)  
**Binary Artifacts:** `base.apk`, `split_config.ar.apk`, `split_config.en.apk`, `split_config.arm64_v8a.apk`, `split_config.xxhdpi.apk`  
**Tooling:** REA (Reverse Engineer Anything) v6.8.0

---

## 1. Application Overview & Metadata

- **Package Name:** `video.player.videoplayer` (Internal codebase namespace: `com.inshot.xplayer`)
- **Version:** `1.6.3` (Target API Level: 34 - Android 14)
- **Primary Category:** Media & Video / High-performance video player
- **Architecture Support:** `arm64-v8a` native acceleration libraries + Java/Kotlin DEX bytecode (Multi-DEX across 3 DEX archives: `classes.dex`, `classes2.dex`, `classes3.dex`)

---

## 2. Core Architecture & Component Hierarchy

### Key Activities (Observed via AXML Analysis)
1. **`com.inshot.xplayer.activities.PlayerActivity`**:
   The primary media playback engine. Handles video decoding, OpenGL ES rendering surface, gesture dispatchers, audio track selection, and hardware/software acceleration switches.
2. **`com.inshot.xplayer.activities.BackgroundPlayerActivity` & `com.inshot.xplayer.service.PipPlayerService`**:
   Background playback service & Picture-in-Picture (PiP) floating window manager.
3. **`com.inshot.xplayer.activities.FileExplorerActivity` & `MainActivity`**:
   Local media scanner and file browser. Categorizes media by folders, timestamps, size, and resolutions.
4. **`com.inshot.xplayer.activities.FileProtectionActivity`**:
   Private Vault ("Secret Folder") with PIN / Biometric (Fingerprint) authentication to hide sensitive media files.
5. **`com.inshot.xplayer.activities.EqualizerActivity`**:
   Multi-band audio frequency processing, bass boost, and audio virtualizer.
6. **`com.inshot.xplayer.activities.ThemeActivity` & `CustomThemeActivity`**:
   User interface styling engine supporting multiple dark/light color palettes.
7. **`com.inshot.xplayer.activities.MusicPlayActivity`**:
   Audio-only playback mode with media notification controls.
8. **`com.inshot.xplayer.trash.TrashActivity`**:
   Recycle bin mechanism for soft-deleting and restoring media files.
9. **`com.inshot.cast.xcast.ControlActivity`**:
   Google Cast / DLNA remote streaming client.

---

## 3. Playback Engine & Gesture System
XPlayer implements a gesture system directly over the video surface:
- **Left Vertical Swipe:** Display brightness adjustment (0% – 100%).
- **Right Vertical Swipe:** Audio volume adjustment (0% – 100% with amplification).
- **Horizontal Swipe:** Precise video scrubbing / seeking with elapsed/total time overlay.
- **Double Tap Left/Right:** Quick 10-second skip rewind / fast-forward.
- **Pinch-to-Zoom:** Scalable video viewport (Aspect ratio cycling: Fit, 16:9, 4:3, Fill, Original).
- **Controls Auto-Hide:** Auto-dismissal of HUD controls after 3 seconds of inactivity.
- **Screen Lock:** Freezes UI controls to prevent accidental touch interactions during playback.

---

## 4. Reconstructed Web Clone Features
Based on this analysis, the clone application in `/app` implements:
- Full responsive dark interface styled in line with XPlayer's native design.
- Complete gesture controls (Brightness slider, Volume slider, Scrub seek bar, 10s skip).
- Video source support: Local file browser, drag-and-drop video playback, and built-in HD test streams.
- Full format audio player mode with visualizer.
- 5-band Web Audio API Equalizer.
- Subtitle parser & customization (.SRT & .VTT).
- Video screenshot frame grabber.
- PIN-protected Private Vault (`1234` default).
- Night mode filter and Sleep timer.

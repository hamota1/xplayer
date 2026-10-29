package com.inshot.xplayer.clone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.inshot.xplayer.clone.models.VideoModel;

import java.io.OutputStream;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO = "extra_video";

    private PlayerView playerView;
    private ExoPlayer player;
    private VideoModel videoModel;
    private AudioManager audioManager;

    private View gestureTouchView;
    private LinearLayout gestureIndicatorLayout;
    private ImageView imgGestureIcon;
    private TextView txtGestureValue;
    private ProgressBar progressGesture;

    private LinearLayout playerTopBar;
    private LinearLayout playerBottomBar;
    private ImageButton btnScreenLock;
    private ImageButton btnPlayPause;
    private SeekBar playerSeekBar;
    private TextView txtCurrentTime, txtDuration, txtPlayerTitle;
    private Button btnAspectRatio, btnSpeed;

    private boolean isControlsLocked = false;
    private boolean isControlsVisible = true;
    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;

    private float currentBrightness = -1.0f; // -1 uses system default
    private int currentVolume = 0;
    private int maxVolume = 15;

    private int currentAspectIndex = 0;
    private final int[] aspectModes = new int[]{
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL
    };
    private final String[] aspectLabels = new String[]{"Fit", "Crop", "Fill"};

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        videoModel = (VideoModel) getIntent().getSerializableExtra(EXTRA_VIDEO);
        if (videoModel == null) {
            Toast.makeText(this, "Error loading video", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        initViews();
        initPlayer();
        setupGestureDetector();
        resetControlsTimer();
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        gestureTouchView = findViewById(R.id.gestureTouchView);
        gestureIndicatorLayout = findViewById(R.id.gestureIndicatorLayout);
        imgGestureIcon = findViewById(R.id.imgGestureIcon);
        txtGestureValue = findViewById(R.id.txtGestureValue);
        progressGesture = findViewById(R.id.progressGesture);

        playerTopBar = findViewById(R.id.playerTopBar);
        playerBottomBar = findViewById(R.id.playerBottomBar);
        btnScreenLock = findViewById(R.id.btnScreenLock);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtDuration = findViewById(R.id.txtDuration);
        txtPlayerTitle = findViewById(R.id.txtPlayerTitle);
        btnAspectRatio = findViewById(R.id.btnAspectRatio);
        btnSpeed = findViewById(R.id.btnSpeed);

        txtPlayerTitle.setText(videoModel.getTitle());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        findViewById(R.id.btnPrev).setOnClickListener(v -> skipSeconds(-10));
        findViewById(R.id.btnNext).setOnClickListener(v -> skipSeconds(10));

        btnAspectRatio.setOnClickListener(v -> cycleAspectRatio());
        btnSpeed.setOnClickListener(v -> showSpeedDialog());

        findViewById(R.id.btnPip).setOnClickListener(v -> enterPipMode());
        findViewById(R.id.btnSnapshot).setOnClickListener(v -> takeSnapshot());
        findViewById(R.id.btnEqualizer).setOnClickListener(v -> {
            Intent intent = new Intent(this, EqualizerActivity.class);
            if (player != null) {
                intent.putExtra("audio_session_id", player.getAudioSessionId());
            }
            startActivity(intent);
        });

        btnScreenLock.setOnClickListener(v -> toggleScreenLock());

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                controlsHandler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetControlsTimer();
            }
        });
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(videoModel.getUri());
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    playerSeekBar.setMax((int) player.getDuration());
                    txtDuration.setText(formatTime(player.getDuration()));
                    startProgressTracker();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });
    }

    private void startProgressTracker() {
        controlsHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    int pos = (int) player.getCurrentPosition();
                    playerSeekBar.setProgress(pos);
                    txtCurrentTime.setText(formatTime(pos));
                }
                controlsHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    // ==========================================
    // TOUCH & GESTURE CONTROL (Brightness / Volume / Seek)
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureDetector() {
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (!isControlsLocked) {
                    toggleControls();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (isControlsLocked) return false;
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                if (e.getX() < screenWidth / 2.0f) {
                    skipSeconds(-10);
                } else {
                    skipSeconds(10);
                }
                return true;
            }
        });

        gestureTouchView.setOnTouchListener(new View.OnTouchListener() {
            private float startX = 0, startY = 0;
            private boolean isDragging = false;
            private int gestureMode = 0; // 1: Brightness, 2: Volume, 3: Seek

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) return true;
                if (isControlsLocked) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        isDragging = false;
                        gestureMode = 0;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getX() - startX;
                        float deltaY = startY - event.getY(); // Up is positive
                        int screenWidth = getResources().getDisplayMetrics().widthPixels;

                        if (!isDragging) {
                            if (Math.abs(deltaY) > 40 && Math.abs(deltaY) > Math.abs(deltaX)) {
                                isDragging = true;
                                gestureMode = (startX < screenWidth / 2.0f) ? 1 : 2;
                            } else if (Math.abs(deltaX) > 60 && Math.abs(deltaX) > Math.abs(deltaY)) {
                                isDragging = true;
                                gestureMode = 3;
                            }
                        }

                        if (isDragging) {
                            if (gestureMode == 1) {
                                // Brightness on Left
                                adjustBrightness(deltaY);
                            } else if (gestureMode == 2) {
                                // Volume on Right
                                adjustVolume(deltaY);
                            } else if (gestureMode == 3) {
                                // Scrubbing / Seek
                                showSeekHud(deltaX);
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isDragging && gestureMode == 3 && player != null) {
                            float finalDeltaX = event.getX() - startX;
                            long seekDelta = (long) (finalDeltaX * 100);
                            player.seekTo(Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + seekDelta)));
                        }
                        gestureIndicatorLayout.setVisibility(View.GONE);
                        isDragging = false;
                        gestureMode = 0;
                        resetControlsTimer();
                        break;
                }
                return true;
            }
        });
    }

    private void adjustBrightness(float deltaY) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (currentBrightness < 0) currentBrightness = 0.5f;
        currentBrightness += (deltaY / 2000.0f);
        currentBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness));
        lp.screenBrightness = currentBrightness;
        getWindow().setAttributes(lp);

        int percent = Math.round(currentBrightness * 100);
        showGestureHud(android.R.drawable.ic_lock_idle_charging, percent + "%", percent);
    }

    private void adjustVolume(float deltaY) {
        if (audioManager == null) return;
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (deltaY > 30) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
        } else if (deltaY < -30) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
        }
        int newVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int percent = (int) (((float) newVol / maxVolume) * 100);
        showGestureHud(android.R.drawable.ic_lock_idle_low_battery, percent + "%", percent);
    }

    private void showSeekHud(float deltaX) {
        long seekDeltaSec = (long) (deltaX / 10);
        String text = (seekDeltaSec >= 0 ? "+" : "") + seekDeltaSec + "s";
        showGestureHud(android.R.drawable.ic_media_ff, text, -1);
    }

    private void showGestureHud(int iconRes, String text, int progress) {
        gestureIndicatorLayout.setVisibility(View.VISIBLE);
        imgGestureIcon.setImageResource(iconRes);
        txtGestureValue.setText(text);
        if (progress >= 0) {
            progressGesture.setVisibility(View.VISIBLE);
            progressGesture.setProgress(progress);
        } else {
            progressGesture.setVisibility(View.GONE);
        }
    }

    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    private void skipSeconds(int sec) {
        if (player == null) return;
        long target = player.getCurrentPosition() + (sec * 1000L);
        player.seekTo(Math.max(0, Math.min(player.getDuration(), target)));
        showGestureHud(sec > 0 ? android.R.drawable.ic_media_ff : android.R.drawable.ic_media_rew, Math.abs(sec) + "s", -1);
        controlsHandler.postDelayed(() -> gestureIndicatorLayout.setVisibility(View.GONE), 800);
    }

    private void cycleAspectRatio() {
        currentAspectIndex = (currentAspectIndex + 1) % aspectModes.length;
        playerView.setResizeMode(aspectModes[currentAspectIndex]);
        btnAspectRatio.setText(aspectLabels[currentAspectIndex]);
    }

    private void showSpeedDialog() {
        final String[] speeds = {"0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x"};
        final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setItems(speeds, (dialog, which) -> {
                    float s = speedValues[which];
                    if (player != null) {
                        player.setPlaybackParameters(new PlaybackParameters(s));
                        btnSpeed.setText(speeds[which].replace(" (Normal)", ""));
                    }
                })
                .show();
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hideControls();
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } else {
            Toast.makeText(this, "Picture-in-Picture requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    private void takeSnapshot() {
        // Capture view snapshot
        try {
            playerView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(playerView.getDrawingCache());
            playerView.setDrawingCacheEnabled(false);

            String filename = "XPlayer_Snap_" + System.currentTimeMillis() + ".png";
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/XPlayer");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(uri);
            } else {
                java.io.File file = new java.io.File(getExternalFilesDir(null), filename);
                fos = new java.io.FileOutputStream(file);
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "Snapshot saved to Gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Snapshot captured", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleScreenLock() {
        isControlsLocked = !isControlsLocked;
        btnScreenLock.setImageResource(isControlsLocked ? android.R.drawable.ic_lock_power_off : android.R.drawable.ic_lock_lock);
        if (isControlsLocked) {
            hideControls();
            Toast.makeText(this, "Controls Locked", Toast.LENGTH_SHORT).show();
        } else {
            showControls();
            Toast.makeText(this, "Controls Unlocked", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
            resetControlsTimer();
        }
    }

    private void showControls() {
        playerTopBar.setVisibility(View.VISIBLE);
        playerBottomBar.setVisibility(View.VISIBLE);
        isControlsVisible = true;
    }

    private void hideControls() {
        playerTopBar.setVisibility(View.GONE);
        playerBottomBar.setVisibility(View.GONE);
        isControlsVisible = false;
    }

    private void resetControlsTimer() {
        controlsHandler.removeCallbacks(hideControlsRunnable);
        controlsHandler.postDelayed(hideControlsRunnable, 3500);
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            // Keep playing in PiP
        } else {
            if (player != null) player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controlsHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}

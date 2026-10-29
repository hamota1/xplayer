package com.inshot.xplayer.clone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.inshot.xplayer.clone.adapters.FolderAdapter;
import com.inshot.xplayer.clone.adapters.VideoAdapter;
import com.inshot.xplayer.clone.models.FolderModel;
import com.inshot.xplayer.clone.models.VideoModel;
import com.inshot.xplayer.clone.utils.MediaScanner;
import com.inshot.xplayer.clone.utils.PrivateVaultManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 101;

    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private EditText editSearch;
    private LinearLayout layoutPermission;
    private TextView txtEmpty;

    private List<VideoModel> allVideos = new ArrayList<>();
    private List<FolderModel> allFolders = new ArrayList<>();
    private int currentTab = 0; // 0: All Videos, 1: Folders

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewMain);
        tabLayout = findViewById(R.id.tabLayout);
        editSearch = findViewById(R.id.editSearch);
        layoutPermission = findViewById(R.id.layoutPermission);
        txtEmpty = findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnVaultMain).setOnClickListener(v -> {
            startActivity(new Intent(this, VaultActivity.class));
        });

        findViewById(R.id.btnEqualizerMain).setOnClickListener(v -> {
            startActivity(new Intent(this, EqualizerActivity.class));
        });

        Button btnGrantPermission = findViewById(R.id.btnGrantPermission);
        btnGrantPermission.setOnClickListener(v -> requestStoragePermission());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateListDisplay();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        checkAndLoadVideos();
    }

    private void checkAndLoadVideos() {
        if (hasStoragePermission()) {
            layoutPermission.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            loadDeviceVideos();
        } else {
            layoutPermission.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            txtEmpty.setVisibility(View.GONE);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_REQ_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndLoadVideos();
            } else {
                Toast.makeText(this, "Permission is required to display your videos", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadDeviceVideos() {
        new Thread(() -> {
            List<VideoModel> scanned = MediaScanner.getAllVideos(this);
            List<FolderModel> folders = MediaScanner.getFoldersFromVideos(scanned);

            runOnUiThread(() -> {
                allVideos = scanned;
                allFolders = folders;
                updateListDisplay();
            });
        }).start();
    }

    private void updateListDisplay() {
        if (currentTab == 0) {
            // All Videos
            if (allVideos.isEmpty()) {
                txtEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                txtEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                VideoAdapter adapter = new VideoAdapter(this, allVideos, new VideoAdapter.OnVideoClickListener() {
                    @Override
                    public void onVideoClick(VideoModel video, int position) {
                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                        intent.putExtra(PlayerActivity.EXTRA_VIDEO, video);
                        startActivity(intent);
                    }

                    @Override
                    public void onMoveToVault(VideoModel video) {
                        if (PrivateVaultManager.moveToVault(MainActivity.this, video)) {
                            allVideos.remove(video);
                            updateListDisplay();
                            Toast.makeText(MainActivity.this, "Moved to Private Vault", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                recyclerView.setAdapter(adapter);
            }
        } else {
            // Folders Tab
            if (allFolders.isEmpty()) {
                txtEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                txtEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                FolderAdapter adapter = new FolderAdapter(this, allFolders, folder -> {
                    // Filter videos by folder
                    List<VideoModel> folderVideos = new ArrayList<>();
                    for (VideoModel v : allVideos) {
                        if (v.getFolder().equalsIgnoreCase(folder.getName())) {
                            folderVideos.add(v);
                        }
                    }
                    tabLayout.getTabAt(0).select();
                    allVideos = folderVideos;
                    updateListDisplay();
                });
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private void filterSearch(String query) {
        if (query.isEmpty()) {
            loadDeviceVideos();
            return;
        }
        List<VideoModel> filtered = new ArrayList<>();
        for (VideoModel v : allVideos) {
            if (v.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                v.getFolder().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(v);
            }
        }
        VideoAdapter adapter = new VideoAdapter(this, filtered, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoModel video, int position) {
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra(PlayerActivity.EXTRA_VIDEO, video);
                startActivity(intent);
            }
            @Override public void onMoveToVault(VideoModel video) {}
        });
        recyclerView.setAdapter(adapter);
    }
}

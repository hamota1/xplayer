package com.inshot.xplayer.clone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.inshot.xplayer.clone.adapters.VideoAdapter;
import com.inshot.xplayer.clone.models.VideoModel;
import com.inshot.xplayer.clone.utils.PrivateVaultManager;

import java.util.List;

public class VaultActivity extends AppCompatActivity {

    private LinearLayout layoutPinLock;
    private LinearLayout layoutVaultContent;
    private TextView txtPinDots;
    private TextView txtPinError;
    private RecyclerView recyclerVault;

    private String enteredPin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        layoutPinLock = findViewById(R.id.layoutPinLock);
        layoutVaultContent = findViewById(R.id.layoutVaultContent);
        txtPinDots = findViewById(R.id.txtPinDots);
        txtPinError = findViewById(R.id.txtPinError);
        recyclerVault = findViewById(R.id.recyclerVault);

        findViewById(R.id.btnBackVault).setOnClickListener(v -> finish());

        setupPinPad();
    }

    private void setupPinPad() {
        int[] numButtons = {
                R.id.btnPin0, R.id.btnPin1, R.id.btnPin2, R.id.btnPin3, R.id.btnPin4,
                R.id.btnPin5, R.id.btnPin6, R.id.btnPin7, R.id.btnPin8, R.id.btnPin9
        };

        for (int i = 0; i <= 9; i++) {
            final String digit = String.valueOf(i);
            findViewById(numButtons[i]).setOnClickListener(v -> appendPin(digit));
        }

        findViewById(R.id.btnPinC).setOnClickListener(v -> {
            enteredPin = "";
            updatePinDisplay();
            txtPinError.setText("");
        });

        findViewById(R.id.btnPinBack).setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                updatePinDisplay();
            }
        });
    }

    private void appendPin(String digit) {
        if (enteredPin.length() < 4) {
            enteredPin += digit;
            updatePinDisplay();
        }
        if (enteredPin.length() == 4) {
            verifyPin();
        }
    }

    private void updatePinDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < enteredPin.length()) {
                sb.append("●  ");
            } else {
                sb.append("○  ");
            }
        }
        txtPinDots.setText(sb.toString().trim());
    }

    private void verifyPin() {
        if (PrivateVaultManager.checkPin(this, enteredPin)) {
            layoutPinLock.setVisibility(View.GONE);
            layoutVaultContent.setVisibility(View.VISIBLE);
            loadVaultVideos();
            Toast.makeText(this, "Vault Unlocked", Toast.LENGTH_SHORT).show();
        } else {
            txtPinError.setText("Incorrect PIN. Default is 1234");
            enteredPin = "";
            updatePinDisplay();
        }
    }

    private void loadVaultVideos() {
        List<VideoModel> vaultVideos = PrivateVaultManager.getVaultVideos(this);
        recyclerVault.setLayoutManager(new LinearLayoutManager(this));
        VideoAdapter adapter = new VideoAdapter(this, vaultVideos, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoModel video, int position) {
                Intent intent = new Intent(VaultActivity.this, PlayerActivity.class);
                intent.putExtra(PlayerActivity.EXTRA_VIDEO, video);
                startActivity(intent);
            }

            @Override
            public void onMoveToVault(VideoModel video) {
                // Already in vault
            }
        });
        recyclerVault.setAdapter(adapter);
    }
}

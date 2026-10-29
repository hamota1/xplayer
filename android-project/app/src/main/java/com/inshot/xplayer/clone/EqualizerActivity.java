package com.inshot.xplayer.clone;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class EqualizerActivity extends AppCompatActivity {

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private LinearLayout layoutBands;
    private SeekBar seekBassBoost;
    private Spinner spinnerPresets;
    private Switch switchEqEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        int audioSessionId = getIntent().getIntExtra("audio_session_id", 0);

        layoutBands = findViewById(R.id.layoutBands);
        seekBassBoost = findViewById(R.id.seekBassBoost);
        spinnerPresets = findViewById(R.id.spinnerPresets);
        switchEqEnabled = findViewById(R.id.switchEqEnabled);

        findViewById(R.id.btnBackEq).setOnClickListener(v -> finish());

        try {
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);

            bassBoost = new BassBoost(0, audioSessionId);
            bassBoost.setEnabled(true);

            setupEqualizerUI();
            setupPresetsUI();
            setupBassBoostUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Equalizer not supported on this device/session", Toast.LENGTH_SHORT).show();
        }

        switchEqEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (equalizer != null) equalizer.setEnabled(isChecked);
            if (bassBoost != null) bassBoost.setEnabled(isChecked);
        });
    }

    private void setupEqualizerUI() {
        if (equalizer == null) return;
        short bands = equalizer.getNumberOfBands();
        final short minEqLevel = equalizer.getBandLevelRange()[0];
        final short maxEqLevel = equalizer.getBandLevelRange()[1];

        layoutBands.removeAllViews();

        for (short i = 0; i < bands; i++) {
            final short bandIndex = i;
            int centerFreq = equalizer.getCenterFreq(bandIndex) / 1000;

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            col.setLayoutParams(colParams);

            TextView txtLevel = new TextView(this);
            txtLevel.setTextColor(getResources().getColor(R.color.text_secondary));
            txtLevel.setTextSize(10);
            txtLevel.setText(String.format("%ddB", equalizer.getBandLevel(bandIndex) / 100));

            SeekBar bar = new SeekBar(this);
            bar.setMax(maxEqLevel - minEqLevel);
            bar.setProgress(equalizer.getBandLevel(bandIndex) - minEqLevel);
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.brand_orange)));
            bar.setThumbTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.brand_orange)));

            // Horizontal seekbar for simplicity
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            bar.setLayoutParams(barParams);

            TextView txtFreq = new TextView(this);
            txtFreq.setTextColor(getResources().getColor(R.color.text_primary));
            txtFreq.setTextSize(11);
            txtFreq.setText(centerFreq >= 1000 ? (centerFreq / 1000) + "k" : centerFreq + "Hz");

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && equalizer != null) {
                        short newLevel = (short) (progress + minEqLevel);
                        equalizer.setBandLevel(bandIndex, newLevel);
                        txtLevel.setText(String.format("%ddB", newLevel / 100));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            col.addView(txtLevel);
            col.addView(bar);
            col.addView(txtFreq);
            layoutBands.addView(col);
        }
    }

    private void setupPresetsUI() {
        if (equalizer == null) return;
        short presetsCount = equalizer.getNumberOfPresets();
        List<String> presetNames = new ArrayList<>();
        for (short i = 0; i < presetsCount; i++) {
            presetNames.add(equalizer.getPresetName(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, presetNames);
        spinnerPresets.setAdapter(adapter);

        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (equalizer != null) {
                    equalizer.usePreset((short) position);
                    setupEqualizerUI();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupBassBoostUI() {
        if (bassBoost == null) return;
        seekBassBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bassBoost != null) {
                    bassBoost.setStrength((short) progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if (bassBoost != null) {
            bassBoost.release();
            bassBoost = null;
        }
    }
}

package com.inshot.xplayer.clone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.inshot.xplayer.clone.models.VideoModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class PrivateVaultManager {
    private static final String PREF_NAME = "xplayer_vault";
    private static final String KEY_PIN = "vault_pin";
    private static final String DEFAULT_PIN = "1234";

    public static boolean checkPin(Context context, String enteredPin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedPin = prefs.getString(KEY_PIN, DEFAULT_PIN);
        return savedPin.equals(enteredPin);
    }

    public static void setPin(Context context, String newPin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PIN, newPin).apply();
    }

    public static File getVaultDirectory(Context context) {
        File vaultDir = new File(context.getFilesDir(), "private_vault");
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
            try {
                // Ensure media scanner ignores this folder
                new File(vaultDir, ".nomedia").createNewFile();
            } catch (Exception ignored) {}
        }
        return vaultDir;
    }

    public static List<VideoModel> getVaultVideos(Context context) {
        List<VideoModel> list = new ArrayList<>();
        File vaultDir = getVaultDirectory(context);
        File[] files = vaultDir.listFiles();
        if (files != null) {
            long fakeId = 1000;
            for (File f : files) {
                if (!f.getName().equals(".nomedia") && f.isFile()) {
                    list.add(new VideoModel(
                            fakeId++,
                            f.getName(),
                            f.getAbsolutePath(),
                            0,
                            f.length(),
                            f.lastModified() / 1000,
                            "Private",
                            "Vault",
                            android.net.Uri.fromFile(f)
                    ));
                }
            }
        }
        return list;
    }

    public static boolean moveToVault(Context context, VideoModel video) {
        try {
            File src = new File(video.getPath());
            if (!src.exists()) return false;
            File dest = new File(getVaultDirectory(context), src.getName());
            return copyAndDelete(src, dest);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean restoreFromVault(Context context, VideoModel video, String restoreDirPath) {
        try {
            File src = new File(video.getPath());
            File destDir = new File(restoreDirPath);
            if (!destDir.exists()) destDir.mkdirs();
            File dest = new File(destDir, src.getName());
            return copyAndDelete(src, dest);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAndDelete(File src, File dest) {
        try (FileChannel in = new FileInputStream(src).getChannel();
             FileChannel out = new FileOutputStream(dest).getChannel()) {
            in.transferTo(0, in.size(), out);
            return src.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

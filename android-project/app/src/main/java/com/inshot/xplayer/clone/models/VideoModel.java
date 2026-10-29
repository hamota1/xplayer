package com.inshot.xplayer.clone.models;

import android.net.Uri;
import java.io.Serializable;
import java.util.Locale;

public class VideoModel implements Serializable {
    private final long id;
    private final String title;
    private final String path;
    private final long durationMs;
    private final long sizeBytes;
    private final long dateModified;
    private final String resolution;
    private final String folder;
    private final String uriString;

    public VideoModel(long id, String title, String path, long durationMs, long sizeBytes, long dateModified, String resolution, String folder, Uri uri) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.durationMs = durationMs;
        this.sizeBytes = sizeBytes;
        this.dateModified = dateModified;
        this.resolution = resolution != null ? resolution : "HD";
        this.folder = folder != null ? folder : "Videos";
        this.uriString = uri != null ? uri.toString() : "";
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getPath() { return path; }
    public long getDurationMs() { return durationMs; }
    public long getSizeBytes() { return sizeBytes; }
    public long getDateModified() { return dateModified; }
    public String getResolution() { return resolution; }
    public String getFolder() { return folder; }
    public Uri getUri() { return Uri.parse(uriString); }

    public String getFormattedDuration() {
        long totalSecs = durationMs / 1000;
        long hours = totalSecs / 3600;
        long mins = (totalSecs % 3600) / 60;
        long secs = totalSecs % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        }
    }

    public String getFormattedSize() {
        if (sizeBytes <= 0) return "0 MB";
        double mb = sizeBytes / (1024.0 * 1024.0);
        if (mb >= 1024.0) {
            double gb = mb / 1024.0;
            return String.format(Locale.getDefault(), "%.1f GB", gb);
        }
        return String.format(Locale.getDefault(), "%.1f MB", mb);
    }
}

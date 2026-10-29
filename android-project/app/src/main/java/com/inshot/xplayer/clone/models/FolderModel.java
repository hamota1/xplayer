package com.inshot.xplayer.clone.models;

import java.io.Serializable;

public class FolderModel implements Serializable {
    private final String name;
    private final String path;
    private int videoCount;

    public FolderModel(String name, String path, int videoCount) {
        this.name = name;
        this.path = path;
        this.videoCount = videoCount;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public int getVideoCount() { return videoCount; }
    public void incrementCount() { this.videoCount++; }
}

package com.inshot.xplayer.clone.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import com.inshot.xplayer.clone.models.FolderModel;
import com.inshot.xplayer.clone.models.VideoModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaScanner {

    public static List<VideoModel> getAllVideos(Context context) {
        List<VideoModel> videoList = new ArrayList<>();
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        };

        String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
                int bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

                do {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(dataColumn);
                    long duration = cursor.getLong(durationColumn);
                    long size = cursor.getLong(sizeColumn);
                    long date = cursor.getLong(dateColumn);
                    String folder = cursor.getString(bucketColumn);

                    if (name == null && path != null) {
                        name = new File(path).getName();
                    }
                    if (folder == null && path != null) {
                        File parent = new File(path).getParentFile();
                        folder = parent != null ? parent.getName() : "Videos";
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                    // Skip empty or corrupted 0 byte files
                    if (duration > 0 || size > 0) {
                        videoList.add(new VideoModel(id, name, path, duration, size, date, "HD", folder, contentUri));
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoList;
    }

    public static List<FolderModel> getFoldersFromVideos(List<VideoModel> videos) {
        Map<String, FolderModel> folderMap = new HashMap<>();
        for (VideoModel v : videos) {
            String folderName = v.getFolder();
            if (folderMap.containsKey(folderName)) {
                folderMap.get(folderName).incrementCount();
            } else {
                folderMap.put(folderName, new FolderModel(folderName, v.getPath(), 1));
            }
        }
        return new ArrayList<>(folderMap.values());
    }
}

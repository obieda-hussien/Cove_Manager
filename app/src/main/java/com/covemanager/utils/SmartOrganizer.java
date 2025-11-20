package com.covemanager.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmartOrganizer {
    private static final String TAG = "SmartOrganizer";

    public static class MisplacedFile {
        public File file;
        public String suggestedPath;

        public MisplacedFile(File file, String suggestedPath) {
            this.file = file;
            this.suggestedPath = suggestedPath;
        }
    }

    public interface ScanCallback {
        void onScanComplete(List<MisplacedFile> misplacedFiles);
        void onProgress(int scannedFiles);
    }

    public static void scanForMisplacedFiles(ScanCallback callback) {
        new Thread(() -> {
            List<MisplacedFile> result = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();

            // Standard directories to exclude from scanning (we assume files here are "correct")
            Set<String> standardDirs = new HashSet<>(Arrays.asList(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), // Often messy, but maybe user intends it? Let's skip scanning INSIDE downloads for now to avoid moving temp files
                new File(root, "Android").getAbsolutePath() // System files
            ));

            scanDirectory(root, standardDirs, result);

            callback.onScanComplete(result);
        }).start();
    }

    private static void scanDirectory(File dir, Set<String> excludePaths, List<MisplacedFile> result) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Don't scan hidden folders or excluded standard folders
                if (file.getName().startsWith(".")) continue;
                if (excludePaths.contains(file.getAbsolutePath())) continue;

                scanDirectory(file, excludePaths, result);
            } else {
                checkFile(file, result);
            }
        }
    }

    private static void checkFile(File file, List<MisplacedFile> result) {
        String name = file.getName().toLowerCase();
        String parent = file.getParent();

        // Image Logic
        if (isImage(name)) {
            String picturesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            result.add(new MisplacedFile(file, picturesPath + File.separator + "Organized_Images"));
        }
        // Video Logic
        else if (isVideo(name)) {
             String moviesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
             result.add(new MisplacedFile(file, moviesPath + File.separator + "Organized_Videos"));
        }
        // Audio Logic
        else if (isAudio(name)) {
            String musicPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            result.add(new MisplacedFile(file, musicPath + File.separator + "Organized_Audio"));
        }
        // Document Logic
        else if (isDocument(name)) {
            String documentsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
            result.add(new MisplacedFile(file, documentsPath + File.separator + "Organized_Documents"));
        }
    }

    private static boolean isImage(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".heic");
    }

    private static boolean isVideo(String name) {
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mov") || name.endsWith(".avi") || name.endsWith(".webm") || name.endsWith(".3gp");
    }

    private static boolean isAudio(String name) {
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".aac") || name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".flac");
    }

    private static boolean isDocument(String name) {
        return name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") || name.endsWith(".pptx");
    }

    public static boolean moveFile(File source, String destPath) {
        File destDir = new File(destPath).getParentFile();
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return source.renameTo(new File(destPath));
    }
}

package com.covemanager.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateDetector {
    public static class DuplicateGroup {
        public long size;
        public String hash; // Using size+name for speed in this demo, or MD5 for accuracy
        public List<File> files = new ArrayList<>();

        public DuplicateGroup(long size, String hash) {
            this.size = size;
            this.hash = hash;
        }
    }

    public interface ScanCallback {
        void onScanComplete(List<DuplicateGroup> duplicates);
        void onProgress(String currentPath);
    }

    public static void scanForDuplicates(File root, ScanCallback callback) {
        new Thread(() -> {
            Map<String, List<File>> fileMap = new HashMap<>();
            scanRecursively(root, fileMap, callback);

            List<DuplicateGroup> result = new ArrayList<>();
            for (Map.Entry<String, List<File>> entry : fileMap.entrySet()) {
                List<File> files = entry.getValue();
                if (files.size() > 1) {
                    // Found duplicates
                    long size = files.get(0).length();
                    // Ignore tiny files/empty files
                    if (size < 1024) continue;

                    DuplicateGroup group = new DuplicateGroup(size, entry.getKey());
                    group.files = files;
                    result.add(group);
                }
            }
            callback.onScanComplete(result);
        }).start();
    }

    private static void scanRecursively(File dir, Map<String, List<File>> fileMap, ScanCallback callback) {
        if (dir == null) return;
        File[] list = dir.listFiles();
        if (list == null) return;

        for (File f : list) {
            if (f.isDirectory()) {
                if (f.getName().startsWith(".")) continue; // Skip hidden
                // Skip Android/data for speed and permission
                if (f.getAbsolutePath().contains("Android/data")) continue;

                callback.onProgress(f.getAbsolutePath());
                scanRecursively(f, fileMap, callback);
            } else {
                // Key generation: Size + Name is fast but not 100% accurate.
                // Size + CRC32 is better.
                // For this implementation, let's use Size + Name to be safe without heavy IO.
                // A better approach for "Smart" manager would be Size -> if collision -> Hash first 1KB.
                String key = f.length() + "_" + f.getName();

                if (!fileMap.containsKey(key)) {
                    fileMap.put(key, new ArrayList<>());
                }
                fileMap.get(key).add(f);
            }
        }
    }
}

package com.covemanager;

import android.util.LruCache;
import java.io.File;

/**
 * Singleton cache manager for folder sizes to improve performance by avoiding redundant calculations.
 * Uses LruCache to automatically manage memory and evict least recently used entries.
 */
public class FolderSizeCache {
    private static final int CACHE_SIZE = 100; // Max number of folder paths to cache
    private static FolderSizeCache instance;
    private final LruCache<String, Long> cache;

    private FolderSizeCache() {
        cache = new LruCache<String, Long>(CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Long value) {
                // Each entry counts as 1 toward cache size
                return 1;
            }
        };
    }

    /**
     * Get the singleton instance of FolderSizeCache
     * @return FolderSizeCache instance
     */
    public static synchronized FolderSizeCache getInstance() {
        if (instance == null) {
            instance = new FolderSizeCache();
        }
        return instance;
    }

    /**
     * Get cached folder size
     * @param path Absolute path of the folder
     * @return Cached size in bytes, or null if not found
     */
    public Long getSize(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return cache.get(path);
    }

    /**
     * Store or update folder size in cache
     * @param path Absolute path of the folder
     * @param size Size in bytes
     */
    public void putSize(String path, long size) {
        if (path != null && !path.isEmpty() && size >= 0) {
            cache.put(path, size);
        }
    }

    /**
     * Remove a path and all its parent paths from the cache.
     * This is crucial for maintaining data freshness when folder contents change.
     * @param path Absolute path to invalidate
     */
    public void invalidatePath(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        // Remove the current path
        cache.remove(path);

        // Recursively invalidate parent directories
        File file = new File(path);
        String parentPath = file.getParent();
        
        // Continue until we reach the root directory
        if (parentPath != null && !parentPath.equals(path)) {
            invalidatePath(parentPath);
        }
    }

    /**
     * Clear all cached entries
     */
    public void clearCache() {
        cache.evictAll();
    }

    /**
     * Get current cache size for debugging
     * @return Number of entries in cache
     */
    public int getCacheSize() {
        return cache.size();
    }
}
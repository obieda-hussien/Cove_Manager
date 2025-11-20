package com.covemanager.utils;

import android.util.LruCache;
import com.covemanager.FileItem;
import java.util.List;

public class FileQueryCache {
    private static FileQueryCache instance;
    private LruCache<String, List<FileItem>> cache;
    private LruCache<String, Long> timestampCache;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private FileQueryCache() {
        // Cache up to 10 different query results (e.g., APKs, Images, Videos, etc.)
        cache = new LruCache<>(10);
        timestampCache = new LruCache<>(10);
    }

    public static synchronized FileQueryCache getInstance() {
        if (instance == null) {
            instance = new FileQueryCache();
        }
        return instance;
    }

    public void put(String queryType, List<FileItem> items) {
        cache.put(queryType, items);
        timestampCache.put(queryType, System.currentTimeMillis());
    }

    public List<FileItem> get(String queryType) {
        Long timestamp = timestampCache.get(queryType);
        if (timestamp == null || (System.currentTimeMillis() - timestamp > CACHE_TTL_MS)) {
            cache.remove(queryType);
            timestampCache.remove(queryType);
            return null;
        }
        return cache.get(queryType);
    }

    public void invalidate(String queryType) {
        cache.remove(queryType);
        timestampCache.remove(queryType);
    }

    public void clearAll() {
        cache.evictAll();
        timestampCache.evictAll();
    }
}

package com.covemanager.utils;

import java.io.File;
import java.util.Comparator;

public class FileSorter {

    public enum SortOption {
        NAME,
        SIZE,
        DATE,
        TYPE
    }

    public static Comparator<File> getComparator(SortOption option) {
        switch (option) {
            case NAME:
                return (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                };
            case SIZE:
                return (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    // For directories, we might not know the size instantly, so fallback to name for now.
                    // Or we assume directories are "smaller" or "larger".
                    // Let's sort by size for files, and name for folders (since folder size calc is async).
                    if (f1.isFile() && f2.isFile()) {
                        return Long.compare(f2.length(), f1.length()); // Descending
                    }
                    return f1.getName().compareToIgnoreCase(f2.getName());
                };
            case DATE:
                return (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return Long.compare(f2.lastModified(), f1.lastModified()); // Newest first
                };
            case TYPE:
                return (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    if (f1.isFile() && f2.isFile()) {
                        String ext1 = getExtension(f1.getName());
                        String ext2 = getExtension(f2.getName());
                        int res = ext1.compareToIgnoreCase(ext2);
                        if (res != 0) return res;
                    }
                    return f1.getName().compareToIgnoreCase(f2.getName());
                };
            default:
                return getComparator(SortOption.NAME);
        }
    }

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        if (i > 0) {
            return name.substring(i + 1);
        }
        return "";
    }
}
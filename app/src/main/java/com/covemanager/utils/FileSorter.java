package com.covemanager.utils;

import java.io.File;
import java.util.Comparator;

public class FileSorter {

    public enum SortField {
        NAME,
        SIZE,
        DATE,
        TYPE
    }

    public enum SortMode {
        FOLDERS_FIRST,
        FILES_FIRST,
        MIXED
    }

    public static Comparator<File> getComparator(SortField field, SortMode mode, boolean ascending) {
        return (f1, f2) -> {
            // Handle grouping (Folders First / Files First / Mixed)
            if (mode == SortMode.FOLDERS_FIRST) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
            } else if (mode == SortMode.FILES_FIRST) {
                if (f1.isDirectory() && !f2.isDirectory()) return 1;
                if (!f1.isDirectory() && f2.isDirectory()) return -1;
            }
            // If Mixed, or if both are same type (both folders or both files), sort by field

            int result = 0;
            switch (field) {
                case SIZE:
                    // For folders, we often don't have size, so fallback to name or treat as 0
                    long s1 = f1.isDirectory() ? 0 : f1.length();
                    long s2 = f2.isDirectory() ? 0 : f2.length();
                    result = Long.compare(s1, s2);
                    break;
                case DATE:
                    result = Long.compare(f1.lastModified(), f2.lastModified());
                    break;
                case TYPE:
                    String ext1 = getExtension(f1.getName());
                    String ext2 = getExtension(f2.getName());
                    result = ext1.compareToIgnoreCase(ext2);
                    break;
                case NAME:
                default:
                    result = f1.getName().compareToIgnoreCase(f2.getName());
                    break;
            }

            return ascending ? result : -result;
        };
    }

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        if (i > 0) {
            return name.substring(i + 1);
        }
        return "";
    }
}

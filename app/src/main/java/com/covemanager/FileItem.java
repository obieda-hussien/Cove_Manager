package com.covemanager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model class representing a file or folder in the file browser
 */
public class FileItem {
    private File file;
    private boolean isDirectory;
    private long size;
    private String name;
    private String path;
    private long lastModified;
    private boolean isSizeCalculating;

    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.name = file.getName();
        this.path = file.getAbsolutePath();
        this.lastModified = file.lastModified();
        this.isSizeCalculating = false;
        
        // For files, get size immediately. For directories, size will be calculated separately
        if (!isDirectory) {
            this.size = file.length();
        } else {
            this.size = -1; // Indicates size not calculated yet
        }
    }

    public File getFile() {
        return file;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(lastModified));
    }

    public String getFormattedSize() {
        if (size < 0) {
            return isSizeCalculating ? "Calculating..." : "Unknown";
        }
        
        if (size == 0) {
            return isDirectory ? "Empty" : "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double sizeInUnit = size;

        while (sizeInUnit >= 1024 && unitIndex < units.length - 1) {
            sizeInUnit /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return String.format(Locale.getDefault(), "%.0f %s", sizeInUnit, units[unitIndex]);
        } else {
            return String.format(Locale.getDefault(), "%.1f %s", sizeInUnit, units[unitIndex]);
        }
    }

    public boolean isSizeCalculating() {
        return isSizeCalculating;
    }

    public void setSizeCalculating(boolean calculating) {
        this.isSizeCalculating = calculating;
    }

    public String getParent() {
        return file.getParent();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileItem fileItem = (FileItem) obj;
        return path.equals(fileItem.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
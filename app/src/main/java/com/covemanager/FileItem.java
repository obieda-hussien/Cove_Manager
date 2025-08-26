package com.covemanager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem {
    private File file;
    private String name;
    private boolean isDirectory;
    private long size;
    private long lastModified;
    private boolean isSelected;
    private String sizeDisplay;

    public FileItem(File file) {
        this.file = file;
        this.name = file.getName();
        this.isDirectory = file.isDirectory();
        this.size = file.length();
        this.lastModified = file.lastModified();
        this.isSelected = false;
        this.sizeDisplay = isDirectory ? "Calculating..." : formatFileSize(size);
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
        this.sizeDisplay = formatFileSize(size);
    }

    public String getSizeDisplay() {
        return sizeDisplay;
    }

    public void setSizeDisplay(String sizeDisplay) {
        this.sizeDisplay = sizeDisplay;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getLastModifiedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(lastModified));
    }

    public String getDetails() {
        if (isDirectory) {
            return sizeDisplay;
        } else {
            return getLastModifiedDate() + " | " + sizeDisplay;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        return String.format(Locale.getDefault(), "%.1f %s", 
            bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
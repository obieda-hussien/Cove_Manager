package com.covemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to manage clipboard operations for copy/move functionality
 */
public class FileClipboard {
    private static FileClipboard instance;
    private List<File> files;
    private OperationType operationType;
    
    public enum OperationType {
        COPY, MOVE
    }
    
    private FileClipboard() {
        files = new ArrayList<>();
        operationType = null;
    }
    
    public static synchronized FileClipboard getInstance() {
        if (instance == null) {
            instance = new FileClipboard();
        }
        return instance;
    }
    
    public void copy(List<File> filesToCopy) {
        this.files.clear();
        this.files.addAll(filesToCopy);
        this.operationType = OperationType.COPY;
    }
    
    public void move(List<File> filesToMove) {
        this.files.clear();
        this.files.addAll(filesToMove);
        this.operationType = OperationType.MOVE;
    }
    
    public void clear() {
        this.files.clear();
        this.operationType = null;
    }
    
    public boolean isEmpty() {
        return files.isEmpty() || operationType == null;
    }
    
    public List<File> getFiles() {
        return new ArrayList<>(files);
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public int getCount() {
        return files.size();
    }
}
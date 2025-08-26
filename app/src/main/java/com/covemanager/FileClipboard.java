package com.covemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileClipboard {
    private static FileClipboard instance;
    private List<File> files;
    private OperationType operationType;
    
    public enum OperationType {
        COPY, MOVE
    }
    
    private FileClipboard() {
        this.files = new ArrayList<>();
        this.operationType = OperationType.COPY;
    }
    
    public static FileClipboard getInstance() {
        if (instance == null) {
            instance = new FileClipboard();
        }
        return instance;
    }
    
    public void setFiles(List<FileItem> fileItems, OperationType operation) {
        this.files.clear();
        for (FileItem fileItem : fileItems) {
            this.files.add(fileItem.getFile());
        }
        this.operationType = operation;
    }
    
    public List<File> getFiles() {
        return new ArrayList<>(files);
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public boolean isEmpty() {
        return files.isEmpty();
    }
    
    public void clear() {
        files.clear();
    }
    
    public int getCount() {
        return files.size();
    }
}
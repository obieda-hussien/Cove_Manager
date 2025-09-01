package com.covemanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.Toast;
import com.covemanager.databinding.ActivityFileBrowserBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File browser activity with integrated folder size caching and cache invalidation
 */
public class FileBrowserActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {
    private ActivityFileBrowserBinding binding;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private FolderSizeCache cache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityFileBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbarFileBrowser);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize cache
        cache = FolderSizeCache.getInstance();

        // Set initial directory
        String initialPath = getIntent().getStringExtra("initial_path");
        if (initialPath != null) {
            currentDirectory = new File(initialPath);
        } else {
            currentDirectory = Environment.getExternalStorageDirectory();
        }

        // Set up RecyclerView
        setupRecyclerView();

        // Load files
        loadFiles(currentDirectory);
    }

    private void setupRecyclerView() {
        binding.rvFiles.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadFiles(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Toast.makeText(this, "Cannot access directory", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDirectory = directory;
        binding.tvCurrentPath.setText(directory.getAbsolutePath());

        // Get file list
        File[] files = directory.listFiles();
        List<FileItem> fileItems = new ArrayList<>();

        if (files != null) {
            // Sort files: directories first, then files, alphabetically
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            // Add parent directory option if not at root
            if (directory.getParent() != null) {
                fileItems.add(new FileItem(new File(directory.getParent())) {
                    @Override
                    public String getName() {
                        return "..";
                    }
                });
            }

            // Add all files and directories
            for (File file : files) {
                fileItems.add(new FileItem(file));
            }
        }

        // Update adapter
        if (fileAdapter != null) {
            fileAdapter.cleanup();
        }
        fileAdapter = new FileAdapter(fileItems, this);
        binding.rvFiles.setAdapter(fileAdapter);
    }

    @Override
    public void onFileClick(FileItem fileItem) {
        if (fileItem.isDirectory()) {
            // Navigate to directory
            if (fileItem.getName().equals("..")) {
                loadFiles(new File(currentDirectory.getParent()));
            } else {
                loadFiles(fileItem.getFile());
            }
        } else {
            // Handle file click (could open file viewer, etc.)
            Toast.makeText(this, "File: " + fileItem.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileLongClick(FileItem fileItem) {
        // Show context menu for file operations
        showFileOptionsDialog(fileItem);
    }

    private void showFileOptionsDialog(FileItem fileItem) {
        String[] options = {"Delete", "Rename", "Copy", "Move"};
        
        new AlertDialog.Builder(this)
                .setTitle(fileItem.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Delete
                            deleteFile(fileItem);
                            break;
                        case 1: // Rename
                            renameFile(fileItem);
                            break;
                        case 2: // Copy
                            copyFile(fileItem);
                            break;
                        case 3: // Move
                            moveFile(fileItem);
                            break;
                    }
                })
                .show();
    }

    /**
     * Delete file with cache invalidation
     */
    private void deleteFile(FileItem fileItem) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete " + fileItem.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    File file = fileItem.getFile();
                    if (file.delete()) {
                        // Invalidate cache for parent directory
                        cache.invalidatePath(fileItem.getParent());
                        
                        Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
                        loadFiles(currentDirectory); // Refresh list
                    } else {
                        Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Rename file with cache invalidation
     */
    private void renameFile(FileItem fileItem) {
        // This is a simplified example - in a real app you'd show an input dialog
        File oldFile = fileItem.getFile();
        File newFile = new File(oldFile.getParent(), "renamed_" + oldFile.getName());
        
        if (oldFile.renameTo(newFile)) {
            // Invalidate cache for parent directory
            cache.invalidatePath(fileItem.getParent());
            
            Toast.makeText(this, "File renamed", Toast.LENGTH_SHORT).show();
            loadFiles(currentDirectory); // Refresh list
        } else {
            Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Copy file with cache invalidation
     */
    private void copyFile(FileItem fileItem) {
        // This is a simplified example - in a real app you'd show a directory picker
        File sourceFile = fileItem.getFile();
        File destinationFile = new File(currentDirectory, "copy_" + sourceFile.getName());
        
        // Simulate copy operation (in real app, you'd implement actual file copying)
        try {
            if (sourceFile.isFile()) {
                // For demonstration, just create an empty file
                destinationFile.createNewFile();
                
                // Invalidate cache for destination directory
                cache.invalidatePath(currentDirectory.getAbsolutePath());
                
                Toast.makeText(this, "File copied", Toast.LENGTH_SHORT).show();
                loadFiles(currentDirectory); // Refresh list
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to copy file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Move file with cache invalidation
     */
    private void moveFile(FileItem fileItem) {
        // This is a simplified example - in a real app you'd show a directory picker
        File sourceFile = fileItem.getFile();
        String sourceParent = sourceFile.getParent();
        
        // Simulate moving to a subfolder (in real app, user would choose destination)
        File destinationDirectory = new File(currentDirectory, "moved_files");
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }
        
        File destinationFile = new File(destinationDirectory, sourceFile.getName());
        
        if (sourceFile.renameTo(destinationFile)) {
            // Invalidate cache for both source and destination directories
            cache.invalidatePath(sourceParent);
            cache.invalidatePath(destinationDirectory.getAbsolutePath());
            
            Toast.makeText(this, "File moved", Toast.LENGTH_SHORT).show();
            loadFiles(currentDirectory); // Refresh list
        } else {
            Toast.makeText(this, "Failed to move file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Navigate up one directory level instead of closing activity
        if (currentDirectory.getParent() != null) {
            loadFiles(new File(currentDirectory.getParent()));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileAdapter != null) {
            fileAdapter.cleanup();
        }
        this.binding = null;
    }
}
package com.covemanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;
import com.covemanager.databinding.ActivityFileBrowserBinding;
import com.covemanager.databinding.DialogConfirmDeleteBinding;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileBrowserActivity extends AppCompatActivity implements 
        FileAdapter.OnFileClickListener, FileAdapter.OnFileLongClickListener {
    
    private ActivityFileBrowserBinding binding;
    private FileAdapter fileAdapter;
    private List<FileItem> fileList;
    private String currentPath;
    private ActionMode actionMode;
    private ExecutorService executorService;
    private SharedPreferences preferences;
    private FileClipboard clipboard;

    // Preference keys
    private static final String PREFS_NAME = "file_browser_prefs";
    private static final String PREF_SHOW_HIDDEN = "show_hidden_files";
    private static final String PREF_SORT_BY = "sort_by";
    
    // Sort options
    private static final int SORT_BY_NAME = 0;
    private static final int SORT_BY_SIZE = 1;
    private static final int SORT_BY_DATE = 2;
    private static final int SORT_BY_TYPE = 3;

    public static final String EXTRA_PATH = "extra_path";
    public static final String EXTRA_TITLE = "extra_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityFileBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize preferences and clipboard
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        clipboard = FileClipboard.getInstance();

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Handle incoming intent (for "Open With" functionality)
        handleIncomingIntent();

        // Get path and title from intent
        currentPath = getIntent().getStringExtra(EXTRA_PATH);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        
        if (currentPath == null) {
            currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        
        if (title != null) {
            binding.toolbar.setTitle(title);
        }

        // Initialize executor service for background tasks
        executorService = Executors.newFixedThreadPool(4);

        // Set up RecyclerView
        setupRecyclerView();

        // Set up bottom action bar
        setupBottomActions();

        // Load files
        loadFiles(currentPath);
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri uri = intent.getData();
            String filePath = getFilePathFromUri(uri);
            if (filePath != null) {
                File targetFile = new File(filePath);
                if (targetFile.exists()) {
                    currentPath = targetFile.getParent();
                    // We'll scroll to this file after loading the list
                }
            }
        }
    }

    private String getFilePathFromUri(Uri uri) {
        // Simple implementation for file:// URIs
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        // For content:// URIs, more complex resolution would be needed
        return null;
    }

    private void setupRecyclerView() {
        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, this);
        fileAdapter.setOnFileClickListener(this);
        fileAdapter.setOnFileLongClickListener(this);
        
        binding.rvFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFiles.setAdapter(fileAdapter);
    }

    private void setupBottomActions() {
        binding.bottomActions.btnMove.setOnClickListener(v -> handleMoveAction());
        binding.bottomActions.btnDelete.setOnClickListener(v -> handleDeleteAction());
        binding.bottomActions.btnShare.setOnClickListener(v -> handleShareAction());
        binding.bottomActions.btnCompress.setOnClickListener(v -> handleCompressAction());
        binding.bottomActions.btnMore.setOnClickListener(v -> handleMoreAction());
    }

    private void loadFiles(String path) {
        File directory = new File(path);
        
        if (!directory.exists() || !directory.isDirectory()) {
            Toast.makeText(this, "Directory not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            Toast.makeText(this, "Unable to access directory", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user preferences
        boolean showHidden = preferences.getBoolean(PREF_SHOW_HIDDEN, false);
        int sortBy = preferences.getInt(PREF_SORT_BY, SORT_BY_NAME);

        // Convert to FileItem list with filtering
        List<FileItem> newFileList = new ArrayList<>();
        for (File file : files) {
            // Filter hidden files based on preference
            if (!showHidden && file.getName().startsWith(".")) {
                continue;
            }
            newFileList.add(new FileItem(file));
        }

        // Sort with enhanced logic
        sortFileList(newFileList, sortBy);

        // Update adapter
        fileAdapter.updateFiles(newFileList);

        // Calculate folder sizes asynchronously
        calculateFolderSizes(newFileList);
    }

    private void sortFileList(List<FileItem> files, int sortBy) {
        Collections.sort(files, new Comparator<FileItem>() {
            @Override
            public int compare(FileItem f1, FileItem f2) {
                // Primary sort: folders first
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                }
                
                // Secondary sort: by selected criteria within same type
                switch (sortBy) {
                    case SORT_BY_SIZE:
                        return Long.compare(f2.getSize(), f1.getSize()); // Descending
                    case SORT_BY_DATE:
                        return Long.compare(f2.getLastModified(), f1.getLastModified()); // Newest first
                    case SORT_BY_TYPE:
                        if (!f1.isDirectory() && !f2.isDirectory()) {
                            String ext1 = getFileExtension(f1.getName());
                            String ext2 = getFileExtension(f2.getName());
                            int extCompare = ext1.compareToIgnoreCase(ext2);
                            if (extCompare != 0) return extCompare;
                        }
                        // Fall through to name sorting
                    case SORT_BY_NAME:
                    default:
                        return f1.getName().compareToIgnoreCase(f2.getName());
                }
            }
        });
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private void calculateFolderSizes(List<FileItem> files) {
        for (int i = 0; i < files.size(); i++) {
            FileItem fileItem = files.get(i);
            if (fileItem.isDirectory()) {
                final int position = i;
                executorService.submit(() -> {
                    long size = calculateDirectorySize(fileItem.getFile());
                    runOnUiThread(() -> {
                        fileAdapter.updateFileSize(position, size);
                    });
                });
            }
        }
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } catch (Exception e) {
            // Handle permission errors gracefully
        }
        return size;
    }

    @Override
    public void onFileClick(FileItem fileItem, int position) {
        if (fileItem.isDirectory()) {
            // Navigate to subdirectory
            Intent intent = new Intent(this, FileBrowserActivity.class);
            intent.putExtra(EXTRA_PATH, fileItem.getFile().getAbsolutePath());
            intent.putExtra(EXTRA_TITLE, fileItem.getName());
            startActivity(intent);
        } else {
            // Open file (for now just show toast)
            Toast.makeText(this, "Opening: " + fileItem.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileLongClick(FileItem fileItem, int position) {
        startActionMode(position);
    }

    private void startActionMode(int initialPosition) {
        if (actionMode != null) {
            return;
        }

        fileAdapter.setSelectionMode(true);
        fileAdapter.toggleSelection(initialPosition);
        
        actionMode = startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTitle("1 Selected");
                mode.getMenuInflater().inflate(R.menu.action_mode_menu, menu);
                binding.bottomActions.getRoot().setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.action_select_all) {
                    fileAdapter.selectAll();
                    updateActionModeTitle();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                fileAdapter.setSelectionMode(false);
                binding.bottomActions.getRoot().setVisibility(View.GONE);
            }
        });

        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            int count = fileAdapter.getSelectedCount();
            actionMode.setTitle(count + " Selected");
        }
    }

    private void handleMoveAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        if (selected.isEmpty()) return;
        
        clipboard.setFiles(selected, FileClipboard.OperationType.MOVE);
        Toast.makeText(this, selected.size() + " items ready to move", Toast.LENGTH_SHORT).show();
        
        if (actionMode != null) {
            actionMode.finish();
        }
        
        // Refresh menu to show paste option
        invalidateOptionsMenu();
    }

    private void handleDeleteAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        if (selected.isEmpty()) {
            return;
        }
        
        // Show confirmation dialog
        showDeleteConfirmationDialog(selected);
    }

    private void showDeleteConfirmationDialog(List<FileItem> filesToDelete) {
        DialogConfirmDeleteBinding dialogBinding = DialogConfirmDeleteBinding.inflate(
                LayoutInflater.from(this));

        // Update dialog content based on selection
        int fileCount = filesToDelete.size();
        if (fileCount == 1) {
            dialogBinding.tvDialogTitle.setText("Delete Selected Item");
            dialogBinding.tvDialogMessage.setText("Are you sure you want to delete this item?");
        } else {
            dialogBinding.tvDialogTitle.setText("Delete Selected Items");
            dialogBinding.tvDialogMessage.setText("Are you sure you want to delete these " + fileCount + " items?");
            dialogBinding.tvFileCount.setVisibility(View.VISIBLE);
            dialogBinding.tvFileCount.setText(fileCount + " items selected");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            performDelete(filesToDelete);
        });

        dialog.show();
    }

    private void performDelete(List<FileItem> filesToDelete) {
        executorService.submit(() -> {
            int deletedCount = 0;
            for (FileItem fileItem : filesToDelete) {
                if (deleteFileOrDirectory(fileItem.getFile())) {
                    deletedCount++;
                }
            }
            
            final int finalDeletedCount = deletedCount;
            runOnUiThread(() -> {
                Toast.makeText(this, "Deleted " + finalDeletedCount + " items", Toast.LENGTH_SHORT).show();
                
                if (actionMode != null) {
                    actionMode.finish();
                }
                
                // Refresh the file list
                loadFiles(currentPath);
            });
        });
    }
    
    private boolean deleteFileOrDirectory(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteFileOrDirectory(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    private void handleShareAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        if (selected.isEmpty()) return;
        
        executorService.submit(() -> {
            try {
                List<Uri> uris = new ArrayList<>();
                for (FileItem fileItem : selected) {
                    File file = fileItem.getFile();
                    if (file.exists() && !file.isDirectory()) {
                        Uri uri = FileProvider.getUriForFile(this, 
                            "com.covemanager.provider", file);
                        uris.add(uri);
                    }
                }
                
                runOnUiThread(() -> {
                    if (!uris.isEmpty()) {
                        Intent shareIntent = new Intent();
                        if (uris.size() == 1) {
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                        } else {
                            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, 
                                new ArrayList<>(uris));
                        }
                        shareIntent.setType("*/*");
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        
                        startActivity(Intent.createChooser(shareIntent, "Share files"));
                        
                        if (actionMode != null) {
                            actionMode.finish();
                        }
                    } else {
                        Toast.makeText(this, "No files to share", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error sharing files: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void handleCompressAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        if (selected.isEmpty()) return;
        
        // Show dialog to get ZIP file name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create ZIP Archive");
        
        final EditText input = new EditText(this);
        input.setHint("Archive name (without .zip)");
        input.setText("Archive"); // Default name
        builder.setView(input);
        
        builder.setPositiveButton("Create", (dialog, which) -> {
            String zipName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(zipName)) {
                if (!zipName.endsWith(".zip")) {
                    zipName += ".zip";
                }
                compressFiles(selected, zipName);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void handleMoreAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        if (selected.size() == 1) {
            // Show rename dialog for single item
            showRenameDialog(selected.get(0));
        } else {
            // Show copy option for multiple items
            clipboard.setFiles(selected, FileClipboard.OperationType.COPY);
            Toast.makeText(this, selected.size() + " items ready to copy", Toast.LENGTH_SHORT).show();
            
            if (actionMode != null) {
                actionMode.finish();
            }
            
            // Refresh menu to show paste option
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_browser_menu, menu);
        
        // Update menu state based on preferences
        boolean showHidden = preferences.getBoolean(PREF_SHOW_HIDDEN, false);
        menu.findItem(R.id.action_show_hidden).setChecked(showHidden);
        
        // Update sort menu
        int sortBy = preferences.getInt(PREF_SORT_BY, SORT_BY_NAME);
        switch (sortBy) {
            case SORT_BY_NAME:
                menu.findItem(R.id.sort_by_name).setChecked(true);
                break;
            case SORT_BY_SIZE:
                menu.findItem(R.id.sort_by_size).setChecked(true);
                break;
            case SORT_BY_DATE:
                menu.findItem(R.id.sort_by_date).setChecked(true);
                break;
            case SORT_BY_TYPE:
                menu.findItem(R.id.sort_by_type).setChecked(true);
                break;
        }
        
        // Show/hide paste option based on clipboard
        menu.findItem(R.id.action_paste).setVisible(!clipboard.isEmpty());
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_search) {
            Toast.makeText(this, "Search functionality", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_create_folder) {
            showCreateFolderDialog();
            return true;
        } else if (itemId == R.id.action_show_hidden) {
            toggleShowHiddenFiles(item);
            return true;
        } else if (itemId == R.id.action_paste) {
            handlePasteAction();
            return true;
        } else if (itemId == R.id.sort_by_name) {
            updateSortPreference(SORT_BY_NAME, item);
            return true;
        } else if (itemId == R.id.sort_by_size) {
            updateSortPreference(SORT_BY_SIZE, item);
            return true;
        } else if (itemId == R.id.sort_by_date) {
            updateSortPreference(SORT_BY_DATE, item);
            return true;
        } else if (itemId == R.id.sort_by_type) {
            updateSortPreference(SORT_BY_TYPE, item);
            return true;
        } else if (itemId == R.id.action_more) {
            Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Folder");
        
        final EditText input = new EditText(this);
        input.setHint("Folder name");
        builder.setView(input);
        
        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(folderName)) {
                createNewFolder(folderName);
            } else {
                Toast.makeText(this, "Please enter a valid folder name", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createNewFolder(String folderName) {
        executorService.submit(() -> {
            File newFolder = new File(currentPath, folderName);
            boolean success = newFolder.mkdir();
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Folder created: " + folderName, Toast.LENGTH_SHORT).show();
                    loadFiles(currentPath); // Refresh list
                } else {
                    Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void toggleShowHiddenFiles(MenuItem item) {
        boolean newValue = !item.isChecked();
        item.setChecked(newValue);
        
        preferences.edit()
                .putBoolean(PREF_SHOW_HIDDEN, newValue)
                .apply();
        
        loadFiles(currentPath); // Refresh list
    }

    private void updateSortPreference(int sortBy, MenuItem item) {
        item.setChecked(true);
        
        preferences.edit()
                .putInt(PREF_SORT_BY, sortBy)
                .apply();
        
        loadFiles(currentPath); // Refresh list
    }

    private void compressFiles(List<FileItem> files, String zipName) {
        executorService.submit(() -> {
            try {
                File zipFile = new File(currentPath, zipName);
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                
                for (FileItem fileItem : files) {
                    addToZip(fileItem.getFile(), fileItem.getName(), zos);
                }
                
                zos.close();
                fos.close();
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Archive created: " + zipName, Toast.LENGTH_SHORT).show();
                    loadFiles(currentPath); // Refresh to show new zip file
                    
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                });
                
            } catch (IOException e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error creating archive: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void addToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            // Add directory entry
            zos.putNextEntry(new ZipEntry(entryName + "/"));
            zos.closeEntry();
            
            // Add directory contents
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addToZip(child, entryName + "/" + child.getName(), zos);
                }
            }
        } else {
            // Add file entry
            FileInputStream fis = new FileInputStream(file);
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            
            zos.closeEntry();
            fis.close();
        }
    }

    private void showRenameDialog(FileItem fileItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename");
        
        final EditText input = new EditText(this);
        input.setText(fileItem.getName());
        input.selectAll();
        builder.setView(input);
        
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newName) && !newName.equals(fileItem.getName())) {
                renameFile(fileItem.getFile(), newName);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void renameFile(File file, String newName) {
        executorService.submit(() -> {
            File newFile = new File(file.getParent(), newName);
            boolean success = file.renameTo(newFile);
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
                    loadFiles(currentPath); // Refresh list
                    
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                } else {
                    Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handlePasteAction() {
        if (clipboard.isEmpty()) return;
        
        List<File> filesToPaste = clipboard.getFiles();
        FileClipboard.OperationType operation = clipboard.getOperationType();
        
        executorService.submit(() -> {
            int successCount = 0;
            for (File sourceFile : filesToPaste) {
                File destFile = new File(currentPath, sourceFile.getName());
                
                try {
                    if (operation == FileClipboard.OperationType.COPY) {
                        if (copyFileOrDirectory(sourceFile, destFile)) {
                            successCount++;
                        }
                    } else { // MOVE
                        if (sourceFile.renameTo(destFile)) {
                            successCount++;
                        } else if (copyFileOrDirectory(sourceFile, destFile)) {
                            deleteFileOrDirectory(sourceFile);
                            successCount++;
                        }
                    }
                } catch (IOException e) {
                    // Continue with next file
                }
            }
            
            final int finalSuccessCount = successCount;
            runOnUiThread(() -> {
                String operationName = operation == FileClipboard.OperationType.COPY ? "copied" : "moved";
                Toast.makeText(this, finalSuccessCount + " items " + operationName, Toast.LENGTH_SHORT).show();
                
                if (operation == FileClipboard.OperationType.MOVE) {
                    clipboard.clear();
                    invalidateOptionsMenu(); // Hide paste option
                }
                
                loadFiles(currentPath); // Refresh list
            });
        });
    }

    private boolean copyFileOrDirectory(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }
            
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFileOrDirectory(child, new File(dest, child.getName()));
                }
            }
            return true;
        } else {
            FileInputStream fis = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(dest);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            fis.close();
            fos.close();
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if (actionMode != null) {
            actionMode.finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        this.binding = null;
    }
}
package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.covemanager.databinding.ActivityFileBrowserBinding;
import com.covemanager.utils.FileSorter;
import com.covemanager.utils.FileQueryCache;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import android.database.Cursor;
import android.provider.MediaStore;
import android.content.DialogInterface;

/**
 * Complete FileBrowserActivity with modern file management features:
 * - Selection mode with ActionMode
 * - Full file operations: delete, rename, share, compress, copy, move
 * - FileClipboard integration for cross-directory operations
 * - Modern Material Design UI
 * - Smart Query Mode for Categories (APKs, Images, etc.)
 */
public class FileBrowserActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {
    private ActivityFileBrowserBinding binding;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private FolderSizeCache cache;
    private ActionMode actionMode;
    private FileClipboard clipboard;

    // Sorting Configuration
    private FileSorter.SortField currentSortField = FileSorter.SortField.NAME;
    private FileSorter.SortMode currentSortMode = FileSorter.SortMode.FOLDERS_FIRST;
    private boolean isSortAscending = true;

    private boolean isQueryMode = false;
    private String queryType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Log activity start
            ErrorLogger.logInfo(this, "FileBrowserActivity", "Activity onCreate started");

            // Inflate and get instance of binding
        binding = ActivityFileBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbarFileBrowser);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize cache and clipboard
        cache = FolderSizeCache.getInstance();
        clipboard = FileClipboard.getInstance();

        // Determine mode (Browser vs Query)
        String mode = getIntent().getStringExtra("mode");
        if ("query".equals(mode)) {
            isQueryMode = true;
            queryType = getIntent().getStringExtra("query_type");
            setupRecyclerView();
            setupActionBarButtons();
            loadQueryResults(queryType);
        } else {
            // Browser Mode
            String initialPath = getIntent().getStringExtra("initial_path");
            if (initialPath == null) {
                initialPath = getIntent().getStringExtra("path");
            }
            if (initialPath != null) {
                currentDirectory = new File(initialPath);
            } else {
                currentDirectory = Environment.getExternalStorageDirectory();
            }

            setupRecyclerView();
            setupActionBarButtons();
            updatePasteButtonVisibility();
            loadFiles(currentDirectory);
        }

        // Log successful initialization
        ErrorLogger.logInfo(this, "FileBrowserActivity", "Activity onCreate completed successfully");
    } catch (Exception e) {
        // Log any errors during initialization
        ErrorLogger.logError(this, "FileBrowserActivity", "Error during onCreate", e);
        finish(); // Close activity if critical error occurs
    }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_browser, menu);
        return true;
    }

    private void setupRecyclerView() {
        binding.rvFiles.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupActionBarButtons() {
        binding.btnDelete.setOnClickListener(v -> deleteSelectedFiles());
        binding.btnRename.setOnClickListener(v -> renameSelectedFile());
        binding.btnShare.setOnClickListener(v -> shareSelectedFiles());
        binding.btnCompress.setOnClickListener(v -> compressSelectedFiles());
        binding.btnCopy.setOnClickListener(v -> copySelectedFiles());
        binding.btnMove.setOnClickListener(v -> moveSelectedFiles());
        binding.btnPasteHere.setOnClickListener(v -> pasteFiles());
        
        // Handle new buttons
        binding.btnDetails.setOnClickListener(v -> showFileDetails());
        binding.btnSafeFolder.setOnClickListener(v -> moveToSafeFolder());
        binding.btnOpenWith.setOnClickListener(v -> openWithApp());
        binding.btnMore.setOnClickListener(v -> showMoreOptions());
        binding.btnSort.setOnClickListener(v -> showSortOptions());
    }

    private void loadQueryResults(String type) {
        binding.tvCurrentPath.setText("Category: " + type.toUpperCase());
        new LoadQueryTask().execute(type);
    }

    private class LoadQueryTask extends AsyncTask<String, Void, List<FileItem>> {
        @Override
        protected List<FileItem> doInBackground(String... params) {
            String type = params[0];

            // Check cache first
            List<FileItem> cachedItems = FileQueryCache.getInstance().get(type);
            if (cachedItems != null) {
                return cachedItems;
            }

            List<FileItem> items = new ArrayList<>();

            Uri collection;
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Files.getContentUri("external");
            }

            switch (type) {
                case "apks":
                    selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
                    selectionArgs = new String[]{"application/vnd.android.package-archive"};
                    break;
                case "images":
                    selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
                    selectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)};
                    break;
                case "videos":
                    selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
                    selectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
                    break;
                case "audio":
                    selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
                    selectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO)};
                    break;
                case "documents":
                    // Simplified check for docs
                    selection = MediaStore.Files.FileColumns.MIME_TYPE + " IN (?, ?, ?, ?)";
                    selectionArgs = new String[]{
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain"
                    };
                    break;
            }

            try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(dataColumn);
                        File file = new File(path);
                        if (file.exists()) {
                            items.add(new FileItem(file));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Cache results
            FileQueryCache.getInstance().put(type, items);

            return items;
        }

        @Override
        protected void onPostExecute(List<FileItem> items) {
            // Apply current sorting manually for query results if needed, or reload
             // Since we just loaded them, let's sort them now before displaying
            // Note: FileAdapter takes List<FileItem>, but FileSorter works on File[].
            // We need to sort the FileItems.

            // Simple bubble sort or Collections.sort would work here using a wrapper comparator
            // For now, relying on MediaStore sortOrder (Date Descending) as a baseline,
            // but true dynamic sorting for query results would require re-implementation.
            // TODO: Implement sorting for Query Results

            if (fileAdapter != null) {
                fileAdapter.cleanup();
            }
            fileAdapter = new FileAdapter(items, FileBrowserActivity.this);
            binding.rvFiles.setAdapter(fileAdapter);
        }
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
            // Sort files based on current sort option
            Arrays.sort(files, FileSorter.getComparator(currentSortField, currentSortMode, isSortAscending));

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
            // Handle file click
            openFile(fileItem.getFile());
        }
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri, getMimeType(file.getName()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileLongClick(FileItem fileItem) {
        // This is handled in the adapter now
    }

    @Override
    public void onSelectionModeStarted() {
        if (actionMode == null) {
            actionMode = startActionMode(new ActionModeCallback());
        }
        binding.layoutActionBar.setVisibility(View.VISIBLE);

        // Hide standard toolbar content if needed, or just rely on ActionMode overlay
        // If using WindowActionModeOverlay, the CAB will cover the toolbar.
        // If not, we might need to manually hide elements.
    }

    @Override
    public void onSelectionModeEnded() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        binding.layoutActionBar.setVisibility(View.GONE);
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        if (actionMode != null) {
            actionMode.setTitle(selectedCount + " selected");
        }
        
        // Enable/disable rename button based on selection count
        binding.btnRename.setEnabled(selectedCount == 1);
    }

    private void updatePasteButtonVisibility() {
        if (clipboard.isEmpty()) {
            binding.btnPasteHere.setVisibility(View.GONE);
        } else {
            binding.btnPasteHere.setVisibility(View.VISIBLE);
            if (clipboard.getOperationType() == FileClipboard.OperationType.COPY) {
                binding.btnPasteHere.setText("Copy Here (" + clipboard.getCount() + " files)");
            } else {
                binding.btnPasteHere.setText("Move Here (" + clipboard.getCount() + " files)");
            }
        }
    }

    // File Operations
    private void deleteSelectedFiles() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Files")
                .setMessage("Are you sure you want to delete " + selectedFiles.size() + " item(s)?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new DeleteFilesTask().execute(selectedFiles.toArray(new File[0]));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameSelectedFile() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.size() != 1) return;

        File file = selectedFiles.get(0);
        EditText editText = new EditText(this);
        editText.setText(file.getName());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename File")
                .setView(editText)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        File newFile = new File(file.getParent(), newName);
                        if (file.renameTo(newFile)) {
                            cache.invalidatePath(file.getParent());
                            Toast.makeText(this, "File renamed successfully", Toast.LENGTH_SHORT).show();
                            fileAdapter.endSelectionMode();
                            if (!isQueryMode) loadFiles(currentDirectory);
                            else loadQueryResults(queryType);
                        } else {
                            Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareSelectedFiles() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.isEmpty()) return;

        Intent shareIntent = new Intent();
        ArrayList<Uri> uris = new ArrayList<>();

        for (File file : selectedFiles) {
            if (file.exists() && file.isFile()) {
                Uri uri = FileProvider.getUriForFile(this, 
                    getApplicationContext().getPackageName() + ".provider", file);
                uris.add(uri);
            }
        }

        if (uris.size() == 1) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }

        shareIntent.setType("*/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share files"));
    }

    private void compressSelectedFiles() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.isEmpty()) return;

        EditText editText = new EditText(this);
        editText.setText("archive.zip");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Compress Files")
                .setMessage("Enter ZIP file name:")
                .setView(editText)
                .setPositiveButton("Compress", (dialog, which) -> {
                    String zipName = editText.getText().toString().trim();
                    if (!zipName.isEmpty()) {
                        if (!zipName.endsWith(".zip")) {
                            zipName += ".zip";
                        }
                        new CompressFilesTask().execute(zipName, selectedFiles.toArray(new File[0]));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void copySelectedFiles() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (!selectedFiles.isEmpty()) {
            clipboard.copy(selectedFiles);
            fileAdapter.endSelectionMode();
            updatePasteButtonVisibility();
            Toast.makeText(this, selectedFiles.size() + " files copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveSelectedFiles() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (!selectedFiles.isEmpty()) {
            clipboard.move(selectedFiles);
            fileAdapter.endSelectionMode();
            updatePasteButtonVisibility();
            Toast.makeText(this, selectedFiles.size() + " files cut to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteFiles() {
        if (clipboard.isEmpty()) return;

        List<File> filesToPaste = clipboard.getFiles();
        boolean isMove = clipboard.getOperationType() == FileClipboard.OperationType.MOVE;

        new PasteFilesTask(isMove).execute(filesToPaste.toArray(new File[0]));
    }

    private void showFileDetails() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.isEmpty()) return;
        
        // Show file details dialog
        StringBuilder details = new StringBuilder();
        for (File file : selectedFiles) {
            details.append("Name: ").append(file.getName()).append("\n");
            details.append("Path: ").append(file.getAbsolutePath()).append("\n");
            if (file.isFile()) {
                details.append("Size: ").append(formatFileSize(file.length())).append("\n");
            }
            details.append("Modified: ").append(new java.util.Date(file.lastModified())).append("\n\n");
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("File Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void moveToSafeFolder() {
        // Placeholder for safe folder functionality
        Toast.makeText(this, "Safe folder feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openWithApp() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.isEmpty() || selectedFiles.size() > 1) {
            Toast.makeText(this, "Please select a single file", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = selectedFiles.get(0);
        openFile(file);
    }

    private void showMoreOptions() {
        // Show additional options in a bottom sheet or dialog
        String[] options = {"Properties", "Create Shortcut", "Add to Favorites", "Scan with Antivirus"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("More Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showFileDetails();
                            break;
                        case 1:
                            Toast.makeText(this, "Create shortcut feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(this, "Add to favorites feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            Toast.makeText(this, "Antivirus scan feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showSortOptions() {
        String[] sortFields = {"Name", "Size", "Date", "Type"};
        String[] sortModes = {"Folders First", "Files First", "Mixed"};

        // We need a custom dialog layout or multi-choice logic
        // For simplicity, let's use a single list that combines common options
        // OR use a multi-step dialog.
        // Let's go with a simpler single-choice list for now as requested "Comfortable and Organized"

        String[] options = {
            "Name (A-Z)", "Name (Z-A)",
            "Size (Large-Small)", "Size (Small-Large)",
            "Date (New-Old)", "Date (Old-New)",
            "Folders First", "Files First", "Mixed"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortField = FileSorter.SortField.NAME;
                            isSortAscending = true;
                            break;
                        case 1:
                            currentSortField = FileSorter.SortField.NAME;
                            isSortAscending = false;
                            break;
                        case 2:
                            currentSortField = FileSorter.SortField.SIZE;
                            isSortAscending = false;
                            break;
                        case 3:
                            currentSortField = FileSorter.SortField.SIZE;
                            isSortAscending = true;
                            break;
                        case 4:
                            currentSortField = FileSorter.SortField.DATE;
                            isSortAscending = false;
                            break;
                        case 5:
                            currentSortField = FileSorter.SortField.DATE;
                            isSortAscending = true;
                            break;
                        case 6:
                            currentSortMode = FileSorter.SortMode.FOLDERS_FIRST;
                            break;
                        case 7:
                            currentSortMode = FileSorter.SortMode.FILES_FIRST;
                            break;
                        case 8:
                            currentSortMode = FileSorter.SortMode.MIXED;
                            break;
                    }
                    if (!isQueryMode) loadFiles(currentDirectory);
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_sort) {
            showSortOptions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            case "txt": return "text/plain";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "ppt": case "pptx": return "application/vnd.ms-powerpoint";
            case "zip": return "application/zip";
            case "apk": return "application/vnd.android.package-archive";
            default: return "*/*";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        if (bytes < unit * unit) return String.format("%.1f KB", bytes / (float) unit);
        if (bytes < unit * unit * unit) return String.format("%.1f MB", bytes / (float) (unit * unit));
        return String.format("%.1f GB", bytes / (float) (unit * unit * unit));
    }

    // ActionMode Callback
    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            // You can inflate a menu here if needed
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == android.R.id.selectAll) {
                fileAdapter.selectAll();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            fileAdapter.endSelectionMode();
        }
    }

    // AsyncTasks for file operations
    private class DeleteFilesTask extends AsyncTask<File, Void, Boolean> {
        @Override
        protected Boolean doInBackground(File... files) {
            boolean success = true;
            for (File file : files) {
                try {
                    if (!deleteRecursively(file)) {
                        success = false;
                        ErrorLogger.logError(FileBrowserActivity.this, "DeleteFilesTask", 
                            "Failed to delete file: " + file.getAbsolutePath());
                    } else {
                        ErrorLogger.logInfo(FileBrowserActivity.this, "DeleteFilesTask", 
                            "Successfully deleted: " + file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    success = false;
                    ErrorLogger.trackFileError(FileBrowserActivity.this, "delete", 
                        file.getAbsolutePath(), e);
                }
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            try {
                if (!isQueryMode) {
                    cache.invalidatePath(currentDirectory.getAbsolutePath());
                    loadFiles(currentDirectory);
                } else {
                     loadQueryResults(queryType);
                }
                fileAdapter.endSelectionMode();
                
                if (success) {
                    Toast.makeText(FileBrowserActivity.this, "Files deleted successfully", Toast.LENGTH_SHORT).show();
                    ErrorLogger.logInfo(FileBrowserActivity.this, "DeleteFilesTask", "All files deleted successfully");
                } else {
                    Toast.makeText(FileBrowserActivity.this, "Some files could not be deleted", Toast.LENGTH_SHORT).show();
                    ErrorLogger.logWarning(FileBrowserActivity.this, "DeleteFilesTask", "Some files could not be deleted");
                }
            } catch (Exception e) {
                ErrorLogger.logError(FileBrowserActivity.this, "DeleteFilesTask", "Error in onPostExecute", e);
            }
        }

        private boolean deleteRecursively(File file) {
            try {
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            if (!deleteRecursively(child)) {
                                return false;
                            }
                        }
                    }
                }
                return file.delete();
            } catch (Exception e) {
                ErrorLogger.trackFileError(FileBrowserActivity.this, "deleteRecursively", 
                    file.getAbsolutePath(), e);
                return false;
            }
        }
    }

    private class CompressFilesTask extends AsyncTask<Object, Void, Boolean> {
        private String zipFileName;

        @Override
        protected Boolean doInBackground(Object... params) {
            zipFileName = (String) params[0];
            File[] files = Arrays.copyOfRange(params, 1, params.length, File[].class);
            
            File zipFile = new File(currentDirectory, zipFileName);
            
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                for (File file : files) {
                    addToZip(file, file.getName(), zos);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!isQueryMode) {
                cache.invalidatePath(currentDirectory.getAbsolutePath());
                loadFiles(currentDirectory);
            }
            fileAdapter.endSelectionMode();
            
            if (success) {
                Toast.makeText(FileBrowserActivity.this, "Files compressed to " + zipFileName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(FileBrowserActivity.this, "Failed to compress files", Toast.LENGTH_SHORT).show();
            }
        }

        private void addToZip(File file, String fileName, ZipOutputStream zos) throws IOException {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        addToZip(child, fileName + "/" + child.getName(), zos);
                    }
                }
            } else {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private class PasteFilesTask extends AsyncTask<File, Void, Boolean> {
        private boolean isMove;

        public PasteFilesTask(boolean isMove) {
            this.isMove = isMove;
        }

        @Override
        protected Boolean doInBackground(File... files) {
            boolean success = true;
            for (File file : files) {
                File destination = new File(currentDirectory, file.getName());
                
                if (isMove) {
                    if (!file.renameTo(destination)) {
                        // If rename fails, try copy then delete
                        if (copyFile(file, destination)) {
                            deleteRecursively(file);
                        } else {
                            success = false;
                        }
                    }
                } else {
                    if (!copyFile(file, destination)) {
                        success = false;
                    }
                }
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            clipboard.clear();
            updatePasteButtonVisibility();
            if (!isQueryMode) {
                cache.invalidatePath(currentDirectory.getAbsolutePath());
                loadFiles(currentDirectory);
            }
            
            String operation = isMove ? "moved" : "copied";
            if (success) {
                Toast.makeText(FileBrowserActivity.this, "Files " + operation + " successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(FileBrowserActivity.this, "Some files could not be " + operation, Toast.LENGTH_SHORT).show();
            }
        }

        private boolean copyFile(File source, File destination) {
            if (source.isDirectory()) {
                if (!destination.mkdirs()) return false;
                File[] children = source.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (!copyFile(child, new File(destination, child.getName()))) {
                            return false;
                        }
                    }
                }
                return true;
            } else {
                try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
                     FileChannel destChannel = new FileOutputStream(destination).getChannel()) {
                    destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }

        private boolean deleteRecursively(File file) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (!deleteRecursively(child)) {
                            return false;
                        }
                    }
                }
            }
            return file.delete();
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
        if (!isQueryMode && currentDirectory != null && currentDirectory.getParent() != null) {
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
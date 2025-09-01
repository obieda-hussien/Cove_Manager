package com.covemanager;

import androidx.appcompat.app.AlertDialog;
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

/**
 * Complete FileBrowserActivity with modern file management features:
 * - Selection mode with ActionMode
 * - Full file operations: delete, rename, share, compress, copy, move
 * - FileClipboard integration for cross-directory operations
 * - Modern Material Design UI
 */
public class FileBrowserActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {
    private ActivityFileBrowserBinding binding;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private FolderSizeCache cache;
    private ActionMode actionMode;
    private FileClipboard clipboard;

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

        // Initialize cache and clipboard
        cache = FolderSizeCache.getInstance();
        clipboard = FileClipboard.getInstance();

        // Set initial directory
        String initialPath = getIntent().getStringExtra("initial_path");
        if (initialPath == null) {
            initialPath = getIntent().getStringExtra("path");
        }
        if (initialPath != null) {
            currentDirectory = new File(initialPath);
        } else {
            currentDirectory = Environment.getExternalStorageDirectory();
        }

        // Set up RecyclerView
        setupRecyclerView();

        // Set up action bar buttons
        setupActionBarButtons();

        // Check clipboard state
        updatePasteButtonVisibility();

        // Load files
        loadFiles(currentDirectory);
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
        // This is handled in the adapter now
    }

    @Override
    public void onSelectionModeStarted() {
        if (actionMode == null) {
            actionMode = startActionMode(new ActionModeCallback());
        }
        binding.layoutActionBar.setVisibility(View.VISIBLE);
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
                binding.btnPasteHere.setText("انسخ هنا (" + clipboard.getCount() + " files)");
            } else {
                binding.btnPasteHere.setText("الصق هنا (" + clipboard.getCount() + " files)");
            }
        }
    }

    // File Operations
    private void deleteSelectedFiles() {
        List<File> selectedFiles = fileAdapter.getSelectedItems();
        if (selectedFiles.isEmpty()) return;

        new AlertDialog.Builder(this)
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

        new AlertDialog.Builder(this)
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
                            loadFiles(currentDirectory);
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

        new AlertDialog.Builder(this)
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
                if (!deleteRecursively(file)) {
                    success = false;
                }
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            cache.invalidatePath(currentDirectory.getAbsolutePath());
            fileAdapter.endSelectionMode();
            loadFiles(currentDirectory);
            
            if (success) {
                Toast.makeText(FileBrowserActivity.this, "Files deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(FileBrowserActivity.this, "Some files could not be deleted", Toast.LENGTH_SHORT).show();
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
            cache.invalidatePath(currentDirectory.getAbsolutePath());
            fileAdapter.endSelectionMode();
            loadFiles(currentDirectory);
            
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
            cache.invalidatePath(currentDirectory.getAbsolutePath());
            loadFiles(currentDirectory);
            
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
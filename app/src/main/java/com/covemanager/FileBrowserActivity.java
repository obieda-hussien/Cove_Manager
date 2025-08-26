package com.covemanager;

import androidx.appcompat.view.ActionMode;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.covemanager.databinding.ActivityFileBrowserBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileBrowserActivity extends AppCompatActivity implements 
        FileAdapter.OnFileClickListener, FileAdapter.OnFileLongClickListener {
    
    private ActivityFileBrowserBinding binding;
    private FileAdapter fileAdapter;
    private List<FileItem> fileList;
    private String currentPath;
    private ActionMode actionMode;
    private ExecutorService executorService;

    public static final String EXTRA_PATH = "extra_path";
    public static final String EXTRA_TITLE = "extra_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityFileBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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

        // Convert to FileItem list
        List<FileItem> newFileList = new ArrayList<>();
        for (File file : files) {
            // Skip hidden files
            if (!file.getName().startsWith(".")) {
                newFileList.add(new FileItem(file));
            }
        }

        // Sort: folders first, then files, both alphabetically
        Collections.sort(newFileList, new Comparator<FileItem>() {
            @Override
            public int compare(FileItem f1, FileItem f2) {
                // Folders first
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                } else {
                    // Both are files or both are folders, sort alphabetically
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            }
        });

        // Update adapter
        fileAdapter.updateFiles(newFileList);

        // Calculate folder sizes asynchronously
        calculateFolderSizes(newFileList);
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
        Toast.makeText(this, "Move " + selected.size() + " items", Toast.LENGTH_SHORT).show();
        // TODO: Implement move functionality
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
        // TODO: Implement custom dialog (Phase 9)
        Toast.makeText(this, "Delete " + filesToDelete.size() + " items", Toast.LENGTH_SHORT).show();
    }

    private void handleShareAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        Toast.makeText(this, "Share " + selected.size() + " items", Toast.LENGTH_SHORT).show();
        // TODO: Implement share functionality
    }

    private void handleCompressAction() {
        List<FileItem> selected = fileAdapter.getSelectedFiles();
        Toast.makeText(this, "Compress " + selected.size() + " items", Toast.LENGTH_SHORT).show();
        // TODO: Implement compress functionality
    }

    private void handleMoreAction() {
        Toast.makeText(this, "More actions", Toast.LENGTH_SHORT).show();
        // TODO: Implement more actions
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_browser_menu, menu);
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
        } else if (itemId == R.id.action_more) {
            Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
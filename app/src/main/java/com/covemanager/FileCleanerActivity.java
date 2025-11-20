package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import com.covemanager.databinding.ActivityFileCleanerBinding;
import com.covemanager.ui.dialogs.DuplicateFilesActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;

/**
 * File cleaner activity for cleaning empty folders and junk files.
 */
public class FileCleanerActivity extends AppCompatActivity {
    private ActivityFileCleanerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityFileCleanerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbarFileCleaner);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("File Cleaner");
        }

        setupButtons();
    }

    private void setupButtons() {
        binding.btnCleanEmptyFolders.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Clean Empty Folders")
                .setMessage("This will scan your internal storage and delete all empty folders. Continue?")
                .setPositiveButton("Scan & Clean", (dialog, which) -> {
                    new CleanEmptyFoldersTask().execute();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        binding.btnFindDuplicates.setOnClickListener(v -> {
            Intent intent = new Intent(this, DuplicateFilesActivity.class);
            startActivity(intent);
        });
    }

    private class CleanEmptyFoldersTask extends AsyncTask<Void, String, Integer> {
        @Override
        protected void onPreExecute() {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.setIndeterminate(true);
            binding.tvStatus.setText("Scanning...");
            binding.btnCleanEmptyFolders.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File root = Environment.getExternalStorageDirectory();
            return deleteEmptyFolders(root);
        }

        private int deleteEmptyFolders(File dir) {
            int deletedCount = 0;
            if (dir == null || !dir.isDirectory()) return 0;

            // Skip Android/data and Android/obb to prevent permission issues or app breakage
            if (dir.getAbsolutePath().contains("Android/data") || dir.getAbsolutePath().contains("Android/obb")) {
                return 0;
            }

            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deletedCount += deleteEmptyFolders(file);
                    }
                }
            }

            // Re-check if empty after cleaning children
            files = dir.listFiles();
            if (files != null && files.length == 0) {
                // Don't delete the root storage itself or critical public dirs
                if (!isCriticalDirectory(dir)) {
                    if (dir.delete()) {
                        publishProgress("Deleted: " + dir.getName());
                        deletedCount++;
                    }
                }
            }
            return deletedCount;
        }

        private boolean isCriticalDirectory(File dir) {
            String path = dir.getAbsolutePath();
            File root = Environment.getExternalStorageDirectory();
            if (path.equals(root.getAbsolutePath())) return true;

            // Protect standard directories
            String[] protectedDirs = {
                Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_PICTURES,
                Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DOCUMENTS,
                "Android"
            };

            for (String protectedDir : protectedDirs) {
                 File pDir = new File(root, protectedDir);
                 if (path.equals(pDir.getAbsolutePath())) return true;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            binding.tvStatus.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnCleanEmptyFolders.setEnabled(true);
            binding.tvStatus.setText("Finished");

            new MaterialAlertDialogBuilder(FileCleanerActivity.this)
                .setTitle("Cleaning Complete")
                .setMessage("Deleted " + result + " empty folders.")
                .setPositiveButton("OK", null)
                .show();
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
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}

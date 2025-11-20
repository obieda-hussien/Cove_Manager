package com.covemanager.ui.dialogs;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.covemanager.R;
import com.covemanager.databinding.ActivityDuplicateFilesBinding;
import com.covemanager.databinding.ItemDuplicateGroupBinding;
import com.covemanager.utils.DuplicateDetector;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuplicateFilesActivity extends AppCompatActivity {

    private ActivityDuplicateFilesBinding binding;
    private DuplicateAdapter adapter;
    private List<DuplicateDetector.DuplicateGroup> duplicateGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDuplicateFilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Duplicate Files");
        }

        setupRecyclerView();
        setupButtons();
        startScan();
    }

    private void setupRecyclerView() {
        adapter = new DuplicateAdapter(duplicateGroups);
        binding.rvDuplicates.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDuplicates.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnDeleteSelected.setOnClickListener(v -> {
            int count = adapter.getSelectedCount();
            if (count == 0) return;

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Duplicates")
                    .setMessage("Are you sure you want to delete " + count + " files? Originals will be kept.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteSelected())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void startScan() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.rvDuplicates.setVisibility(View.GONE);
        binding.layoutActions.setVisibility(View.GONE);

        DuplicateDetector.scanForDuplicates(Environment.getExternalStorageDirectory(), new DuplicateDetector.ScanCallback() {
            @Override
            public void onScanComplete(List<DuplicateDetector.DuplicateGroup> duplicates) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvStatus.setVisibility(View.GONE);

                    if (duplicates.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvDuplicates.setVisibility(View.VISIBLE);
                        binding.layoutActions.setVisibility(View.VISIBLE);
                        duplicateGroups.clear();
                        duplicateGroups.addAll(duplicates);
                        adapter.autoSelectDuplicates(); // Auto-select logic
                        adapter.notifyDataSetChanged();
                        updateDeleteButton();
                    }
                });
            }

            @Override
            public void onProgress(String currentPath) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.tvStatus.setText("Scanning: " + new File(currentPath).getName());
                });
            }
        });
    }

    private void deleteSelected() {
        List<File> filesToDelete = adapter.getSelectedFiles();
        int deletedCount = 0;
        for (File f : filesToDelete) {
            if (f.delete()) {
                deletedCount++;
            }
        }

        Toast.makeText(this, "Deleted " + deletedCount + " duplicates.", Toast.LENGTH_SHORT).show();
        startScan(); // Refresh
    }

    private void updateDeleteButton() {
        int count = adapter.getSelectedCount();
        binding.btnDeleteSelected.setText("Delete Selected (" + count + ")");
        binding.btnDeleteSelected.setEnabled(count > 0);
    }

    private class DuplicateAdapter extends RecyclerView.Adapter<DuplicateAdapter.ViewHolder> {
        private List<DuplicateDetector.DuplicateGroup> groups;
        private Set<File> selectedFiles = new HashSet<>();

        public DuplicateAdapter(List<DuplicateDetector.DuplicateGroup> groups) {
            this.groups = groups;
        }

        public void autoSelectDuplicates() {
            selectedFiles.clear();
            for (DuplicateDetector.DuplicateGroup group : groups) {
                // Logic: Keep the oldest file (smallest lastModified), select all others
                if (group.files.size() > 1) {
                    File oldest = group.files.get(0);
                    for (File f : group.files) {
                        if (f.lastModified() < oldest.lastModified()) {
                            oldest = f;
                        }
                    }

                    for (File f : group.files) {
                        if (!f.equals(oldest)) {
                            selectedFiles.add(f);
                        }
                    }
                }
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemDuplicateGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DuplicateDetector.DuplicateGroup group = groups.get(position);
            holder.binding.tvGroupHeader.setText(group.files.size() + " Files • " + formatSize(group.size));

            holder.binding.layoutFilesContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

            for (File file : group.files) {
                View fileView = inflater.inflate(R.layout.item_misplaced_file, holder.binding.layoutFilesContainer, false);
                // Reuse existing layout structure but tweak for duplicates
                TextView tvName = fileView.findViewById(R.id.tv_file_name);
                TextView tvPath = fileView.findViewById(R.id.tv_current_path);
                TextView tvInfo = fileView.findViewById(R.id.tv_dest_path);
                // Add checkbox manually or reuse a layout with checkbox if available.
                // item_misplaced_file doesn't have checkbox.
                // Quick fix: Add a CheckBox programmatically or use a different layout.
                // Let's create a simple checkbox view

                CheckBox checkBox = new CheckBox(holder.itemView.getContext());
                checkBox.setText(file.getParent());
                checkBox.setChecked(selectedFiles.contains(file));

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedFiles.add(file);
                    else selectedFiles.remove(file);
                    updateDeleteButton();
                });

                holder.binding.layoutFilesContainer.addView(checkBox);
            }
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        public int getSelectedCount() {
            return selectedFiles.size();
        }

        public List<File> getSelectedFiles() {
             return new ArrayList<>(selectedFiles);
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int unit = 1024;
            if (bytes < unit * unit) return String.format("%.1f KB", bytes / (float) unit);
            return String.format("%.1f MB", bytes / (float) (unit * unit));
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemDuplicateGroupBinding binding;
            public ViewHolder(ItemDuplicateGroupBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
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
}

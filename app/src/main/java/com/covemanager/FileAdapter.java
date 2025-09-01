package com.covemanager;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.covemanager.databinding.ItemFileBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * Modern FileAdapter with selection mode, ActionMode, and advanced file operations
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private List<FileItem> fileItems;
    private List<File> selectedItems;
    private OnFileClickListener listener;
    private ExecutorService executorService;
    private Handler mainHandler;
    private FolderSizeCache cache;
    private boolean isSelectionMode = false;

    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem);
        void onFileLongClick(FileItem fileItem);
        void onSelectionModeStarted();
        void onSelectionModeEnded();
        void onSelectionChanged(int selectedCount);
    }

    public FileAdapter(List<FileItem> fileItems, OnFileClickListener listener) {
        this.fileItems = fileItems;
        this.selectedItems = new ArrayList<>();
        this.listener = listener;
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cache = FolderSizeCache.getInstance();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFileBinding binding = ItemFileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileItems.get(position);
        holder.bind(fileItem);
    }

    @Override
    public int getItemCount() {
        return fileItems.size();
    }

    // Selection Mode Methods
    public void startSelectionMode() {
        if (!isSelectionMode) {
            isSelectionMode = true;
            selectedItems.clear();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onSelectionModeStarted();
            }
        }
    }

    public void endSelectionMode() {
        if (isSelectionMode) {
            isSelectionMode = false;
            selectedItems.clear();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onSelectionModeEnded();
            }
        }
    }

    public void toggleSelection(int position) {
        if (position >= 0 && position < fileItems.size()) {
            FileItem fileItem = fileItems.get(position);
            File file = fileItem.getFile();
            
            if (selectedItems.contains(file)) {
                selectedItems.remove(file);
            } else {
                selectedItems.add(file);
            }
            
            notifyItemChanged(position);
            
            if (listener != null) {
                listener.onSelectionChanged(selectedItems.size());
            }
            
            // End selection mode if no items selected
            if (selectedItems.isEmpty()) {
                endSelectionMode();
            }
        }
    }

    public void selectAll() {
        selectedItems.clear();
        for (FileItem item : fileItems) {
            if (!item.getName().equals("..")) { // Don't select parent directory
                selectedItems.add(item.getFile());
            }
        }
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onSelectionChanged(selectedItems.size());
        }
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
    }

    public List<File> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    public int getSelectedItemsCount() {
        return selectedItems.size();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    private boolean isSelected(File file) {
        return selectedItems.contains(file);
    }

    /**
     * Calculate folder size using caching
     */
    private long calculateFolderSize(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }

        long totalSize = 0;
        File[] files = folder.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    totalSize += calculateFolderSize(file);
                } else {
                    totalSize += file.length();
                }
            }
        }
        
        return totalSize;
    }

    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private ItemFileBinding binding;

        public FileViewHolder(ItemFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FileItem fileItem) {
            binding.tvFileName.setText(fileItem.getName());
            binding.tvFileDetails.setText("Modified: " + fileItem.getFormattedDate());

            // Set appropriate icon
            if (fileItem.isDirectory()) {
                binding.ivFileIcon.setImageResource(R.drawable.ic_category_documents); // Use folder icon if available
            } else {
                binding.ivFileIcon.setImageResource(R.drawable.ic_category_documents);
            }

            // Handle selection mode UI
            if (isSelectionMode) {
                binding.checkboxSelect.setVisibility(View.VISIBLE);
                boolean selected = isSelected(fileItem.getFile());
                binding.checkboxSelect.setChecked(selected);
                
                // Change background color for selected items
                if (selected) {
                    binding.constraintLayoutRoot.setBackgroundColor(
                        binding.getRoot().getContext().getResources().getColor(android.R.color.holo_blue_light, null));
                } else {
                    binding.constraintLayoutRoot.setBackground(
                        binding.getRoot().getContext().getDrawable(android.R.attr.selectableItemBackground));
                }
            } else {
                binding.checkboxSelect.setVisibility(View.GONE);
                binding.constraintLayoutRoot.setBackground(
                    binding.getRoot().getContext().getDrawable(android.R.attr.selectableItemBackground));
            }

            // Handle folder size with caching
            if (fileItem.isDirectory()) {
                // Check cache first
                Long cachedSize = cache.getSize(fileItem.getPath());
                
                if (cachedSize != null) {
                    // Cache hit - update UI immediately
                    fileItem.setSize(cachedSize);
                    fileItem.setSizeCalculating(false);
                    binding.tvFileSize.setText(fileItem.getFormattedSize());
                    binding.pbCalculating.setVisibility(View.GONE);
                } else {
                    // Cache miss - calculate in background
                    fileItem.setSizeCalculating(true);
                    binding.tvFileSize.setText("Calculating...");
                    binding.pbCalculating.setVisibility(View.VISIBLE);
                    
                    // Start background calculation
                    executorService.execute(() -> {
                        long calculatedSize = calculateFolderSize(fileItem.getFile());
                        
                        // Update cache before updating UI
                        cache.putSize(fileItem.getPath(), calculatedSize);
                        
                        // Update UI on main thread
                        mainHandler.post(() -> {
                            fileItem.setSize(calculatedSize);
                            fileItem.setSizeCalculating(false);
                            
                            // Only update if this ViewHolder is still bound to the same item
                            if (getAdapterPosition() != RecyclerView.NO_POSITION &&
                                getAdapterPosition() < fileItems.size() &&
                                fileItems.get(getAdapterPosition()).equals(fileItem)) {
                                
                                binding.tvFileSize.setText(fileItem.getFormattedSize());
                                binding.pbCalculating.setVisibility(View.GONE);
                            }
                        });
                    });
                }
            } else {
                // Regular file - show size immediately
                binding.tvFileSize.setText(fileItem.getFormattedSize());
                binding.pbCalculating.setVisibility(View.GONE);
            }

            // Set click listeners
            binding.getRoot().setOnClickListener(v -> {
                if (isSelectionMode) {
                    // In selection mode, clicking toggles selection
                    toggleSelection(getAdapterPosition());
                } else {
                    // Normal mode, handle file click
                    if (listener != null) {
                        listener.onFileClick(fileItem);
                    }
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (!isSelectionMode) {
                    // Start selection mode on long press
                    startSelectionMode();
                    toggleSelection(getAdapterPosition());
                }
                return true;
            });

            // Handle checkbox clicks
            binding.checkboxSelect.setOnClickListener(v -> {
                if (isSelectionMode) {
                    toggleSelection(getAdapterPosition());
                }
            });
        }
    }
}
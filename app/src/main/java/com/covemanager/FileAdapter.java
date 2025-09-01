package com.covemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.covemanager.databinding.ItemFileBinding;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * Adapter for displaying files and folders with integrated caching for folder sizes
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private List<FileItem> fileItems;
    private OnFileClickListener listener;
    private ExecutorService executorService;
    private Handler mainHandler;
    private FolderSizeCache cache;

    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem);
        void onFileLongClick(FileItem fileItem);
    }

    public FileAdapter(List<FileItem> fileItems, OnFileClickListener listener) {
        this.fileItems = fileItems;
        this.listener = listener;
        this.executorService = Executors.newFixedThreadPool(2); // Limit concurrent calculations
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
                if (listener != null) {
                    listener.onFileClick(fileItem);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onFileLongClick(fileItem);
                }
                return true;
            });
        }
    }
}
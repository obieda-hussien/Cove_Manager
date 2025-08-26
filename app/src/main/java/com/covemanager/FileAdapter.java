package com.covemanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.covemanager.databinding.ItemFileBinding;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private List<FileItem> files;
    private List<FileItem> selectedFiles;
    private boolean isSelectionMode;
    private OnFileClickListener onFileClickListener;
    private OnFileLongClickListener onFileLongClickListener;
    private Context context;

    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem, int position);
    }

    public interface OnFileLongClickListener {
        void onFileLongClick(FileItem fileItem, int position);
    }

    public FileAdapter(List<FileItem> files, Context context) {
        this.files = files != null ? files : new ArrayList<>();
        this.selectedFiles = new ArrayList<>();
        this.isSelectionMode = false;
        this.context = context;
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.onFileClickListener = listener;
    }

    public void setOnFileLongClickListener(OnFileLongClickListener listener) {
        this.onFileLongClickListener = listener;
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
        FileItem fileItem = files.get(position);
        holder.bind(fileItem, position);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void updateFiles(List<FileItem> newFiles) {
        this.files.clear();
        if (newFiles != null) {
            this.files.addAll(newFiles);
        }
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            clearSelection();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void toggleSelection(int position) {
        if (position >= 0 && position < files.size()) {
            FileItem fileItem = files.get(position);
            if (fileItem.isSelected()) {
                fileItem.setSelected(false);
                selectedFiles.remove(fileItem);
            } else {
                fileItem.setSelected(true);
                selectedFiles.add(fileItem);
            }
            notifyItemChanged(position);
        }
    }

    public void clearSelection() {
        for (FileItem fileItem : selectedFiles) {
            fileItem.setSelected(false);
        }
        selectedFiles.clear();
        notifyDataSetChanged();
    }

    public List<FileItem> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    public int getSelectedCount() {
        return selectedFiles.size();
    }

    public void selectAll() {
        selectedFiles.clear();
        for (FileItem fileItem : files) {
            fileItem.setSelected(true);
            selectedFiles.add(fileItem);
        }
        notifyDataSetChanged();
    }

    public void updateFileSize(int position, long size) {
        if (position >= 0 && position < files.size()) {
            files.get(position).setSize(size);
            notifyItemChanged(position);
        }
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private ItemFileBinding binding;

        public FileViewHolder(ItemFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FileItem fileItem, int position) {
            binding.tvFileName.setText(fileItem.getName());
            binding.tvFileDetails.setText(fileItem.getDetails());
            
            // Set icon based on file type
            int iconRes = getFileIcon(fileItem);
            binding.ivFileIcon.setImageResource(iconRes);
            
            // Set background color for icon
            int colorRes = getFileIconColor(fileItem);
            binding.ivFileIcon.setBackgroundColor(
                ContextCompat.getColor(context, colorRes));

            // Handle selection mode
            binding.cbFileSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            binding.cbFileSelect.setChecked(fileItem.isSelected());
            
            // Set background based on selection
            if (fileItem.isSelected() && isSelectionMode) {
                binding.getRoot().setBackgroundColor(
                    ContextCompat.getColor(context, R.color.colorPrimary));
                binding.getRoot().setAlpha(0.1f);
            } else {
                binding.getRoot().setBackground(
                    ContextCompat.getDrawable(context, android.R.attr.selectableItemBackground));
                binding.getRoot().setAlpha(1.0f);
            }

            // Click listeners
            binding.getRoot().setOnClickListener(v -> {
                if (isSelectionMode) {
                    toggleSelection(position);
                } else if (onFileClickListener != null) {
                    onFileClickListener.onFileClick(fileItem, position);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (onFileLongClickListener != null) {
                    onFileLongClickListener.onFileLongClick(fileItem, position);
                }
                return true;
            });

            binding.cbFileSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isSelectionMode) {
                    if (isChecked != fileItem.isSelected()) {
                        toggleSelection(position);
                    }
                }
            });
        }

        private int getFileIcon(FileItem fileItem) {
            if (fileItem.isDirectory()) {
                return R.drawable.ic_category_downloads; // Using as folder icon
            }
            
            String name = fileItem.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || 
                name.endsWith(".gif") || name.endsWith(".bmp")) {
                return R.drawable.ic_image;
            } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") || 
                       name.endsWith(".mov") || name.endsWith(".wmv")) {
                return R.drawable.ic_video;
            } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") || 
                       name.endsWith(".aac") || name.endsWith(".ogg")) {
                return R.drawable.ic_audio;
            } else if (name.endsWith(".apk")) {
                return R.drawable.ic_apk;
            } else if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || 
                       name.endsWith(".txt") || name.endsWith(".rtf")) {
                return R.drawable.ic_document;
            } else {
                return R.drawable.ic_document; // Default file icon
            }
        }

        private int getFileIconColor(FileItem fileItem) {
            if (fileItem.isDirectory()) {
                return R.color.categoryDownloads;
            }
            
            String name = fileItem.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || 
                name.endsWith(".gif") || name.endsWith(".bmp")) {
                return R.color.categoryImages;
            } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") || 
                       name.endsWith(".mov") || name.endsWith(".wmv")) {
                return R.color.categoryVideos;
            } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") || 
                       name.endsWith(".aac") || name.endsWith(".ogg")) {
                return R.color.categoryAudio;
            } else if (name.endsWith(".apk")) {
                return R.color.categoryApks;
            } else {
                return R.color.categoryDocuments;
            }
        }
    }
}
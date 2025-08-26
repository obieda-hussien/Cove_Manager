package com.covemanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.covemanager.databinding.ItemFilePreviewBinding;
import java.util.ArrayList;
import java.util.List;

public class FilePreviewAdapter extends RecyclerView.Adapter<FilePreviewAdapter.PreviewViewHolder> {
    private List<FileItem> files;
    private Context context;
    private OnPreviewClickListener onPreviewClickListener;

    public interface OnPreviewClickListener {
        void onPreviewClick(FileItem fileItem, int position);
    }

    public FilePreviewAdapter(List<FileItem> files, Context context) {
        this.files = files != null ? files : new ArrayList<>();
        this.context = context;
    }

    public void setOnPreviewClickListener(OnPreviewClickListener listener) {
        this.onPreviewClickListener = listener;
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFilePreviewBinding binding = ItemFilePreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PreviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
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

    class PreviewViewHolder extends RecyclerView.ViewHolder {
        private ItemFilePreviewBinding binding;

        public PreviewViewHolder(ItemFilePreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FileItem fileItem, int position) {
            // Set file size
            binding.tvPreviewSize.setText(fileItem.getSizeDisplay());
            
            // Set icon and background color based on file type
            int iconRes = getFileIcon(fileItem);
            int colorRes = getFileIconColor(fileItem);
            
            binding.ivPreviewIcon.setImageResource(iconRes);
            binding.ivPreviewBackground.setBackgroundColor(
                ContextCompat.getColor(context, colorRes));

            // Click listener
            binding.getRoot().setOnClickListener(v -> {
                if (onPreviewClickListener != null) {
                    onPreviewClickListener.onPreviewClick(fileItem, position);
                }
            });
        }

        private int getFileIcon(FileItem fileItem) {
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
            } else {
                return R.drawable.ic_document;
            }
        }

        private int getFileIconColor(FileItem fileItem) {
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
package com.covemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.covemanager.databinding.ItemCategoryBinding;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CategoryViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private ItemCategoryBinding binding;
        private OnCategoryClickListener listener;

        public CategoryViewHolder(ItemCategoryBinding binding, OnCategoryClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        public void bind(Category category) {
    binding.tvCategoryName.setText(category.getName());
    // Set the icon on the foreground ImageView
    binding.ivCategoryIcon.setImageResource(category.getIcon());
    // Set the background color on the background ShapeableImageView
    binding.ivCategoryBackground.setBackgroundColor(
            ContextCompat.getColor(binding.getRoot().getContext(), category.getColor()));
    
    // Set click listener
    binding.getRoot().setOnClickListener(v -> {
        if (listener != null) {
            listener.onCategoryClick(category);
        }
    });
}

    }
}
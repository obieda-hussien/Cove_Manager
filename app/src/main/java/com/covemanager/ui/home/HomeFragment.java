package com.covemanager.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.covemanager.Category;
import com.covemanager.CategoryAdapter;
import com.covemanager.FileBrowserActivity;
import com.covemanager.R;
import com.covemanager.databinding.FragmentHomeBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private FragmentHomeBinding binding;
    private CategoryAdapter categoryAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCategoriesRecyclerView();
    }

    private void setupCategoriesRecyclerView() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Images", R.drawable.ic_category_images, R.color.categoryImages));
        categories.add(new Category("Videos", R.drawable.ic_category_videos, R.color.categoryVideos));
        categories.add(new Category("Audio", R.drawable.ic_category_audio, R.color.categoryAudio));
        categories.add(new Category("Documents", R.drawable.ic_category_documents, R.color.categoryDocuments));
        categories.add(new Category("Downloads", R.drawable.ic_category_downloads, R.color.categoryDownloads));
        categories.add(new Category("APKs", R.drawable.ic_category_apks, R.color.categoryApks));

        categoryAdapter = new CategoryAdapter(categories, this);
        binding.rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    @Override
    public void onCategoryClick(Category category) {
        Intent intent = new Intent(getContext(), FileBrowserActivity.class);

        String action = "";
        String mimeType = "";

        switch (category.getName()) {
            case "Images":
                // Use query mode for images
                intent.putExtra("mode", "query");
                intent.putExtra("query_type", "images");
                break;
            case "Videos":
                intent.putExtra("mode", "query");
                intent.putExtra("query_type", "videos");
                break;
            case "Audio":
                intent.putExtra("mode", "query");
                intent.putExtra("query_type", "audio");
                break;
            case "Documents":
                intent.putExtra("mode", "query");
                intent.putExtra("query_type", "documents");
                break;
            case "Downloads":
                intent.putExtra("initial_path", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                break;
            case "APKs":
                // New logic for APKs
                intent.putExtra("mode", "query");
                intent.putExtra("query_type", "apks");
                break;
            default:
                intent.putExtra("initial_path", Environment.getExternalStorageDirectory().getAbsolutePath());
        }

        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.covemanager.ui.home;

import android.content.Context;
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

        String initialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        switch (category.getName()) {
            case "Images":
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
                break;
            case "Videos":
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
                break;
            case "Audio":
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
                break;
            case "Documents":
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                break;
            case "Downloads":
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                break;
            case "APKs":
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                break;
        }

        File targetDir = new File(initialPath);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            initialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        intent.putExtra("initial_path", initialPath);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.covemanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.covemanager.databinding.ActivityMainBinding;
import com.covemanager.databinding.DialogStoragePermissionBinding;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class MainActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    private ActivityMainBinding binding;
    private CategoryAdapter categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // set content view to binding's root
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);

        // Set up categories RecyclerView
        setupCategoriesRecyclerView();

        // Check storage permission
        checkStoragePermission();
    }

    private void setupCategoriesRecyclerView() {
        // Create category data
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Images", R.drawable.ic_category_images, R.color.categoryImages));
        categories.add(new Category("Videos", R.drawable.ic_category_videos, R.color.categoryVideos));
        categories.add(new Category("Audio", R.drawable.ic_category_audio, R.color.categoryAudio));
        categories.add(new Category("Documents", R.drawable.ic_category_documents, R.color.categoryDocuments));
        categories.add(new Category("Downloads", R.drawable.ic_category_downloads, R.color.categoryDownloads));
        categories.add(new Category("APKs", R.drawable.ic_category_apks, R.color.categoryApks));

        // Set up RecyclerView
        categoryAdapter = new CategoryAdapter(categories, this);
        binding.rvCategories.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog();
            }
        }
    }

    private void showPermissionDialog() {
        DialogStoragePermissionBinding dialogBinding = DialogStoragePermissionBinding.inflate(
                LayoutInflater.from(this));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        dialogBinding.btnAllowPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                requestStoragePermission();
            }
        });

        dialog.show();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_search) {
            // Handle search action
            return true;
        } else if (itemId == R.id.action_settings) {
            // Handle settings action - start SettingsActivity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCategoryClick(Category category) {
        // Launch FileBrowserActivity with appropriate path based on category
        Intent intent = new Intent(this, FileBrowserActivity.class);
        
        // Determine initial path based on category
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
                // For APKs, default to Downloads folder
                initialPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                break;
        }
        
        // Make sure the directory exists, fallback to root if not
        File targetDir = new File(initialPath);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            initialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        
        intent.putExtra("initial_path", initialPath);
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}

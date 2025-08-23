
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

public class MainActivity extends AppCompatActivity {
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
        categories.add(new Category("Images", R.drawable.ic_image, R.color.categoryImages));
        categories.add(new Category("Videos", R.drawable.ic_video, R.color.categoryVideos));
        categories.add(new Category("Audio", R.drawable.ic_audio, R.color.categoryAudio));
        categories.add(new Category("Documents", R.drawable.ic_document, R.color.categoryDocuments));
        categories.add(new Category("Downloads", R.drawable.ic_download, R.color.categoryDownloads));
        categories.add(new Category("APKs", R.drawable.ic_apk, R.color.categoryApks));

        // Set up RecyclerView
        categoryAdapter = new CategoryAdapter(categories);
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
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}

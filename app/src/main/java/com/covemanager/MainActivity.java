
package com.covemanager;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.covemanager.databinding.ActivityMainBinding;
import com.covemanager.databinding.DialogStoragePermissionBinding;
import com.covemanager.ui.browse.BrowseFragment;
import com.covemanager.ui.home.HomeFragment;
import com.covemanager.ui.tools.ToolsFragment;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Log activity start
            ErrorLogger.logInfo(this, "MainActivity", "Activity onCreate started");

            // Inflate and get instance of binding
            binding = ActivityMainBinding.inflate(getLayoutInflater());

            // set content view to binding's root
            setContentView(binding.getRoot());

            // Set up toolbar
            setSupportActionBar(binding.toolbar);

            // Set up bottom navigation
            setupBottomNavigation();

            // Check storage permission
            checkStoragePermission();

            // Log successful initialization
            ErrorLogger.logInfo(this, "MainActivity", "Activity onCreate completed successfully");
        } catch (Exception e) {
            // Log any errors during initialization
            ErrorLogger.logError(this, "MainActivity", "Error during onCreate", e);
        }
    }

    private HomeFragment homeFragment;
    private BrowseFragment browseFragment;
    private ToolsFragment toolsFragment;
    private Fragment activeFragment;

    private void setupBottomNavigation() {
        // Initialize fragments
        homeFragment = new HomeFragment();
        browseFragment = new BrowseFragment();
        toolsFragment = new ToolsFragment();
        activeFragment = homeFragment;

        // Add all fragments but hide the ones not active
        getSupportFragmentManager().beginTransaction()
                .add(R.id.nav_host_fragment_content_main, homeFragment, "home")
                .add(R.id.nav_host_fragment_content_main, browseFragment, "browse")
                .add(R.id.nav_host_fragment_content_main, toolsFragment, "tools")
                .hide(browseFragment)
                .hide(toolsFragment)
                .commit();

        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.navigation_home) {
                    showFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.navigation_browse) {
                    showFragment(browseFragment);
                    return true;
                } else if (itemId == R.id.navigation_tools) {
                    showFragment(toolsFragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
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

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
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
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.action_search) {
                // Handle search action
                ErrorLogger.logInfo(this, "MainActivity", "Search action selected");
                return true;
            } else if (itemId == R.id.action_settings) {
                // Handle settings action - start SettingsActivity
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                ErrorLogger.logInfo(this, "MainActivity", "Settings activity started");
                return true;
            } else if (itemId == R.id.action_debug_logs) {
                // Handle debug logs - start DebugErrorViewerActivity
                DebugErrorViewerActivity.start(this);
                ErrorLogger.logInfo(this, "MainActivity", "Debug error viewer started");
                return true;
            }
        } catch (Exception e) {
            ErrorLogger.trackNavigationError(this, "MainActivity", "Unknown", e);
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}

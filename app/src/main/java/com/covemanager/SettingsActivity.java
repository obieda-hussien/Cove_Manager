package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.os.Bundle;
import android.view.MenuItem;
import com.covemanager.databinding.ActivitySettingsBinding;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private SettingsAdapter settingsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());

        // set content view to binding's root
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbarSettings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set up settings RecyclerView
        setupSettingsRecyclerView();
    }

    private void setupSettingsRecyclerView() {
        // Create settings data
        List<Setting> settings = new ArrayList<>();
        settings.add(new Setting("Theme", "System default", R.drawable.ic_theme));
        settings.add(new Setting("Language", "English", R.drawable.ic_language));
        settings.add(new Setting("Privacy Policy", "View privacy policy", R.drawable.ic_privacy));

        // Set up RecyclerView
        settingsAdapter = new SettingsAdapter(settings);
        binding.rvSettings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSettings.setAdapter(settingsAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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
package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.covemanager.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private DebugErrorTracker errorTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivitySplashBinding.inflate(getLayoutInflater());

        // Set content view to binding's root
        setContentView(binding.getRoot());

        // Initialize error tracker
        errorTracker = DebugErrorTracker.getInstance(this);

        // Check if this is a crash restart
        boolean isCrashRestart = getIntent().getBooleanExtra("crashed_restart", false) || 
                                errorTracker.wasRestartedFromCrash();

        if (isCrashRestart) {
            // Navigate directly to debug viewer with crash details
            navigateToDebugViewer();
        } else {
            // Normal startup - go to MainActivity after delay
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Prevent user from navigating back to splash screen
                }
            }, SPLASH_DELAY);
        }
    }
    
    /**
     * Navigate to debug viewer after crash restart
     */
    private void navigateToDebugViewer() {
        try {
            // Short delay to show splash, then navigate to debug viewer
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, DebugErrorViewerActivity.class);
                    intent.putExtra("show_crash_details", true);
                    
                    // Get crash details if available
                    String crashDetails = errorTracker.getCrashDetailsFromRestart();
                    if (crashDetails != null) {
                        intent.putExtra("crash_details", crashDetails);
                    }
                    
                    startActivity(intent);
                    finish();
                    
                    // Clear the crash restart flag
                    errorTracker.clearCrashRestartFlag();
                }
            }, 1000); // Shorter delay for crash restart
            
        } catch (Exception e) {
            ErrorLogger.logError(this, "SplashActivity", "Error navigating to debug viewer after crash", e);
            // Fallback to normal navigation
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
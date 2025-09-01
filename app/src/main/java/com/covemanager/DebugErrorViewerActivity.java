package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Debug Error Viewer Activity
 * Shows real-time error logs from the DebugErrorTracker
 */
public class DebugErrorViewerActivity extends AppCompatActivity {
    private TextView textViewLogs;
    private TextView textViewCrashDetails;
    private ScrollView scrollView;
    private Handler handler;
    private Runnable refreshRunnable;
    private File logFile;
    private long lastFileSize = 0;
    private boolean showCrashDetails = false;
    
    public static void start(Context context) {
        Intent intent = new Intent(context, DebugErrorViewerActivity.class);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Check if we should show crash details
            showCrashDetails = getIntent().getBooleanExtra("show_crash_details", false);
            
            setupUI();
            initializeLogFile();
            
            // Show crash details if this is a crash restart
            if (showCrashDetails) {
                displayCrashDetails();
            }
            
            startAutoRefresh();
            
            ErrorLogger.logInfo(this, "DebugErrorViewer", "Debug error viewer started");
        } catch (Exception e) {
            ErrorLogger.logError(this, "DebugErrorViewer", "Error initializing debug viewer", e);
            finish();
        }
    }
    
    private void setupUI() {
        // Create main vertical layout
        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(this);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        // Create crash details section (initially hidden)
        textViewCrashDetails = new TextView(this);
        textViewCrashDetails.setTextSize(14);
        textViewCrashDetails.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textViewCrashDetails.setPadding(16, 16, 16, 16);
        textViewCrashDetails.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        textViewCrashDetails.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        textViewCrashDetails.setVisibility(android.view.View.GONE);
        
        // Create scroll view for logs
        scrollView = new ScrollView(this);
        textViewLogs = new TextView(this);
        
        // Style the logs TextView
        textViewLogs.setTextSize(12);
        textViewLogs.setTypeface(android.graphics.Typeface.MONOSPACE);
        textViewLogs.setPadding(16, 16, 16, 16);
        textViewLogs.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        textViewLogs.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        
        scrollView.addView(textViewLogs);
        
        // Add views to main layout
        mainLayout.addView(textViewCrashDetails);
        mainLayout.addView(scrollView);
        
        setContentView(mainLayout);
        
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Debug Error Logs");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    /**
     * Display crash details prominently at the top
     */
    private void displayCrashDetails() {
        try {
            String crashDetails = getIntent().getStringExtra("crash_details");
            if (crashDetails == null) {
                // Try to get from error tracker
                DebugErrorTracker tracker = DebugErrorTracker.getInstance(this);
                crashDetails = tracker.getCrashDetailsFromRestart();
            }
            
            if (crashDetails != null && !crashDetails.trim().isEmpty()) {
                textViewCrashDetails.setText("🔴 التطبيق تم إعادة تشغيله بعد حدوث خطأ:\n\n" + crashDetails);
                textViewCrashDetails.setVisibility(android.view.View.VISIBLE);
                
                // Update toolbar title to indicate crash
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("🔴 تفاصيل الخطأ - Debug Logs");
                }
            }
        } catch (Exception e) {
            ErrorLogger.logError(this, "DebugErrorViewer", "Error displaying crash details", e);
        }
    }
    
    private void initializeLogFile() {
        DebugErrorTracker tracker = DebugErrorTracker.getInstance(this);
        logFile = tracker.getErrorLogFile();
        
        if (logFile != null && logFile.exists()) {
            lastFileSize = logFile.length();
            loadLogContent();
        } else {
            textViewLogs.setText("Error log file not found or not accessible.\nMake sure storage permissions are granted.");
        }
    }
    
    private void startAutoRefresh() {
        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogContent();
                handler.postDelayed(this, 2000); // Refresh every 2 seconds
            }
        };
        handler.post(refreshRunnable);
    }
    
    private void stopAutoRefresh() {
        if (handler != null && refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
    }
    
    private void loadLogContent() {
        try {
            if (logFile == null || !logFile.exists()) {
                textViewLogs.setText("Log file not available");
                return;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            textViewLogs.setText(content.toString());
            
            // Auto-scroll to bottom
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
            
        } catch (IOException e) {
            textViewLogs.setText("Error reading log file: " + e.getMessage());
            ErrorLogger.logError(this, "DebugErrorViewer", "Error reading log file", e);
        }
    }
    
    private void refreshLogContent() {
        if (logFile == null || !logFile.exists()) {
            return;
        }
        
        try {
            long currentFileSize = logFile.length();
            if (currentFileSize > lastFileSize) {
                // File has grown, reload content
                lastFileSize = currentFileSize;
                loadLogContent();
            }
        } catch (Exception e) {
            ErrorLogger.logError(this, "DebugErrorViewer", "Error refreshing log content", e);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Refresh")
            .setIcon(android.R.drawable.ic_popup_sync)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        menu.add(0, 2, 0, "Clear Logs")
            .setIcon(android.R.drawable.ic_menu_delete)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        menu.add(0, 3, 0, "Share Logs")
            .setIcon(android.R.drawable.ic_menu_share)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case 1: // Refresh
                loadLogContent();
                Toast.makeText(this, "Logs refreshed", Toast.LENGTH_SHORT).show();
                return true;
            case 2: // Clear
                clearLogs();
                return true;
            case 3: // Share
                shareLogs();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void clearLogs() {
        try {
            DebugErrorTracker tracker = DebugErrorTracker.getInstance(this);
            tracker.clearErrorLog();
            loadLogContent();
            Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
            ErrorLogger.logInfo(this, "DebugErrorViewer", "Error logs cleared by user");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to clear logs", Toast.LENGTH_SHORT).show();
            ErrorLogger.logError(this, "DebugErrorViewer", "Error clearing logs", e);
        }
    }
    
    private void shareLogs() {
        try {
            if (logFile == null || !logFile.exists()) {
                Toast.makeText(this, "No log file to share", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Cove Manager Debug Logs");
            shareIntent.putExtra(Intent.EXTRA_TEXT, textViewLogs.getText().toString());
            
            startActivity(Intent.createChooser(shareIntent, "Share Debug Logs"));
            
            ErrorLogger.logInfo(this, "DebugErrorViewer", "Debug logs shared by user");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share logs", Toast.LENGTH_SHORT).show();
            ErrorLogger.logError(this, "DebugErrorViewer", "Error sharing logs", e);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadLogContent();
    }
    
    @Override
    protected void onDestroy() {
        stopAutoRefresh();
        super.onDestroy();
    }
}
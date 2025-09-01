package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Debug Error Viewer Activity
 * Shows real-time error logs from the DebugErrorTracker with modern Material Design UI
 */
public class DebugErrorViewerActivity extends AppCompatActivity {
    private TextView textViewLogs;
    private TextView textViewCrashDetails;
    private TextView textViewLogCount;
    private TextView textViewRefreshIndicator;
    private MaterialCardView crashDetailsCard;
    private ScrollView scrollViewLogs;
    private FloatingActionButton fabActions;
    private Handler handler;
    private Runnable refreshRunnable;
    private File logFile;
    private long lastFileSize = 0;
    private boolean showCrashDetails = false;
    private int logEntryCount = 0;
    
    public static void start(Context context) {
        Intent intent = new Intent(context, DebugErrorViewerActivity.class);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_error_viewer);
        
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
        // Initialize views from layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        textViewLogs = findViewById(R.id.tv_logs);
        textViewCrashDetails = findViewById(R.id.tv_crash_details);
        textViewLogCount = findViewById(R.id.tv_log_count);
        textViewRefreshIndicator = findViewById(R.id.tv_refresh_indicator);
        crashDetailsCard = findViewById(R.id.crash_details_card);
        scrollViewLogs = findViewById(R.id.scroll_view_logs);
        fabActions = findViewById(R.id.fab_actions);
        
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Debug Error Logs");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Set up FAB click listener for quick actions
        fabActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuickActionsMenu();
            }
        });
        
        // Update initial log count
        updateLogCount();
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
                crashDetailsCard.setVisibility(View.VISIBLE);
                
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
                // Animate refresh indicator
                animateRefreshIndicator();
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
                textViewLogs.setText("سجل الأخطاء غير متوفر\nLog file not available");
                updateLogCount();
                return;
            }
            
            StringBuilder content = new StringBuilder();
            logEntryCount = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                    // Count non-empty lines as log entries
                    if (!line.trim().isEmpty()) {
                        logEntryCount++;
                    }
                }
            }
            
            if (content.length() == 0) {
                textViewLogs.setText("لا توجد أخطاء مسجلة\nNo errors logged yet");
            } else {
                textViewLogs.setText(content.toString());
            }
            
            updateLogCount();
            
            // Auto-scroll to bottom
            scrollViewLogs.post(new Runnable() {
                @Override
                public void run() {
                    scrollViewLogs.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
            
        } catch (IOException e) {
            textViewLogs.setText("خطأ في قراءة ملف السجل\nError reading log file: " + e.getMessage());
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
    
    /**
     * Update the log entry count display
     */
    private void updateLogCount() {
        if (textViewLogCount != null) {
            String countText = logEntryCount + " entries";
            if (logEntryCount == 0) {
                countText = "No logs";
            } else if (logEntryCount == 1) {
                countText = "1 entry";
            }
            textViewLogCount.setText(countText);
        }
    }
    
    /**
     * Animate the refresh indicator to show activity
     */
    private void animateRefreshIndicator() {
        if (textViewRefreshIndicator != null) {
            textViewRefreshIndicator.animate()
                .rotation(textViewRefreshIndicator.getRotation() + 360f)
                .setDuration(500)
                .start();
        }
    }
    
    /**
     * Show quick actions menu via FAB
     */
    private void showQuickActionsMenu() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("إجراءات سريعة - Quick Actions");
        
        String[] options = {
            "🔄 تحديث - Refresh",
            "🗑️ مسح السجلات - Clear Logs", 
            "📤 مشاركة - Share Logs",
            "📋 نسخ الكل - Copy All"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Refresh
                    loadLogContent();
                    Toast.makeText(this, "تم التحديث - Refreshed", Toast.LENGTH_SHORT).show();
                    break;
                case 1: // Clear
                    clearLogs();
                    break;
                case 2: // Share
                    shareLogs();
                    break;
                case 3: // Copy All
                    copyAllLogs();
                    break;
            }
        });
        
        builder.setNegativeButton("إلغاء - Cancel", null);
        builder.show();
    }
    
    /**
     * Copy all logs to clipboard
     */
    private void copyAllLogs() {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                "Cove Manager Debug Logs", 
                textViewLogs.getText().toString()
            );
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "تم نسخ السجلات - Logs copied to clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "فشل النسخ - Failed to copy", Toast.LENGTH_SHORT).show();
            ErrorLogger.logError(this, "DebugErrorViewer", "Error copying logs", e);
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
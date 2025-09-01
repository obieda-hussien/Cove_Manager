package com.covemanager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Debug Error Tracker - Real-time error tracking for the Cove Manager application
 * Captures and logs all application errors with timestamps and stack traces
 */
public class DebugErrorTracker {
    private static final String TAG = "DebugErrorTracker";
    private static final String ERROR_LOG_FILE = "cove_manager_errors.log";
    private static final boolean DEBUG_MODE = true; // Set to false for release builds
    
    private static DebugErrorTracker instance;
    private Context applicationContext;
    private File errorLogFile;
    private SimpleDateFormat dateFormat;
    
    private DebugErrorTracker(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        initializeErrorLogFile();
        setupUncaughtExceptionHandler();
    }
    
    public static synchronized DebugErrorTracker getInstance(Context context) {
        if (instance == null) {
            instance = new DebugErrorTracker(context);
        }
        return instance;
    }
    
    /**
     * Initialize the error log file in external storage
     */
    private void initializeErrorLogFile() {
        try {
            File externalDir = applicationContext.getExternalFilesDir(null);
            if (externalDir != null) {
                errorLogFile = new File(externalDir, ERROR_LOG_FILE);
                
                // Create file if it doesn't exist
                if (!errorLogFile.exists()) {
                    errorLogFile.createNewFile();
                    logToFile("=== Cove Manager Error Tracking Started ===");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize error log file", e);
        }
    }
    
    /**
     * Set up global uncaught exception handler
     */
    private void setupUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // Log the uncaught exception
                logError("UNCAUGHT_EXCEPTION", "Thread: " + thread.getName(), throwable);
                
                // Call the default handler to maintain normal crash behavior
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
    
    /**
     * Log an error with context information
     */
    public void logError(String tag, String message, Throwable throwable) {
        String timestamp = dateFormat.format(new Date());
        String errorEntry = String.format(
            "[%s] ERROR [%s]: %s\n%s\n%s\n",
            timestamp,
            tag,
            message,
            getStackTrace(throwable),
            "----------------------------------------"
        );
        
        // Log to Android Log
        Log.e(tag, message, throwable);
        
        // Log to file
        logToFile(errorEntry);
        
        // Show debug notification in debug mode
        if (DEBUG_MODE) {
            showDebugNotification(tag, message);
        }
    }
    
    /**
     * Log an error with just a message (no exception)
     */
    public void logError(String tag, String message) {
        String timestamp = dateFormat.format(new Date());
        String errorEntry = String.format(
            "[%s] ERROR [%s]: %s\n%s\n",
            timestamp,
            tag,
            message,
            "----------------------------------------"
        );
        
        // Log to Android Log
        Log.e(tag, message);
        
        // Log to file
        logToFile(errorEntry);
        
        // Show debug notification in debug mode
        if (DEBUG_MODE) {
            showDebugNotification(tag, message);
        }
    }
    
    /**
     * Log a warning message
     */
    public void logWarning(String tag, String message) {
        String timestamp = dateFormat.format(new Date());
        String warningEntry = String.format(
            "[%s] WARNING [%s]: %s\n",
            timestamp,
            tag,
            message
        );
        
        Log.w(tag, message);
        logToFile(warningEntry);
    }
    
    /**
     * Log an info message
     */
    public void logInfo(String tag, String message) {
        String timestamp = dateFormat.format(new Date());
        String infoEntry = String.format(
            "[%s] INFO [%s]: %s\n",
            timestamp,
            tag,
            message
        );
        
        Log.i(tag, message);
        logToFile(infoEntry);
    }
    
    /**
     * Write to the error log file
     */
    private void logToFile(String message) {
        if (errorLogFile == null) return;
        
        try (FileWriter writer = new FileWriter(errorLogFile, true)) {
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to error log file", e);
        }
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return "No stack trace available";
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Show debug notification (only in debug mode)
     */
    private void showDebugNotification(String tag, String message) {
        if (applicationContext != null) {
            try {
                // Show a toast with error info (non-blocking)
                String toastMessage = "Error in " + tag + ": " + 
                    (message.length() > 50 ? message.substring(0, 50) + "..." : message);
                
                // Post to main thread for UI operations
                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to show debug notification", e);
            }
        }
    }
    
    /**
     * Get the error log file for external access
     */
    public File getErrorLogFile() {
        return errorLogFile;
    }
    
    /**
     * Clear the error log file
     */
    public void clearErrorLog() {
        if (errorLogFile != null && errorLogFile.exists()) {
            try {
                FileWriter writer = new FileWriter(errorLogFile, false);
                writer.write("=== Error Log Cleared ===\n");
                writer.close();
                logInfo(TAG, "Error log cleared");
            } catch (IOException e) {
                Log.e(TAG, "Failed to clear error log", e);
            }
        }
    }
    
    /**
     * Get error log file size in KB
     */
    public long getErrorLogSizeKB() {
        if (errorLogFile != null && errorLogFile.exists()) {
            return errorLogFile.length() / 1024;
        }
        return 0;
    }
    
    /**
     * Check if debug mode is enabled
     */
    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }
    
    /**
     * Manually track an exception that was caught
     */
    public void trackCaughtException(String location, Exception exception) {
        logError("CAUGHT_EXCEPTION", "Location: " + location, exception);
    }
    
    /**
     * Track file operation errors
     */
    public void trackFileOperationError(String operation, String filePath, Exception exception) {
        String message = String.format("File operation '%s' failed for: %s", operation, filePath);
        logError("FILE_OPERATION", message, exception);
    }
    
    /**
     * Track UI errors
     */
    public void trackUIError(String activity, String action, Exception exception) {
        String message = String.format("UI error in %s during %s", activity, action);
        logError("UI_ERROR", message, exception);
    }
    
    /**
     * Track navigation errors
     */
    public void trackNavigationError(String fromActivity, String toActivity, Exception exception) {
        String message = String.format("Navigation failed from %s to %s", fromActivity, toActivity);
        logError("NAVIGATION", message, exception);
    }
}
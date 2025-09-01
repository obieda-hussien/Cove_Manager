package com.covemanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    private static final String CRASH_RESTART_FILE = "crash_restart_data.txt";
    private static final boolean DEBUG_MODE = true; // Set to false for release builds
    private static final int RESTART_DELAY_MS = 2000; // 2 seconds delay before restart
    
    private static DebugErrorTracker instance;
    private Context applicationContext;
    private File errorLogFile;
    private File crashRestartFile;
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
                crashRestartFile = new File(externalDir, CRASH_RESTART_FILE);
                
                // Create error log file if it doesn't exist
                if (!errorLogFile.exists()) {
                    errorLogFile.createNewFile();
                    logToFile("=== Cove Manager Error Tracking Started ===");
                }
                
                // Create crash restart file if it doesn't exist
                if (!crashRestartFile.exists()) {
                    crashRestartFile.createNewFile();
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
                try {
                    // Log the uncaught exception
                    String crashDetails = logCrashAndPrepareRestart(thread, throwable);
                    
                    // Schedule app restart after 2 seconds
                    scheduleAppRestart(crashDetails);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in crash handler", e);
                } finally {
                    // Call the default handler to maintain normal crash behavior
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    }
                }
            }
        });
    }
    
    /**
     * Log crash details and prepare for restart
     */
    private String logCrashAndPrepareRestart(Thread thread, Throwable throwable) {
        String timestamp = dateFormat.format(new Date());
        String crashDetails = String.format(
            "=== CRASH OCCURRED ===\n" +
            "Time: %s\n" +
            "Thread: %s\n" +
            "Exception: %s\n" +
            "Message: %s\n" +
            "Stack Trace:\n%s\n" +
            "=== END CRASH DETAILS ===\n",
            timestamp,
            thread.getName(),
            throwable.getClass().getSimpleName(),
            throwable.getMessage(),
            getStackTrace(throwable)
        );
        
        // Log to main error file
        logError("UNCAUGHT_EXCEPTION", "Thread: " + thread.getName(), throwable);
        
        // Save crash details for restart
        saveCrashRestartData(crashDetails);
        
        return crashDetails;
    }
    
    /**
     * Save crash data for display after restart
     */
    private void saveCrashRestartData(String crashDetails) {
        try {
            if (crashRestartFile != null) {
                FileWriter writer = new FileWriter(crashRestartFile, false);
                writer.write("CRASH_RESTART_FLAG=true\n");
                writer.write("CRASH_TIME=" + System.currentTimeMillis() + "\n");
                writer.write("CRASH_DETAILS_START\n");
                writer.write(crashDetails);
                writer.write("CRASH_DETAILS_END\n");
                writer.close();
                
                Log.i(TAG, "Crash restart data saved");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save crash restart data", e);
        }
    }
    
    /**
     * Schedule app restart using AlarmManager
     */
    private void scheduleAppRestart(String crashDetails) {
        try {
            AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
            
            // Create intent to restart the app
            Intent restartIntent = new Intent(applicationContext, CrashRestartReceiver.class);
            restartIntent.putExtra("crash_details", crashDetails);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Schedule restart after 2 seconds
            long triggerTime = SystemClock.elapsedRealtime() + RESTART_DELAY_MS;
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerTime, pendingIntent);
            
            Log.i(TAG, "App restart scheduled in " + RESTART_DELAY_MS + "ms");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule app restart", e);
        }
    }
    
    /**
     * Check if app was restarted due to crash
     */
    public boolean wasRestartedFromCrash() {
        if (crashRestartFile == null || !crashRestartFile.exists()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(crashRestartFile))) {
            String line = reader.readLine();
            if (line != null && line.equals("CRASH_RESTART_FLAG=true")) {
                // Check if crash was recent (within last 30 seconds)
                String timeLine = reader.readLine();
                if (timeLine != null && timeLine.startsWith("CRASH_TIME=")) {
                    long crashTime = Long.parseLong(timeLine.substring("CRASH_TIME=".length()));
                    long currentTime = System.currentTimeMillis();
                    return (currentTime - crashTime) < 30000; // 30 seconds
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking crash restart flag", e);
        }
        
        return false;
    }
    
    /**
     * Get crash details from restart file
     */
    public String getCrashDetailsFromRestart() {
        if (crashRestartFile == null || !crashRestartFile.exists()) {
            return null;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(crashRestartFile))) {
            StringBuilder details = new StringBuilder();
            String line;
            boolean inDetails = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.equals("CRASH_DETAILS_START")) {
                    inDetails = true;
                    continue;
                } else if (line.equals("CRASH_DETAILS_END")) {
                    break;
                } else if (inDetails) {
                    details.append(line).append("\n");
                }
            }
            
            return details.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading crash details", e);
            return null;
        }
    }
    
    /**
     * Clear crash restart flag
     */
    public void clearCrashRestartFlag() {
        try {
            if (crashRestartFile != null && crashRestartFile.exists()) {
                crashRestartFile.delete();
                Log.i(TAG, "Crash restart flag cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing crash restart flag", e);
        }
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
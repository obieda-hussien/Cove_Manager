package com.covemanager;

import android.app.Application;
import android.util.Log;

/**
 * Custom Application class for Cove Manager
 * Initializes global error tracking and other application-wide components
 */
public class CoveManagerApplication extends Application {
    private static final String TAG = "CoveManagerApp";
    private DebugErrorTracker errorTracker;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "Cove Manager Application starting...");
        
        // Initialize the debug error tracker
        initializeErrorTracking();
        
        // Check if app was restarted due to crash
        checkCrashRestart();
        
        // Log application startup
        if (errorTracker != null) {
            errorTracker.logInfo(TAG, "Cove Manager Application started successfully");
        }
    }
    
    /**
     * Initialize the global error tracking system
     */
    private void initializeErrorTracking() {
        try {
            errorTracker = DebugErrorTracker.getInstance(this);
            Log.i(TAG, "Error tracking initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize error tracking", e);
        }
    }
    
    /**
     * Check if app was restarted due to crash and handle accordingly
     */
    private void checkCrashRestart() {
        try {
            if (errorTracker != null && errorTracker.wasRestartedFromCrash()) {
                Log.i(TAG, "App was restarted due to crash - will launch debug viewer");
                // The actual navigation will be handled in SplashActivity
                // We just log this for tracking purposes
                errorTracker.logInfo(TAG, "App restarted after crash - preparing to show debug info");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking crash restart status", e);
        }
    }
    
    /**
     * Get the error tracker instance
     */
    public DebugErrorTracker getErrorTracker() {
        return errorTracker;
    }
    
    @Override
    public void onTerminate() {
        if (errorTracker != null) {
            errorTracker.logInfo(TAG, "Cove Manager Application terminating");
        }
        super.onTerminate();
    }
    
    @Override
    public void onLowMemory() {
        if (errorTracker != null) {
            errorTracker.logWarning(TAG, "Application received low memory warning");
        }
        super.onLowMemory();
    }
    
    @Override
    public void onTrimMemory(int level) {
        if (errorTracker != null) {
            String levelName = getTrimMemoryLevelName(level);
            errorTracker.logWarning(TAG, "Memory trim requested: " + levelName);
        }
        super.onTrimMemory(level);
    }
    
    /**
     * Convert trim memory level to readable string
     */
    private String getTrimMemoryLevelName(int level) {
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
                return "RUNNING_MODERATE";
            case TRIM_MEMORY_RUNNING_LOW:
                return "RUNNING_LOW";
            case TRIM_MEMORY_RUNNING_CRITICAL:
                return "RUNNING_CRITICAL";
            case TRIM_MEMORY_UI_HIDDEN:
                return "UI_HIDDEN";
            case TRIM_MEMORY_BACKGROUND:
                return "BACKGROUND";
            case TRIM_MEMORY_MODERATE:
                return "MODERATE";
            case TRIM_MEMORY_COMPLETE:
                return "COMPLETE";
            default:
                return "UNKNOWN(" + level + ")";
        }
    }
}
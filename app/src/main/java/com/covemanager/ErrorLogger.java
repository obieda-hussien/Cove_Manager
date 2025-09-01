package com.covemanager;

import android.app.Activity;
import android.content.Context;

/**
 * Utility class to easily access the DebugErrorTracker from any activity or context
 */
public class ErrorLogger {
    private static final String TAG = "ErrorLogger";
    
    /**
     * Get the error tracker instance from application context
     */
    private static DebugErrorTracker getErrorTracker(Context context) {
        try {
            if (context instanceof Activity) {
                CoveManagerApplication app = (CoveManagerApplication) ((Activity) context).getApplication();
                return app.getErrorTracker();
            } else {
                // Fallback to getting instance directly
                return DebugErrorTracker.getInstance(context);
            }
        } catch (Exception e) {
            // Last resort - create new instance
            return DebugErrorTracker.getInstance(context);
        }
    }
    
    /**
     * Log an error with exception
     */
    public static void logError(Context context, String tag, String message, Throwable throwable) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.logError(tag, message, throwable);
        }
    }
    
    /**
     * Log an error without exception
     */
    public static void logError(Context context, String tag, String message) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.logError(tag, message);
        }
    }
    
    /**
     * Log a warning
     */
    public static void logWarning(Context context, String tag, String message) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.logWarning(tag, message);
        }
    }
    
    /**
     * Log info message
     */
    public static void logInfo(Context context, String tag, String message) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.logInfo(tag, message);
        }
    }
    
    /**
     * Track a caught exception
     */
    public static void trackException(Context context, String location, Exception exception) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.trackCaughtException(location, exception);
        }
    }
    
    /**
     * Track file operation error
     */
    public static void trackFileError(Context context, String operation, String filePath, Exception exception) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.trackFileOperationError(operation, filePath, exception);
        }
    }
    
    /**
     * Track UI error
     */
    public static void trackUIError(Context context, String activity, String action, Exception exception) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.trackUIError(activity, action, exception);
        }
    }
    
    /**
     * Track navigation error
     */
    public static void trackNavigationError(Context context, String fromActivity, String toActivity, Exception exception) {
        DebugErrorTracker tracker = getErrorTracker(context);
        if (tracker != null) {
            tracker.trackNavigationError(fromActivity, toActivity, exception);
        }
    }
    
    /**
     * Example usage in try-catch blocks
     */
    public static void example(Context context) {
        try {
            // Some risky operation
            throw new RuntimeException("Example error");
        } catch (Exception e) {
            // Log the error
            ErrorLogger.logError(context, "ExampleActivity", "Failed to perform operation", e);
        }
    }
}
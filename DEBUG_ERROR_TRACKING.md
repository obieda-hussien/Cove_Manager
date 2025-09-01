# Debug Error Tracking System

The Cove Manager application now includes a comprehensive real-time error tracking system that captures and logs all application errors with timestamps and detailed stack traces.

## Features

- **Global Exception Handling**: Automatically catches all uncaught exceptions
- **Real-time Error Logging**: Logs errors to file with timestamps
- **Debug Notifications**: Shows toast notifications for errors in debug mode
- **Error Log Viewer**: Built-in activity to view logs in real-time
- **Easy Integration**: Simple API for manual error logging

## Components

### 1. DebugErrorTracker.java
Core error tracking singleton that handles:
- Uncaught exception handling
- File logging with timestamps
- Debug mode notifications
- Cache management (auto-clears large logs)

### 2. CoveManagerApplication.java
Custom Application class that initializes the error tracking system on app startup.

### 3. ErrorLogger.java
Utility class providing easy access to error tracking from any activity:

```java
// Log an error with exception
ErrorLogger.logError(context, "ActivityName", "Error message", exception);

// Log a warning
ErrorLogger.logWarning(context, "ActivityName", "Warning message");

// Track caught exceptions
ErrorLogger.trackException(context, "MethodName", exception);

// Track file operation errors
ErrorLogger.trackFileError(context, "copy", "/path/to/file", exception);
```

### 4. DebugErrorViewerActivity.java
Real-time error log viewer with features:
- Auto-refresh every 2 seconds
- Scroll to latest entries
- Clear logs
- Share logs
- Monospace font for better readability

## How to Use

### Accessing Error Logs
1. Open the main menu (three dots) in MainActivity
2. Select "Debug Logs"
3. View real-time error logs with auto-refresh

### Manual Error Logging
Add error tracking to your activities:

```java
public class MyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Your code here
            ErrorLogger.logInfo(this, "MyActivity", "Activity started successfully");
        } catch (Exception e) {
            ErrorLogger.logError(this, "MyActivity", "Error during onCreate", e);
        }
    }
    
    private void performFileOperation() {
        try {
            // File operation code
        } catch (IOException e) {
            ErrorLogger.trackFileError(this, "read", filePath, e);
        }
    }
}
```

### Error Log Location
Logs are stored in: `/Android/data/com.covemanager/files/cove_manager_errors.log`

## Debug vs Release Mode

### Debug Mode (DEBUG_MODE = true)
- Shows toast notifications for errors
- All logging enabled
- Real-time error viewer available

### Release Mode (DEBUG_MODE = false)
- Silent error logging (no toasts)
- Critical errors still logged to file
- Error viewer still accessible

## Error Categories

The system tracks different types of errors:

1. **UNCAUGHT_EXCEPTION**: Global uncaught exceptions
2. **CAUGHT_EXCEPTION**: Manually tracked exceptions
3. **FILE_OPERATION**: File system operation errors
4. **UI_ERROR**: UI-related errors
5. **NAVIGATION**: Activity navigation errors

## Log Format

```
[2024-01-15 14:30:25.123] ERROR [ActivityName]: Error description
java.lang.RuntimeException: Detailed stack trace
    at com.covemanager.MyActivity.method(MyActivity.java:123)
    at ...
----------------------------------------
```

## Best Practices

1. **Wrap risky operations** in try-catch blocks with error logging
2. **Use specific error categories** for better debugging
3. **Include context information** in error messages
4. **Monitor log file size** - system auto-manages but check periodically
5. **Use appropriate log levels**:
   - `logError()`: Critical errors that affect functionality
   - `logWarning()`: Non-critical issues that should be addressed
   - `logInfo()`: General application events

## Performance Impact

- Minimal overhead in production
- File I/O operations are lightweight
- Auto-refresh in viewer can be disabled if needed
- Log rotation prevents excessive storage usage

## Troubleshooting

### No logs appearing
1. Check storage permissions
2. Verify external storage is available
3. Check if DEBUG_MODE is enabled

### Log file not accessible
1. Ensure app has storage permissions
2. Check external storage mount status
3. Try clearing app data and restarting

### Performance issues
1. Check log file size (shown in viewer)
2. Clear logs if file becomes too large
3. Disable auto-refresh in viewer if needed

## Examples in Existing Code

The error tracking system is already integrated into:

- **MainActivity**: Activity lifecycle tracking
- **FileBrowserActivity**: File operation error tracking
- **DeleteFilesTask**: Detailed file deletion error tracking

Review these implementations for examples of proper error tracking usage.
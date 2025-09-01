package com.covemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver that handles app restart after crash
 */
public class CrashRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "CrashRestartReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.i(TAG, "Received crash restart broadcast");
            
            // Start the app with the main activity
            Intent restartIntent = new Intent(context, SplashActivity.class);
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            restartIntent.putExtra("crashed_restart", true);
            
            // Add crash details if available
            String crashDetails = intent.getStringExtra("crash_details");
            if (crashDetails != null) {
                restartIntent.putExtra("crash_details", crashDetails);
            }
            
            context.startActivity(restartIntent);
            
            Log.i(TAG, "App restarted successfully after crash");
            
        } catch (Exception e) {
            Log.e(TAG, "Error restarting app after crash", e);
        }
    }
}
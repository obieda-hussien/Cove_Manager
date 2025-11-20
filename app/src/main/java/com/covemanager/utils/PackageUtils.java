package com.covemanager.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.util.Arrays;

public class PackageUtils {

    public static void installPackage(Context context, File file) {
        PackageManager pm = context.getPackageManager();
        PackageInfo newInfo = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_SIGNATURES);

        if (newInfo != null) {
            newInfo.applicationInfo.sourceDir = file.getAbsolutePath();
            newInfo.applicationInfo.publicSourceDir = file.getAbsolutePath();

            try {
                PackageInfo installedInfo = pm.getPackageInfo(newInfo.packageName, PackageManager.GET_SIGNATURES);
                // App is already installed, check signatures
                if (!signaturesMatch(installedInfo, newInfo)) {
                    showSignatureMismatchDialog(context, newInfo.packageName);
                    return;
                }
                showInstallDialog(context, file, "Update App", "Do you want to update this app?");
            } catch (PackageManager.NameNotFoundException e) {
                // App not installed, proceed with install
                showInstallDialog(context, file, "Install App", "Do you want to install this app?");
            }
        } else {
            Toast.makeText(context, "Invalid Package", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean signaturesMatch(PackageInfo p1, PackageInfo p2) {
        if (p1.signatures != null && p2.signatures != null) {
            if (p1.signatures.length != p2.signatures.length) return false;
            for (int i = 0; i < p1.signatures.length; i++) {
                if (!Arrays.equals(p1.signatures[i].toByteArray(), p2.signatures[i].toByteArray())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static void showSignatureMismatchDialog(Context context, String packageName) {
        new MaterialAlertDialogBuilder(context)
            .setTitle("Signature Conflict")
            .setMessage("The installed version of this app has a different signature. You must uninstall the current version before installing this one.")
            .setPositiveButton("Uninstall", (dialog, which) -> {
                Uri packageUri = Uri.parse("package:" + packageName);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
                context.startActivity(uninstallIntent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private static void showInstallDialog(Context context, File file, String title, String message) {
        new MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Install", (dialog, which) -> {
                launchInstallIntent(context, file);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private static void launchInstallIntent(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

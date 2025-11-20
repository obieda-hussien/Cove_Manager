package com.covemanager.ui.browse;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.covemanager.FileBrowserActivity;
import com.covemanager.R;
import com.covemanager.databinding.FragmentBrowseBinding;

import java.io.File;
import java.text.DecimalFormat;

public class BrowseFragment extends Fragment {

    private FragmentBrowseBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBrowseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStorageNavigation();
        updateStorageInfo();
    }

    private void setupStorageNavigation() {
        binding.cardInternalStorage.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FileBrowserActivity.class);
            String internalStoragePath = Environment.getExternalStorageDirectory().getPath();
            intent.putExtra("path", internalStoragePath);
            startActivity(intent);
        });

        binding.cardSdStorage.setOnClickListener(v -> {
            File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(requireContext(), null);
            if (externalFilesDirs.length > 1 && externalFilesDirs[1] != null) {
                String sdCardPath = externalFilesDirs[1].getAbsolutePath();
                File sdRoot = new File(sdCardPath);
                while (sdRoot.getParent() != null && !sdRoot.getName().equals("Android")) {
                    sdRoot = sdRoot.getParentFile();
                }
                if (sdRoot.getParent() != null) {
                    sdRoot = sdRoot.getParentFile();
                }

                Intent intent = new Intent(getContext(), FileBrowserActivity.class);
                intent.putExtra("path", sdRoot.getAbsolutePath());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "SD Card not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStorageInfo() {
        // Update Internal Storage Info
        File internalStorage = Environment.getDataDirectory();
        StatFs stat = new StatFs(internalStorage.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();

        long totalSize = totalBlocks * blockSize;
        long availableSize = availableBlocks * blockSize;
        long usedSize = totalSize - availableSize;

        int progress = (int) ((usedSize * 100) / totalSize);
        binding.pbInternalStorage.setProgress(progress);
        binding.pbInternalStorage.setIndeterminate(false); // Stop "Calculating..." animation effect if any

        String usedSizeStr = formatSize(usedSize);
        String totalSizeStr = formatSize(totalSize);
        binding.tvInternalUsage.setText(usedSizeStr + " used of " + totalSizeStr);


        // Update SD Card Info
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(requireContext(), null);
        if (externalFilesDirs.length > 1 && externalFilesDirs[1] != null) {
            File sdCard = externalFilesDirs[1];
            // Navigate up to find root (simple heuristic)
            File sdRoot = sdCard;
             while (sdRoot.getParent() != null && !sdRoot.getName().equals("Android")) {
                sdRoot = sdRoot.getParentFile();
            }
            if (sdRoot.getParent() != null) {
                sdRoot = sdRoot.getParentFile();
            }

            try {
                StatFs statSd = new StatFs(sdRoot.getPath());
                long blockSizeSd = statSd.getBlockSizeLong();
                long totalBlocksSd = statSd.getBlockCountLong();
                long availableBlocksSd = statSd.getAvailableBlocksLong();

                long totalSizeSd = totalBlocksSd * blockSizeSd;
                long availableSizeSd = availableBlocksSd * blockSizeSd;
                long usedSizeSd = totalSizeSd - availableSizeSd;

                // TODO: Add progress bar for SD card in XML if needed, for now just text status
                binding.tvSdStatus.setText(formatSize(usedSizeSd) + " used of " + formatSize(totalSizeSd));
            } catch (Exception e) {
                 binding.tvSdStatus.setText("Available");
            }
        } else {
            binding.tvSdStatus.setText("Not detected");
        }
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStorageInfo(); // Refresh when returning
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

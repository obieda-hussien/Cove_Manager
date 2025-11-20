package com.covemanager.ui.browse;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package com.covemanager.ui.tools;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.covemanager.FileCleanerActivity;
import com.covemanager.R;
import com.covemanager.databinding.FragmentToolsBinding;
import com.covemanager.ui.dialogs.DuplicateFilesActivity;
import com.covemanager.ui.dialogs.SmartOrganizerBottomSheet;

public class ToolsFragment extends Fragment {

    private FragmentToolsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentToolsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolsNavigation();
    }

    private void setupToolsNavigation() {
        binding.toolFileCleaner.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FileCleanerActivity.class);
            startActivity(intent);
        });

        binding.toolSmartOrganizer.setOnClickListener(v -> {
            SmartOrganizerBottomSheet bottomSheet = new SmartOrganizerBottomSheet();
            bottomSheet.show(getParentFragmentManager(), "SmartOrganizer");
        });

        binding.toolDuplicateFinder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DuplicateFilesActivity.class);
            startActivity(intent);
        });

        // Add more tools here as needed
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

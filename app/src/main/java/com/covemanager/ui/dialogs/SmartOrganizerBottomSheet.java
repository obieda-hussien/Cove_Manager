package com.covemanager.ui.dialogs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.covemanager.R;
import com.covemanager.databinding.BottomSheetSmartOrganizerBinding;
import com.covemanager.databinding.ItemMisplacedFileBinding;
import com.covemanager.utils.SmartOrganizer;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SmartOrganizerBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetSmartOrganizerBinding binding;
    private List<SmartOrganizer.MisplacedFile> misplacedFiles = new ArrayList<>();
    private MisplacedFilesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSmartOrganizerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new MisplacedFilesAdapter();
        binding.rvMisplacedFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMisplacedFiles.setAdapter(adapter);

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnOrganize.setOnClickListener(v -> startOrganizing());

        startScan();
    }

    private void startScan() {
        binding.progressBar.setIndeterminate(true);
        binding.tvStatus.setText("Scanning for misplaced files...");

        SmartOrganizer.scanForMisplacedFiles(new SmartOrganizer.ScanCallback() {
            @Override
            public void onScanComplete(List<SmartOrganizer.MisplacedFile> files) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    misplacedFiles = files;
                    binding.progressBar.setIndeterminate(false);
                    binding.progressBar.setProgress(100); // Or hide

                    if (files.isEmpty()) {
                        binding.tvStatus.setText("Your storage is already organized!");
                        binding.btnOrganize.setEnabled(false);
                    } else {
                        binding.tvStatus.setText("Found " + files.size() + " misplaced files.");
                        binding.btnOrganize.setEnabled(true);
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onProgress(int scannedFiles) {
                // Optional: update UI with scan progress count
            }
        });
    }

    private void startOrganizing() {
        binding.btnOrganize.setEnabled(false);
        binding.btnCancel.setEnabled(false);
        binding.tvStatus.setText("Moving files...");
        binding.progressBar.setIndeterminate(false);
        binding.progressBar.setMax(misplacedFiles.size());
        binding.progressBar.setProgress(0);

        new Thread(() -> {
            int count = 0;
            int successCount = 0;
            for (SmartOrganizer.MisplacedFile item : misplacedFiles) {
                File destFile = new File(item.suggestedPath, item.file.getName());

                // Ensure unique name if file exists
                if (destFile.exists()) {
                    String name = item.file.getName();
                    String base = name.substring(0, name.lastIndexOf('.'));
                    String ext = name.substring(name.lastIndexOf('.'));
                    destFile = new File(item.suggestedPath, base + "_" + System.currentTimeMillis() + ext);
                }

                if (SmartOrganizer.moveFile(item.file, destFile.getAbsolutePath())) {
                    successCount++;
                }

                count++;
                int finalCount = count;
                new Handler(Looper.getMainLooper()).post(() -> binding.progressBar.setProgress(finalCount));
            }

            int finalSuccessCount = successCount;
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getContext(), "Moved " + finalSuccessCount + " files.", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    private class MisplacedFilesAdapter extends RecyclerView.Adapter<MisplacedFilesAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemMisplacedFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SmartOrganizer.MisplacedFile item = misplacedFiles.get(position);
            holder.binding.tvFileName.setText(item.file.getName());
            holder.binding.tvCurrentPath.setText(item.file.getParent());
            holder.binding.tvDestPath.setText("-> " + new File(item.suggestedPath).getName());
        }

        @Override
        public int getItemCount() {
            return misplacedFiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemMisplacedFileBinding binding;
            ViewHolder(ItemMisplacedFileBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
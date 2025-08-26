package com.covemanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import com.covemanager.databinding.ActivityFileCleanerBinding;
import java.util.ArrayList;
import java.util.List;

public class FileCleanerActivity extends AppCompatActivity {
    
    private ActivityFileCleanerBinding binding;
    private FilePreviewAdapter imagesAdapter;
    private FilePreviewAdapter videosAdapter;
    private FilePreviewAdapter audioAdapter;
    private FilePreviewAdapter largeFilesAdapter;
    private FilePreviewAdapter apksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityFileCleanerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set up preview RecyclerViews
        setupPreviewRecyclerViews();

        // Set up "See More" button click listeners
        setupSeeMoreButtons();

        // Load placeholder data for preview
        loadPlaceholderData();
    }

    private void setupPreviewRecyclerViews() {
        // Set up horizontal LinearLayoutManager for all preview RecyclerViews
        binding.rvImagesPreview.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvVideosPreview.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvAudioPreview.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvLargeFilesPreview.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvApksPreview.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Initialize adapters with empty lists
        imagesAdapter = new FilePreviewAdapter(new ArrayList<>(), this);
        videosAdapter = new FilePreviewAdapter(new ArrayList<>(), this);
        audioAdapter = new FilePreviewAdapter(new ArrayList<>(), this);
        largeFilesAdapter = new FilePreviewAdapter(new ArrayList<>(), this);
        apksAdapter = new FilePreviewAdapter(new ArrayList<>(), this);

        // Set adapters
        binding.rvImagesPreview.setAdapter(imagesAdapter);
        binding.rvVideosPreview.setAdapter(videosAdapter);
        binding.rvAudioPreview.setAdapter(audioAdapter);
        binding.rvLargeFilesPreview.setAdapter(largeFilesAdapter);
        binding.rvApksPreview.setAdapter(apksAdapter);
    }

    private void setupSeeMoreButtons() {
        binding.btnImagesSeeMore.setOnClickListener(v -> 
            openCategoryBrowser("Images", "image"));
        
        binding.btnVideosSeeMore.setOnClickListener(v -> 
            openCategoryBrowser("Videos", "video"));
        
        binding.btnAudioSeeMore.setOnClickListener(v -> 
            openCategoryBrowser("Audio", "audio"));
        
        binding.btnLargeFilesSeeMore.setOnClickListener(v -> 
            openCategoryBrowser("Large Files", "large"));
        
        binding.btnApksSeeMore.setOnClickListener(v -> 
            openCategoryBrowser("APK Files", "apk"));
    }

    private void openCategoryBrowser(String categoryName, String categoryType) {
        // For now, we'll use the existing FileBrowserActivity
        // In a real implementation, you might create a specialized CategoryBrowserActivity
        Toast.makeText(this, "Opening " + categoryName + " browser", Toast.LENGTH_SHORT).show();
        
        // TODO: Create a specialized activity or modify FileBrowserActivity to handle category filtering
        // Intent intent = new Intent(this, CategoryBrowserActivity.class);
        // intent.putExtra("category_name", categoryName);
        // intent.putExtra("category_type", categoryType);
        // startActivity(intent);
    }

    private void loadPlaceholderData() {
        // Create placeholder file items for preview
        List<FileItem> placeholderImages = createPlaceholderFiles("image", 5);
        List<FileItem> placeholderVideos = createPlaceholderFiles("video", 3);
        List<FileItem> placeholderAudio = createPlaceholderFiles("audio", 4);
        List<FileItem> placeholderLargeFiles = createPlaceholderFiles("large", 3);
        List<FileItem> placeholderApks = createPlaceholderFiles("apk", 4);

        // Update adapters with placeholder data
        imagesAdapter.updateFiles(placeholderImages);
        videosAdapter.updateFiles(placeholderVideos);
        audioAdapter.updateFiles(placeholderAudio);
        largeFilesAdapter.updateFiles(placeholderLargeFiles);
        apksAdapter.updateFiles(placeholderApks);
    }

    private List<FileItem> createPlaceholderFiles(String type, int count) {
        List<FileItem> files = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            // Create mock File objects for placeholder data
            java.io.File mockFile;
            switch (type) {
                case "image":
                    mockFile = new java.io.File("/storage/emulated/0/Pictures/img_" + i + ".jpg");
                    break;
                case "video":
                    mockFile = new java.io.File("/storage/emulated/0/Movies/video_" + i + ".mp4");
                    break;
                case "audio":
                    mockFile = new java.io.File("/storage/emulated/0/Music/audio_" + i + ".mp3");
                    break;
                case "large":
                    mockFile = new java.io.File("/storage/emulated/0/Download/large_file_" + i + ".zip");
                    break;
                case "apk":
                    mockFile = new java.io.File("/storage/emulated/0/Download/app_" + i + ".apk");
                    break;
                default:
                    mockFile = new java.io.File("/storage/emulated/0/file_" + i + ".txt");
                    break;
            }
            
            // Create FileItem but override the file properties since it's mock data
            FileItem fileItem = new FileItem(mockFile) {
                @Override
                public String getName() {
                    return mockFile.getName();
                }
                
                @Override
                public long getSize() {
                    // Return mock sizes based on type
                    switch (type) {
                        case "image":
                            return (long) (Math.random() * 10 * 1024 * 1024); // 0-10MB
                        case "video":
                            return (long) (Math.random() * 1000 * 1024 * 1024); // 0-1GB
                        case "audio":
                            return (long) (Math.random() * 50 * 1024 * 1024); // 0-50MB
                        case "large":
                            return (long) (Math.random() * 500 * 1024 * 1024); // 0-500MB
                        case "apk":
                            return (long) (Math.random() * 100 * 1024 * 1024); // 0-100MB
                        default:
                            return (long) (Math.random() * 1024 * 1024); // 0-1MB
                    }
                }
                
                @Override
                public String getSizeDisplay() {
                    long size = getSize();
                    if (size <= 0) return "0 B";
                    
                    final String[] units = {"B", "KB", "MB", "GB", "TB"};
                    int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
                    
                    return String.format(java.util.Locale.getDefault(), "%.1f %s", 
                        size / Math.pow(1024, digitGroups), units[digitGroups]);
                }
            };
            
            files.add(fileItem);
        }
        return files;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
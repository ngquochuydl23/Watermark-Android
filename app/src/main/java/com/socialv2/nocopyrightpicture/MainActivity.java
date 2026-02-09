package com.socialv2.nocopyrightpicture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.socialv2.nocopyrightpicture.model.WatermarkSettings;
import com.socialv2.nocopyrightpicture.util.Constants;
import com.socialv2.nocopyrightpicture.viewmodel.WatermarkViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity - Activity chính của ứng dụng Watermark
 * Sử dụng kiến trúc MVVM
 */
public class MainActivity extends AppCompatActivity {

    // ViewModel
    private WatermarkViewModel viewModel;

    // UI Components
    private EditText etWatermarkText;
    private ImageView ivPreview;
    private LinearLayout tvPlaceholder;
    private FrameLayout loadingOverlay;
    private LinearProgressIndicator progressBar;
    private TextView tvProgress;
    private MaterialButton btnCamera;
    private MaterialButton btnGallery;
    private ExtendedFloatingActionButton fabShare;
    private ImageButton btnSettings;

    // Camera
    private Uri currentPhotoUri;

    // Processed images for sharing
    private List<Uri> processedImageUris = new ArrayList<>();

    // Activity Result Launchers
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViewModel();
        initViews();
        initActivityLaunchers();
        setupListeners();
        observeViewModel();
        loadSavedWatermarkText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload settings khi quay lại từ SettingsActivity
        viewModel.loadSettings();
        loadSavedWatermarkText();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(WatermarkViewModel.class);
    }

    private void initViews() {
        etWatermarkText = findViewById(R.id.etWatermarkText);
        ivPreview = findViewById(R.id.ivPreview);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);
        tvProgress = findViewById(R.id.tvProgress);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        fabShare = findViewById(R.id.fabShare);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void initActivityLaunchers() {
        // Camera Launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result && currentPhotoUri != null) {
                    saveWatermarkText();
                    viewModel.processImageFromUri(currentPhotoUri);
                }
            }
        );

        // Gallery Launcher (Photo Picker cho Android 13+)
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(20),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    saveWatermarkText();
                    viewModel.processMultipleImages(uris);
                }
            }
        );

        // Permission Launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            results -> {
                boolean allGranted = true;
                for (Boolean granted : results.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (!allGranted) {
                    Toast.makeText(this, "Cần cấp quyền để sử dụng tính năng này", 
                        Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void setupListeners() {
        btnCamera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                launchCamera();
            } else {
                requestCameraPermission();
            }
        });

        btnGallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                launchGallery();
            } else {
                requestStoragePermission();
            }
        });

        fabShare.setOnClickListener(v -> showShareBottomSheet());
        
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Auto-save watermark text khi thay đổi
        etWatermarkText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Update settings trong ViewModel
                viewModel.getSettings().setText(s.toString());
            }
        });
    }

    private void observeViewModel() {
        // Observe processing state
        viewModel.getProcessingState().observe(this, state -> {
            switch (state) {
                case IDLE:
                    hideProgress();
                    break;
                case LOADING:
                    loadingOverlay.setVisibility(View.VISIBLE);
                    break;
                case PROCESSING:
                    showProgress();
                    break;
                case SUCCESS:
                    hideProgress();
                    break;
                case ERROR:
                    hideProgress();
                    break;
            }
        });

        // Observe processed images
        viewModel.getProcessedImages().observe(this, uris -> {
            if (uris != null && !uris.isEmpty()) {
                processedImageUris = new ArrayList<>(uris);
                fabShare.setVisibility(View.VISIBLE);
                
                // Hiển thị ảnh đầu tiên làm preview
                ivPreview.setImageURI(uris.get(0));
                tvPlaceholder.setVisibility(View.GONE);
            }
        });

        // Observe preview bitmap
        viewModel.getPreviewBitmap().observe(this, bitmap -> {
            if (bitmap != null) {
                ivPreview.setImageBitmap(bitmap);
                tvPlaceholder.setVisibility(View.GONE);
                loadingOverlay.setVisibility(View.GONE);
            }
        });

        // Observe progress
        viewModel.getProgress().observe(this, progress -> {
            if (progress != null) {
                progressBar.setProgress(progress);
                tvProgress.setText(String.format(Locale.getDefault(), 
                    "Đang xử lý: %d%%", progress));
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
        
        // Observe success messages
        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearSuccess();
            }
        });
    }

    // ============ PERMISSIONS ============

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, 
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true; // Scoped storage, no permission needed
        } else {
            return ContextCompat.checkSelfPermission(this, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestCameraPermission() {
        permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+
            permissionLauncher.launch(new String[]{
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13
            permissionLauncher.launch(new String[]{
                Manifest.permission.READ_MEDIA_IMAGES
            });
        } else {
            // Android 12 trở xuống
            permissionLauncher.launch(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }

    // ============ CAMERA ============

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(currentPhotoUri);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở camera: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(new Date());
        String fileName = "IMG_" + timeStamp;
        File storageDir = getCacheDir();
        return new File(storageDir, fileName + ".jpg");
    }

    // ============ GALLERY ============

    private void launchGallery() {
        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build());
    }

    // ============ SHARING ============

    private void showShareBottomSheet() {
        if (processedImageUris.isEmpty()) {
            Toast.makeText(this, "Chưa có ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use Android native share sheet
        Intent shareIntent;
        if (processedImageUris.size() == 1) {
            // Single image
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, processedImageUris.get(0));
        } else {
            // Multiple images
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("image/*");
            ArrayList<Uri> uris = new ArrayList<>(processedImageUris);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh"));
    }

    // ============ UI HELPERS ============

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        loadingOverlay.setVisibility(View.VISIBLE);
        btnCamera.setEnabled(false);
        btnGallery.setEnabled(false);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.GONE);
        btnCamera.setEnabled(true);
        btnGallery.setEnabled(true);
    }

    private void loadSavedWatermarkText() {
        WatermarkSettings settings = viewModel.getSettings();
        if (settings != null && settings.getText() != null) {
            etWatermarkText.setText(settings.getText());
        }
    }

    private void saveWatermarkText() {
        String text = etWatermarkText.getText().toString().trim();
        if (!text.isEmpty()) {
            viewModel.getSettings().setText(text);
            viewModel.saveSettings();
        }
    }
}
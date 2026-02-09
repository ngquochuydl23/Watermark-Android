package com.socialv2.nocopyrightpicture.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.socialv2.nocopyrightpicture.model.WatermarkSettings;
import com.socialv2.nocopyrightpicture.repository.ImageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WatermarkViewModel - ViewModel xử lý logic nghiệp vụ
 */
public class WatermarkViewModel extends AndroidViewModel {
    
    // Repository
    private final ImageRepository imageRepository;
    
    // Thread pool cho background tasks
    private final ExecutorService executorService;
    
    // LiveData cho trạng thái UI
    private final MutableLiveData<ProcessingState> processingState = new MutableLiveData<>();
    private final MutableLiveData<List<Uri>> processedImages = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> previewBitmap = new MutableLiveData<>();
    private final MutableLiveData<Integer> progress = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // Settings
    private WatermarkSettings settings;
    
    /**
     * Enum trạng thái xử lý
     */
    public enum ProcessingState {
        IDLE,
        LOADING,
        PROCESSING,
        SUCCESS,
        ERROR
    }

    public WatermarkViewModel(@NonNull Application application) {
        super(application);
        imageRepository = new ImageRepository(application);
        executorService = Executors.newFixedThreadPool(2);
        processingState.setValue(ProcessingState.IDLE);
        loadSettings();
    }

    // Getters cho LiveData
    public LiveData<ProcessingState> getProcessingState() {
        return processingState;
    }

    public LiveData<List<Uri>> getProcessedImages() {
        return processedImages;
    }

    public LiveData<Bitmap> getPreviewBitmap() {
        return previewBitmap;
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public WatermarkSettings getSettings() {
        return settings;
    }

    /**
     * Load settings từ SharedPreferences
     */
    public void loadSettings() {
        settings = WatermarkSettings.loadFromPrefs(getApplication());
    }
    
    /**
     * Lưu settings vào SharedPreferences
     */
    public void saveSettings() {
        if (settings != null) {
            settings.saveToPrefs(getApplication());
        }
    }

    /**
     * Xử lý một ảnh đơn lẻ (từ Camera)
     */
    public void processImageFromUri(Uri uri) {
        processingState.postValue(ProcessingState.PROCESSING);
        progress.postValue(0);
        
        executorService.execute(() -> {
            try {
                Uri savedUri = imageRepository.processImage(uri, settings);
                
                List<Uri> result = new ArrayList<>();
                result.add(savedUri);
                
                processedImages.postValue(result);
                progress.postValue(100);
                processingState.postValue(ProcessingState.SUCCESS);
                successMessage.postValue("Đã lưu ảnh vào thư mục Pictures/WatermarkPhotos");
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi xử lý ảnh: " + e.getMessage());
                processingState.postValue(ProcessingState.ERROR);
            }
        });
    }

    /**
     * Xử lý nhiều ảnh (từ Gallery)
     */
    public void processMultipleImages(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            errorMessage.postValue("Không có ảnh nào được chọn");
            return;
        }
        
        processingState.postValue(ProcessingState.PROCESSING);
        progress.postValue(0);
        
        executorService.execute(() -> {
            List<Uri> savedUris = new ArrayList<>();
            int total = uris.size();
            int processed = 0;
            int failed = 0;
            
            for (Uri uri : uris) {
                try {
                    Uri savedUri = imageRepository.processImage(uri, settings);
                    savedUris.add(savedUri);
                } catch (Exception e) {
                    // Log error but continue with other images
                    e.printStackTrace();
                    failed++;
                }
                
                processed++;
                int progressPercent = (processed * 100) / total;
                progress.postValue(progressPercent);
            }
            
            if (savedUris.isEmpty()) {
                errorMessage.postValue("Không thể xử lý bất kỳ ảnh nào");
                processingState.postValue(ProcessingState.ERROR);
            } else {
                processedImages.postValue(savedUris);
                processingState.postValue(ProcessingState.SUCCESS);
                
                String message;
                if (failed > 0) {
                    message = String.format("Đã lưu %d/%d ảnh vào Pictures/WatermarkPhotos", 
                        savedUris.size(), total);
                } else {
                    message = String.format("Đã lưu %d ảnh vào Pictures/WatermarkPhotos", 
                        savedUris.size());
                }
                successMessage.postValue(message);
            }
        });
    }

    /**
     * Tạo preview watermark (không lưu)
     */
    public void generatePreview(Uri uri) {
        processingState.postValue(ProcessingState.LOADING);
        
        executorService.execute(() -> {
            try {
                Bitmap original = imageRepository.uriToBitmap(uri);
                Bitmap watermarked = imageRepository.addWatermark(original, settings);
                
                // Scale down for preview to save memory
                int maxSize = 1024;
                int width = watermarked.getWidth();
                int height = watermarked.getHeight();
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                
                if (scale < 1) {
                    Bitmap scaled = Bitmap.createScaledBitmap(
                        watermarked, 
                        (int) (width * scale), 
                        (int) (height * scale), 
                        true);
                    watermarked.recycle();
                    watermarked = scaled;
                }
                
                original.recycle();
                previewBitmap.postValue(watermarked);
                processingState.postValue(ProcessingState.IDLE);
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi tạo preview: " + e.getMessage());
                processingState.postValue(ProcessingState.ERROR);
            }
        });
    }

    /**
     * Reset trạng thái về IDLE
     */
    public void resetState() {
        processingState.postValue(ProcessingState.IDLE);
        progress.postValue(0);
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.postValue(null);
    }
    
    /**
     * Clear success message
     */
    public void clearSuccess() {
        successMessage.postValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}

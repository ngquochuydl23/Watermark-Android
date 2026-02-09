package com.socialv2.nocopyrightpicture.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.socialv2.nocopyrightpicture.model.WatermarkSettings;
import com.socialv2.nocopyrightpicture.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ImageRepository - Repository xử lý các tác vụ liên quan đến hình ảnh
 */
public class ImageRepository {
    private final Context context;

    public ImageRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Chuyển đổi URI thành Bitmap với xử lý rotation
     */
    public Bitmap uriToBitmap(Uri uri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        
        // Decode bitmap với options để tránh OutOfMemory
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream inputStream = contentResolver.openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Không thể mở input stream cho URI");
        }
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        // Tính toán sample size để giảm kích thước nếu ảnh quá lớn
        int maxSize = 4096;
        int sampleSize = 1;
        if (options.outWidth > maxSize || options.outHeight > maxSize) {
            sampleSize = Math.max(options.outWidth / maxSize, options.outHeight / maxSize);
        }
        
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        
        inputStream = contentResolver.openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Không thể mở input stream cho URI");
        }
        
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        if (bitmap == null) {
            throw new IOException("Không thể decode bitmap");
        }
        
        // Handle rotation từ EXIF data
        int rotation = getRotationFromUri(uri);
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            bitmap = rotatedBitmap;
        }
        
        return bitmap;
    }

    /**
     * Lấy thông tin rotation từ EXIF
     */
    private int getRotationFromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return 0;
            
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
            inputStream.close();
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Thêm watermark vào bitmap
     */
    public Bitmap addWatermark(Bitmap originalBitmap, WatermarkSettings settings) {
        // Tạo bản copy có thể chỉnh sửa
        Bitmap resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        
        int width = resultBitmap.getWidth();
        int height = resultBitmap.getHeight();
        
        // Thiết lập Paint cho watermark
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(settings.getColor());
        paint.setAlpha(settings.getAlpha());
        
        // Tính toán text size dựa trên kích thước ảnh
        float scaledTextSize = Math.min(width, height) * 0.06f;
        paint.setTextSize(scaledTextSize);
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(true);
        
        String watermarkText = settings.getText();
        if (watermarkText == null || watermarkText.isEmpty()) {
            watermarkText = Constants.DEFAULT_WATERMARK_TEXT;
        }
        
        // Tính toán kích thước text
        Rect textBounds = new Rect();
        paint.getTextBounds(watermarkText, 0, watermarkText.length(), textBounds);
        
        // Tính toán vị trí để căn giữa
        float centerX = width / 2f;
        float centerY = height / 2f;
        
        // Lưu trạng thái canvas
        canvas.save();
        
        // Xoay canvas -45 độ quanh tâm
        canvas.rotate(-45, centerX, centerY);
        
        // Vẽ nhiều dòng watermark để phủ toàn bộ ảnh
        float textWidth = paint.measureText(watermarkText);
        float textHeight = textBounds.height();
        float spacing = textHeight * 4; // Khoảng cách giữa các dòng
        
        // Tính toán diagonal để đảm bảo phủ hết ảnh khi xoay
        float diagonal = (float) Math.sqrt(width * width + height * height);
        
        // Vẽ watermark pattern
        for (float y = -diagonal; y < diagonal * 1.5f; y += spacing) {
            for (float x = -diagonal; x < diagonal * 1.5f; x += textWidth + 150) {
                canvas.drawText(watermarkText, 
                    centerX + x - textWidth / 2, 
                    centerY + y + textHeight / 2, 
                    paint);
            }
        }
        
        // Khôi phục trạng thái canvas
        canvas.restore();
        
        return resultBitmap;
    }

    /**
     * Lưu bitmap vào Gallery sử dụng MediaStore API
     */
    public Uri saveImageToGallery(Bitmap bitmap) throws IOException {
        String fileName = generateFileName();
        
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        
        Uri imageUri;
        OutputStream outputStream;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ sử dụng Scoped Storage
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, 
                Environment.DIRECTORY_PICTURES + "/" + Constants.WATERMARK_FOLDER);
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1);
            
            ContentResolver resolver = context.getContentResolver();
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            
            if (imageUri == null) {
                throw new IOException("Không thể tạo MediaStore record.");
            }
            
            outputStream = resolver.openOutputStream(imageUri);
            if (outputStream == null) {
                throw new IOException("Không thể mở output stream.");
            }
            
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            outputStream.close();
            
            contentValues.clear();
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(imageUri, contentValues, null, null);
            
        } else {
            // Android 9 trở xuống
            File directory = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Constants.WATERMARK_FOLDER);
            
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Không thể tạo thư mục");
            }
            
            File imageFile = new File(directory, fileName);
            outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            outputStream.close();
            
            // Thêm vào MediaStore
            contentValues.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            imageUri = context.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }
        
        return imageUri;
    }

    /**
     * Tạo tên file unique
     */
    private String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault());
        return "WM_" + sdf.format(new Date()) + ".jpg";
    }

    /**
     * Xử lý hoàn chỉnh: load, watermark, save
     */
    public Uri processImage(Uri sourceUri, WatermarkSettings settings) throws IOException {
        Bitmap original = uriToBitmap(sourceUri);
        Bitmap watermarked = addWatermark(original, settings);
        Uri savedUri = saveImageToGallery(watermarked);
        
        // Giải phóng bộ nhớ
        if (original != watermarked) {
            original.recycle();
        }
        watermarked.recycle();
        
        return savedUri;
    }
}

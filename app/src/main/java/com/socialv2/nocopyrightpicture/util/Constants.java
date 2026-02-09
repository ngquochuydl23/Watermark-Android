package com.socialv2.nocopyrightpicture.util;

/**
 * Constants - Chứa các hằng số sử dụng trong ứng dụng
 */
public class Constants {
    // SharedPreferences
    public static final String PREFS_NAME = "watermark_prefs";
    public static final String KEY_WATERMARK_TEXT = "watermark_text";
    public static final String KEY_OPACITY = "opacity";
    public static final String KEY_COLOR = "color";
    public static final String KEY_TEXT_SIZE = "text_size";
    
    // Default values
    public static final String DEFAULT_WATERMARK_TEXT = "© Ngọc Hằng - 0909710267";
    public static final int DEFAULT_OPACITY = 40; // 40%
    public static final int DEFAULT_COLOR = 0xFFFFFFFF; // White
    public static final float DEFAULT_TEXT_SIZE = 48f;
    
    // Colors
    public static final int COLOR_WHITE = 0xFFFFFFFF;
    public static final int COLOR_BLACK = 0xFF000000;
    public static final int COLOR_RED = 0xFFFF0000;
    
    // Folder name
    public static final String WATERMARK_FOLDER = "WatermarkPhotos";
    
    // Package names for sharing
    public static final String PACKAGE_FACEBOOK = "com.facebook.katana";
    public static final String PACKAGE_ZALO = "com.zing.zalo";
    public static final String PACKAGE_TELEGRAM = "org.telegram.messenger";
    
    // App names for error messages
    public static final String APP_NAME_FACEBOOK = "Facebook";
    public static final String APP_NAME_ZALO = "Zalo";
    public static final String APP_NAME_TELEGRAM = "Telegram";
}

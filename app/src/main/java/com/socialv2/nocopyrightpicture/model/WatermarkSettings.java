package com.socialv2.nocopyrightpicture.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.socialv2.nocopyrightpicture.util.Constants;

/**
 * WatermarkSettings - Model chứa các cài đặt watermark
 */
public class WatermarkSettings {
    private String text;
    private int opacity; // 0-100
    private int color;
    private float textSize;

    public WatermarkSettings() {
        this.text = Constants.DEFAULT_WATERMARK_TEXT;
        this.opacity = Constants.DEFAULT_OPACITY;
        this.color = Constants.DEFAULT_COLOR;
        this.textSize = Constants.DEFAULT_TEXT_SIZE;
    }

    public WatermarkSettings(String text, int opacity, int color, float textSize) {
        this.text = text;
        this.opacity = opacity;
        this.color = color;
        this.textSize = textSize;
    }

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getOpacity() { return opacity; }
    public void setOpacity(int opacity) { this.opacity = opacity; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public float getTextSize() { return textSize; }
    public void setTextSize(float textSize) { this.textSize = textSize; }

    /**
     * Tính giá trị alpha (0-255) từ opacity (0-100)
     */
    public int getAlpha() {
        return (int) (opacity * 2.55f);
    }

    /**
     * Load settings từ SharedPreferences
     */
    public static WatermarkSettings loadFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
            Constants.PREFS_NAME, Context.MODE_PRIVATE);
        
        WatermarkSettings settings = new WatermarkSettings();
        settings.setText(prefs.getString(Constants.KEY_WATERMARK_TEXT, 
            Constants.DEFAULT_WATERMARK_TEXT));
        settings.setOpacity(prefs.getInt(Constants.KEY_OPACITY, 
            Constants.DEFAULT_OPACITY));
        settings.setColor(prefs.getInt(Constants.KEY_COLOR, 
            Constants.DEFAULT_COLOR));
        settings.setTextSize(prefs.getFloat(Constants.KEY_TEXT_SIZE, 
            Constants.DEFAULT_TEXT_SIZE));
        
        return settings;
    }

    /**
     * Lưu settings vào SharedPreferences
     */
    public void saveToPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
            Constants.PREFS_NAME, Context.MODE_PRIVATE);
        
        prefs.edit()
            .putString(Constants.KEY_WATERMARK_TEXT, text)
            .putInt(Constants.KEY_OPACITY, opacity)
            .putInt(Constants.KEY_COLOR, color)
            .putFloat(Constants.KEY_TEXT_SIZE, textSize)
            .apply();
    }
}

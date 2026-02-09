package com.socialv2.nocopyrightpicture;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import com.socialv2.nocopyrightpicture.model.WatermarkSettings;
import com.socialv2.nocopyrightpicture.util.Constants;

import java.util.Locale;

/**
 * SettingsActivity - Activity cài đặt watermark
 */
public class SettingsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText etWatermarkText;
    private Slider sliderOpacity;
    private TextView tvOpacityValue;
    private RadioGroup rgColor;
    private RadioButton rbWhite, rbBlack, rbRed;
    private MaterialCardView cardWhite, cardBlack, cardRed;
    private TextView tvPreview;
    private MaterialButton btnSave;

    private WatermarkSettings settings;
    private int selectedColor = Constants.COLOR_WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        loadSettings();
        setupListeners();
        updatePreview();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etWatermarkText = findViewById(R.id.etWatermarkText);
        sliderOpacity = findViewById(R.id.sliderOpacity);
        tvOpacityValue = findViewById(R.id.tvOpacityValue);
        rgColor = findViewById(R.id.rgColor);
        tvPreview = findViewById(R.id.tvPreviewWatermark);
        btnSave = findViewById(R.id.btnSave);
        
        // Color cards
        cardWhite = findViewById(R.id.cardWhite);
        cardBlack = findViewById(R.id.cardBlack);
        cardRed = findViewById(R.id.cardRed);
        rbWhite = findViewById(R.id.rbWhite);
        rbBlack = findViewById(R.id.rbBlack);
        rbRed = findViewById(R.id.rbRed);

        // Setup toolbar
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadSettings() {
        settings = WatermarkSettings.loadFromPrefs(this);

        // Set text
        etWatermarkText.setText(settings.getText());

        // Set opacity
        sliderOpacity.setValue(settings.getOpacity());
        tvOpacityValue.setText(String.format(Locale.getDefault(), "%d%%", settings.getOpacity()));

        // Set color
        int color = settings.getColor();
        selectedColor = color;
        updateColorSelection(color);
    }
    
    private void updateColorSelection(int color) {
        // Reset all cards
        cardWhite.setStrokeColor(getResources().getColor(android.R.color.transparent, getTheme()));
        cardBlack.setStrokeColor(getResources().getColor(android.R.color.transparent, getTheme()));
        cardRed.setStrokeColor(getResources().getColor(android.R.color.transparent, getTheme()));
        
        // Highlight selected card
        int accentColor = getResources().getColor(R.color.vibrant_blue_green, getTheme());
        if (color == Constants.COLOR_WHITE) {
            cardWhite.setStrokeColor(accentColor);
            rbWhite.setChecked(true);
        } else if (color == Constants.COLOR_BLACK) {
            cardBlack.setStrokeColor(accentColor);
            rbBlack.setChecked(true);
        } else if (color == Constants.COLOR_RED) {
            cardRed.setStrokeColor(accentColor);
            rbRed.setChecked(true);
        }
    }

    private void setupListeners() {
        // Text change listener
        etWatermarkText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                settings.setText(s.toString());
                updatePreview();
            }
        });

        // Opacity slider listener
        sliderOpacity.addOnChangeListener((slider, value, fromUser) -> {
            int opacity = (int) value;
            settings.setOpacity(opacity);
            tvOpacityValue.setText(String.format(Locale.getDefault(), "%d%%", opacity));
            updatePreview();
        });

        // Color card click listeners
        cardWhite.setOnClickListener(v -> {
            selectedColor = Constants.COLOR_WHITE;
            settings.setColor(selectedColor);
            updateColorSelection(selectedColor);
            updatePreview();
        });
        
        cardBlack.setOnClickListener(v -> {
            selectedColor = Constants.COLOR_BLACK;
            settings.setColor(selectedColor);
            updateColorSelection(selectedColor);
            updatePreview();
        });
        
        cardRed.setOnClickListener(v -> {
            selectedColor = Constants.COLOR_RED;
            settings.setColor(selectedColor);
            updateColorSelection(selectedColor);
            updatePreview();
        });

        // Save button listener
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void updatePreview() {
        String text = settings.getText();
        if (text == null || text.isEmpty()) {
            text = Constants.DEFAULT_WATERMARK_TEXT;
        }
        
        tvPreview.setText(text);
        
        // Set color với opacity
        int color = settings.getColor();
        int alpha = settings.getAlpha();
        int colorWithAlpha = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        tvPreview.setTextColor(colorWithAlpha);
        
        // Thêm shadow cho dễ nhìn
        if (color == Constants.COLOR_WHITE) {
            tvPreview.setShadowLayer(3f, 2f, 2f, Color.BLACK);
        } else {
            tvPreview.setShadowLayer(3f, 2f, 2f, Color.WHITE);
        }
    }

    private void saveSettings() {
        String text = etWatermarkText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung watermark", Toast.LENGTH_SHORT).show();
            return;
        }

        settings.setText(text);
        settings.saveToPrefs(this);
        
        Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
        finish();
    }
}

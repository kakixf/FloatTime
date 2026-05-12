package com.timepop.floatingtime;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvPreviewTime;
    private TextView tvPreviewMs;
    private View previewContainer;
    private Button btnSelectFormat;
    private Button btnReset;
    private ImageView btnBack;
    private Switch switchMsColor;
    private TextView switchMsLabel;
    private View colorPreviewText;
    private View colorPreviewMs;
    private View colorPreviewBg;
    private TextView tvAlphaBg;
    private LinearLayout itemTextColor;
    private LinearLayout itemMsColor;
    private LinearLayout itemBgColor;

    private String currentTimeFormat = "HH:mm:ss.SSS";
    private int textColor = Color.GREEN;
    private int msColor = Color.RED;
    private int bgColor = Color.argb(153, 0, 0, 0);
    private boolean msColorEnabled = true;

    private int colorPickerTarget = 0;
    private static final int TARGET_TEXT = 1;
    private static final int TARGET_MS = 2;
    private static final int TARGET_BG = 3;

    private Handler handler;
    private Runnable updatePreviewRunnable;

    private String[] formatNames = {
        "完整格式 (HH:mm:ss.SSS)",
        "无毫秒 (HH:mm:ss)",
        "精简毫秒 (mm:ss.SSS)"
    };

    private String[] formatValues = {
        "HH:mm:ss.SSS",
        "HH:mm:ss",
        "mm:ss.SSS"
    };

    private static final int DEFAULT_TEXT_COLOR = Color.GREEN;
    private static final int DEFAULT_MS_COLOR = Color.RED;
    private static final int DEFAULT_BG_ALPHA = 153;
    private static final int DEFAULT_BG_COLOR = Color.argb(DEFAULT_BG_ALPHA, 0, 0, 0);
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss.SSS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        handler = new Handler();
        updatePreviewRunnable = new Runnable() {
            @Override
            public void run() {
                updatePreviewTime();
                handler.postDelayed(this, 10);
            }
        };

        initViews();
        loadSettings();
        setupListeners();
        updatePreviews();

        handler.post(updatePreviewRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updatePreviewRunnable);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        previewContainer = findViewById(R.id.previewContainer);
        tvPreviewTime = findViewById(R.id.tvPreviewTime);
        tvPreviewMs = findViewById(R.id.tvPreviewMs);
        btnSelectFormat = findViewById(R.id.btnSelectFormat);
        btnReset = findViewById(R.id.btnReset);
        switchMsColor = findViewById(R.id.switchMsColor);
        switchMsLabel = findViewById(R.id.switchMsLabel);
        colorPreviewText = findViewById(R.id.colorPreviewText);
        colorPreviewMs = findViewById(R.id.colorPreviewMs);
        colorPreviewBg = findViewById(R.id.colorPreviewBg);
        tvAlphaBg = findViewById(R.id.tvAlphaBg);
        itemTextColor = findViewById(R.id.itemTextColor);
        itemMsColor = findViewById(R.id.itemMsColor);
        itemBgColor = findViewById(R.id.itemBgColor);

        try {
            Typeface monoFont = Typeface.createFromAsset(getAssets(), "fonts/RobotoMono-Bold.ttf");
            tvPreviewTime.setTypeface(monoFont);
            tvPreviewMs.setTypeface(monoFont);
        } catch (Exception e) {
            Typeface monoFont = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
            tvPreviewTime.setTypeface(monoFont);
            tvPreviewMs.setTypeface(monoFont);
        }
    }

    private void loadSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);

        int textRed = prefs.getInt("text_red", 0);
        int textGreen = prefs.getInt("text_green", 255);
        int textBlue = prefs.getInt("text_blue", 0);
        textColor = Color.rgb(textRed, textGreen, textBlue);

        msColorEnabled = prefs.getBoolean("ms_color_enabled", true);
        int msRed = prefs.getInt("ms_color_red", 255);
        int msGreen = prefs.getInt("ms_color_green", 0);
        int msBlue = prefs.getInt("ms_color_blue", 0);
        msColor = Color.rgb(msRed, msGreen, msBlue);

        String bgColorHex = prefs.getString("bg_color", "#99000000");
        try {
            bgColor = Color.parseColor(bgColorHex);
        } catch (Exception e) {
            bgColor = DEFAULT_BG_COLOR;
        }

        currentTimeFormat = prefs.getString("time_format", DEFAULT_TIME_FORMAT);

        switchMsColor.setChecked(msColorEnabled);
        updateSwitchLabel();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        switchMsColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            msColorEnabled = isChecked;
            updateSwitchLabel();
            updateMsPreview();
            saveMsColorEnabled();
        });

        btnSelectFormat.setOnClickListener(v -> showFormatDialog());
        btnReset.setOnClickListener(v -> showResetDialog());

        itemTextColor.setOnClickListener(v -> showTextColorPicker());
        itemMsColor.setOnClickListener(v -> showMsColorPicker());
        itemBgColor.setOnClickListener(v -> showBgColorPicker());
    }

    private void updateSwitchLabel() {
        switchMsLabel.setText(msColorEnabled ? "开启" : "关闭");
    }

    private void showTextColorPicker() {
        colorPickerTarget = TARGET_TEXT;
        CustomColorPickerDialog picker = new CustomColorPickerDialog(this, textColor, false);
        picker.setOnColorSelectedListener(color -> {
            textColor = color;
            updateTextColorPreview();
            saveTextColor();
        });
        picker.show("选择字体颜色");
    }

    private void showMsColorPicker() {
        colorPickerTarget = TARGET_MS;
        CustomColorPickerDialog picker = new CustomColorPickerDialog(this, msColor, false);
        picker.setOnColorSelectedListener(color -> {
            msColor = color;
            updateMsColorPreview();
            saveMsColor();
        });
        picker.show("选择毫秒颜色");
    }

    private void showBgColorPicker() {
        colorPickerTarget = TARGET_BG;
        CustomColorPickerDialog picker = new CustomColorPickerDialog(this, bgColor, true);
        picker.setOnColorSelectedListener(color -> {
            bgColor = color;
            updateBgColorPreview();
            saveBackgroundColor();
        });
        picker.show("选择背景颜色");
    }

    private void showFormatDialog() {
        int selectedIndex = 0;
        for (int i = 0; i < formatValues.length; i++) {
            if (formatValues[i].equals(currentTimeFormat)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择时间格式");
        builder.setSingleChoiceItems(formatNames, selectedIndex, (dialog, which) -> {
            selectTimeFormat(formatValues[which]);
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重置设置");
        builder.setMessage("确定要恢复到默认设置吗？\n\n默认设置：\n时间格式：完整格式\n字体颜色：绿色\n毫秒颜色：红色（默认开启）\n背景颜色：黑色（60%透明度）");
        builder.setPositiveButton("重置", (dialog, which) -> {
            resetToDefault();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void resetToDefault() {
        textColor = DEFAULT_TEXT_COLOR;
        msColor = DEFAULT_MS_COLOR;
        msColorEnabled = true;
        bgColor = DEFAULT_BG_COLOR;
        currentTimeFormat = DEFAULT_TIME_FORMAT;

        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("text_red", 0)
                .putInt("text_green", 255)
                .putInt("text_blue", 0)
                .putBoolean("ms_color_enabled", true)
                .putInt("ms_color_red", 255)
                .putInt("ms_color_green", 0)
                .putInt("ms_color_blue", 0)
                .putString("bg_color", "#99000000")
                .putString("time_format", DEFAULT_TIME_FORMAT)
                .apply();

        switchMsColor.setChecked(true);
        updateSwitchLabel();
        updatePreviews();
        FloatingTimeService.updateFloatingView();
    }

    private void updatePreviewTime() {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(currentTimeFormat, java.util.Locale.getDefault());
            String fullTime = sdf.format(new java.util.Date());

            if (msColorEnabled) {
                int dotIndex = fullTime.lastIndexOf('.');
                if (dotIndex >= 0) {
                    tvPreviewTime.setText(fullTime.substring(0, dotIndex));
                    tvPreviewMs.setText(fullTime.substring(dotIndex));
                } else {
                    tvPreviewTime.setText(fullTime);
                    tvPreviewMs.setText("");
                }
            } else {
                tvPreviewTime.setText(fullTime);
                tvPreviewMs.setText("");
            }
        } catch (Exception e) {
            tvPreviewTime.setText("12:34:56");
            tvPreviewMs.setText(".789");
        }
    }

    private void updateTextColorPreview() {
        tvPreviewTime.setTextColor(textColor);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(textColor);
        drawable.setCornerRadius(4);
        colorPreviewText.setBackground(drawable);
    }

    private void updateMsColorPreview() {
        tvPreviewMs.setTextColor(msColor);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(msColor);
        drawable.setCornerRadius(4);
        colorPreviewMs.setBackground(drawable);
    }

    private void updateBgColorPreview() {
        previewContainer.setBackgroundColor(bgColor);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(4);
        colorPreviewBg.setBackground(drawable);

        int alphaPercent = (int) (Color.alpha(bgColor) / 2.55);
        tvAlphaBg.setText("透明度: " + alphaPercent + "%");
    }

    private void updateMsPreview() {
        if (msColorEnabled) {
            tvPreviewMs.setVisibility(View.VISIBLE);
        } else {
            tvPreviewMs.setVisibility(View.GONE);
        }
    }

    private void updatePreviews() {
        updateTextColorPreview();
        updateMsColorPreview();
        updateBgColorPreview();
        updateMsPreview();
        updateFormatButton();
    }

    private void updateFormatButton() {
        for (int i = 0; i < formatValues.length; i++) {
            if (formatValues[i].equals(currentTimeFormat)) {
                btnSelectFormat.setText(formatNames[i]);
                return;
            }
        }
        btnSelectFormat.setText(formatNames[0]);
    }

    private void selectTimeFormat(String format) {
        currentTimeFormat = format;
        updateFormatButton();
        saveTimeFormat();
    }

    private void saveTextColor() {
        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("text_red", Color.red(textColor))
                .putInt("text_green", Color.green(textColor))
                .putInt("text_blue", Color.blue(textColor))
                .apply();

        FloatingTimeService.updateFloatingView();
    }

    private void saveMsColor() {
        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("ms_color_red", Color.red(msColor))
                .putInt("ms_color_green", Color.green(msColor))
                .putInt("ms_color_blue", Color.blue(msColor))
                .apply();

        FloatingTimeService.updateFloatingView();
    }

    private void saveMsColorEnabled() {
        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("ms_color_enabled", msColorEnabled).apply();

        FloatingTimeService.updateFloatingView();
    }

    private void saveBackgroundColor() {
        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);
        String colorHex = String.format("#%02X%02X%02X%02X",
            Color.alpha(bgColor),
            Color.red(bgColor),
            Color.green(bgColor),
            Color.blue(bgColor));
        prefs.edit().putString("bg_color", colorHex).apply();

        FloatingTimeService.updateFloatingView();
    }

    private void saveTimeFormat() {
        android.content.SharedPreferences prefs = getSharedPreferences("FloatingTimePrefs", MODE_PRIVATE);
        prefs.edit().putString("time_format", currentTimeFormat).apply();

        FloatingTimeService.updateFloatingView();
    }
}

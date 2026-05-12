package com.timepop.floatingtime;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class CustomColorPickerDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private OnColorSelectedListener listener;
    private int initialColor;
    private boolean withAlpha;
    private Context context;
    private int currentRed, currentGreen, currentBlue, currentAlpha;
    private SeekBar seekRed, seekGreen, seekBlue, seekAlpha;
    private View colorPreview;
    private TextView tvRed, tvGreen, tvBlue, tvAlpha, tvHex;

    public CustomColorPickerDialog(Context context, int initialColor, boolean withAlpha) {
        this.context = context;
        this.initialColor = initialColor;
        this.withAlpha = withAlpha;
        this.currentRed = Color.red(initialColor);
        this.currentGreen = Color.green(initialColor);
        this.currentBlue = Color.blue(initialColor);
        this.currentAlpha = Color.alpha(initialColor);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public void show(String title) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_color_picker, null);

        seekRed = view.findViewById(R.id.seekRed);
        seekGreen = view.findViewById(R.id.seekGreen);
        seekBlue = view.findViewById(R.id.seekBlue);
        seekAlpha = view.findViewById(R.id.seekAlpha);
        colorPreview = view.findViewById(R.id.colorPreview);
        tvRed = view.findViewById(R.id.tvRed);
        tvGreen = view.findViewById(R.id.tvGreen);
        tvBlue = view.findViewById(R.id.tvBlue);
        tvAlpha = view.findViewById(R.id.tvAlpha);
        tvHex = view.findViewById(R.id.tvHex);
        View alphaContainer = view.findViewById(R.id.alphaContainer);

        if (!withAlpha) {
            alphaContainer.setVisibility(View.GONE);
        }

        setupSeekBar(seekRed, currentRed, Color.RED, tvRed);
        setupSeekBar(seekGreen, currentGreen, Color.GREEN, tvGreen);
        setupSeekBar(seekBlue, currentBlue, Color.BLUE, tvBlue);
        setupSeekBar(seekAlpha, currentAlpha, Color.BLACK, tvAlpha);

        updatePreview();
        updateHexText();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(view);
        builder.setPositiveButton("确定", (dialog, which) -> {
            if (listener != null) {
                int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
                listener.onColorSelected(color);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void setupSeekBar(SeekBar seekBar, int value, int colorType, TextView textView) {
        seekBar.setMax(255);
        seekBar.setProgress(value);
        textView.setText(String.valueOf(value));

        int barColor;
        switch (colorType) {
            case Color.RED:
                barColor = Color.RED;
                break;
            case Color.GREEN:
                barColor = Color.GREEN;
                break;
            case Color.BLUE:
                barColor = Color.BLUE;
                break;
            default:
                barColor = Color.GRAY;
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    switch (colorType) {
                        case Color.RED:
                            currentRed = progress;
                            break;
                        case Color.GREEN:
                            currentGreen = progress;
                            break;
                        case Color.BLUE:
                            currentBlue = progress;
                            break;
                        default:
                            currentAlpha = progress;
                            break;
                    }
                    textView.setText(String.valueOf(progress));
                    updatePreview();
                    updateHexText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updatePreview() {
        int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
        colorPreview.setBackgroundColor(color);
    }

    private void updateHexText() {
        int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
        String hex = String.format("#%02X%02X%02X%02X", currentAlpha, currentRed, currentGreen, currentBlue);
        tvHex.setText(hex);
    }
}

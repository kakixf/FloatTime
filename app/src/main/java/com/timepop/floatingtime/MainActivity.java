package com.timepop.floatingtime;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;

    private ImageButton btnToggle;
    private ImageButton btnSettings;
    private TextView tvStatus;

    private SharedPreferences prefs;
    private Handler handler;
    private boolean isCheckingStatus = false;
    private Boolean lastKnownRunning = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("FloatingTimePrefs", Context.MODE_PRIVATE);

        if (!isServiceRunning()) {
            prefs.edit().putBoolean("service_running", false).apply();
        }

        handler = new Handler();

        btnToggle = findViewById(R.id.btnToggle);
        btnSettings = findViewById(R.id.btnSettings);
        tvStatus = findViewById(R.id.tvStatus);

        btnToggle.setOnClickListener(v -> {
            boolean isRunning = prefs.getBoolean("service_running", false);
            if (isRunning) {
                stopFloatingService();
            } else {
                checkPermissionsAndStartService();
            }
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingTimeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastKnownRunning = prefs.getBoolean("service_running", false);
        updateStatus();
        startStatusCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopStatusCheck();
    }

    private void startStatusCheck() {
        if (isCheckingStatus) return;
        isCheckingStatus = true;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isCheckingStatus) return;

                boolean currentRunning = prefs.getBoolean("service_running", false);

                if (lastKnownRunning != null && currentRunning != lastKnownRunning) {
                    lastKnownRunning = currentRunning;
                    updateStatusDirect(currentRunning);
                }

                handler.postDelayed(this, 200);
            }
        });
    }

    private void stopStatusCheck() {
        isCheckingStatus = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void checkPermissionsAndStartService() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_NOTIFICATION_PERMISSION);
                return;
            }
        }

        startFloatingService();
    }

    private void startFloatingService() {
        tvStatus.setText("正在启动悬浮时间...");
        btnToggle.setEnabled(false);

        Intent serviceIntent = new Intent(this, FloatingTimeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        prefs.edit().putBoolean("service_running", true).apply();

        btnToggle.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        }, 500);
    }

    private void stopFloatingService() {
        tvStatus.setText("正在停止悬浮时间...");
        btnToggle.setEnabled(false);

        Intent serviceIntent = new Intent(this, FloatingTimeService.class);
        stopService(serviceIntent);

        prefs.edit().putBoolean("service_running", false).apply();

        btnToggle.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        }, 300);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                checkPermissionsAndStartService();
            } else {
                tvStatus.setText("需要悬浮窗权限才能显示时间");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndStartService();
            } else {
                tvStatus.setText("需要通知权限才能运行服务");
            }
        }
    }

    private void updateStatus() {
        boolean isRunning = prefs.getBoolean("service_running", false);
        lastKnownRunning = isRunning;
        updateStatusDirect(isRunning);
    }

    private void updateStatusDirect(boolean running) {
        if (running) {
            tvStatus.setText("悬浮时间正在运行中");
            btnToggle.setImageResource(R.drawable.ic_stop);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(0xFFF44336);
            btnToggle.setBackground(drawable);
            btnToggle.setEnabled(true);
        } else {
            tvStatus.setText("点击开始悬浮时间");
            btnToggle.setImageResource(R.drawable.ic_play);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(0xFF4CAF50);
            btnToggle.setBackground(drawable);
            btnToggle.setEnabled(true);
        }
    }
}

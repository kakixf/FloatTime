package com.timepop.floatingtime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloatingTimeService extends Service {

    private static final String CHANNEL_ID = "FloatingTimeServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    private static final String TAG = "FloatingTimeService";

    private static final String PREFS_NAME = "FloatingTimePrefs";
    private static final String PREF_X = "floating_x";
    private static final String PREF_Y = "floating_y";
    private static final String PREF_SERVICE_RUNNING = "service_running";

    private static volatile FloatingTimeService instance;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private TextView tvTime;
    private TextView tvMs;
    private Button btnStop;
    private Timer timer;
    private Handler handler;
    private SimpleDateFormat timeFormat;
    private SharedPreferences prefs;
    private int screenWidth;
    private int screenHeight;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isRunning.set(true);
        handler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SERVICE_RUNNING, true).apply();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        createFloatingWindow();
        startTimeUpdate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning.set(false);
        prefs.edit().putBoolean(PREF_SERVICE_RUNNING, false).apply();
        instance = null;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    public static boolean isRunning() {
        return isRunning.get();
    }

    public static void updateFloatingView() {
        if (instance != null && instance.floatingView != null) {
            instance.handler.post(() -> {
                instance.updateAppearance();
                instance.updateTimeFormat();
                instance.updateWindowWidth();
            });
        }
    }

    private void updateWindowWidth() {
        if (params == null || windowManager == null) {
            Log.w(TAG, "updateWindowWidth: params 或 windowManager 为 null");
            return;
        }

        floatingView.requestLayout();
        windowManager.updateViewLayout(floatingView, params);
        Log.d(TAG, "updateWindowWidth: 已刷新布局");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "悬浮时间服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持悬浮时间显示的服务");
            channel.setShowBadge(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮时间")
            .setContentText("时间显示服务正在运行")
            .setSmallIcon(R.drawable.ic_time)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void createFloatingWindow() {
        Log.d(TAG, "createFloatingWindow: 开始创建悬浮窗口");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_time, null);
        tvTime = floatingView.findViewById(R.id.tvFloatingTime);
        tvMs = floatingView.findViewById(R.id.tvFloatingMs);
        btnStop = floatingView.findViewById(R.id.btnFloatingStop);

        Typeface monoTypeface = getSystemMonospaceFont();
        if (monoTypeface != null) {
            tvTime.setTypeface(monoTypeface);
            tvMs.setTypeface(monoTypeface);
        }

        btnStop.setOnClickListener(v -> {
            prefs.edit().putBoolean(PREF_SERVICE_RUNNING, false).apply();
            stopSelf();
        });

        updateAppearance();

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            Log.d(TAG, "createFloatingWindow: 使用 TYPE_APPLICATION_OVERLAY");
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            Log.d(TAG, "createFloatingWindow: 使用 TYPE_PHONE");
        }

        int width = (int) (getResources().getDisplayMetrics().density * 140);
        int height = (int) (getResources().getDisplayMetrics().density * 28);
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;

        params.x = prefs.getInt(PREF_X, 100);
        params.y = prefs.getInt(PREF_Y, 200);

        try {
            Log.d(TAG, "createFloatingWindow: 尝试添加悬浮窗口");
            windowManager.addView(floatingView, params);
            Log.d(TAG, "createFloatingWindow: 悬浮窗口添加成功，位置: x=" + params.x + ", y=" + params.y);
        } catch (Exception e) {
            Log.e(TAG, "createFloatingWindow: 添加悬浮窗口失败", e);
            e.printStackTrace();
        }

        setupTouchListener();
    }

    private void updateAppearance() {
        if (floatingView == null) return;

        int red = prefs.getInt("text_red", 0);
        int green = prefs.getInt("text_green", 255);
        int blue = prefs.getInt("text_blue", 0);
        int textColor = Color.rgb(red, green, blue);

        tvTime.setTextColor(textColor);

        String timeFormatStr = prefs.getString("time_format", "HH:mm:ss.SSS");
        boolean msColorEnabled = prefs.getBoolean("ms_color_enabled", true);

        float density = getResources().getDisplayMetrics().density;

        if ("mm:ss.SSS".equals(timeFormatStr)) {
            tvTime.setMinWidth((int) (42 * density));
        } else {
            tvTime.setMinWidth((int) (60 * density));
        }

        if (msColorEnabled && timeFormatStr.contains("SSS")) {
            tvMs.setVisibility(View.VISIBLE);
            int msRed = prefs.getInt("ms_color_red", 255);
            int msGreen = prefs.getInt("ms_color_green", 0);
            int msBlue = prefs.getInt("ms_color_blue", 0);
            int msColor = Color.rgb(msRed, msGreen, msBlue);
            tvMs.setTextColor(msColor);
        } else {
            tvMs.setVisibility(View.GONE);
        }

        String bgColorHex = prefs.getString("bg_color", "#CC000000");
        try {
            int bgColor = Color.parseColor(bgColorHex);
            floatingView.setBackgroundColor(bgColor);
        } catch (Exception e) {
            floatingView.setBackgroundColor(0xCC000000);
        }
    }

    private void updateTimeFormat() {
        if (floatingView == null) return;

        String format = prefs.getString("time_format", "HH:mm:ss.SSS");
        timeFormat = new SimpleDateFormat(format, Locale.getDefault());
    }

    private void setupTouchListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        checkBoundaryAndAdjust();
                        prefs.edit()
                            .putInt(PREF_X, params.x)
                            .putInt(PREF_Y, params.y)
                            .apply();
                        return true;
                }
                return false;
            }
        });
    }

    private Typeface getSystemMonospaceFont() {
        try {
            Typeface customFont = Typeface.createFromAsset(getAssets(), "fonts/RobotoMono-Bold.ttf");
            if (customFont != null) {
                Log.d(TAG, "成功加载自定义RobotoMono字体");
                return customFont;
            }
        } catch (Exception e) {
            Log.e(TAG, "加载RobotoMono字体失败: " + e.getMessage());
            e.printStackTrace();
        }

        Typeface monoFont = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        Log.d(TAG, "回退到系统MONOSPACE字体");
        return monoFont;
    }

    private void checkBoundaryAndAdjust() {
        if (floatingView == null || windowManager == null) return;

        floatingView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        );

        int viewWidth = floatingView.getMeasuredWidth();
        int viewHeight = floatingView.getMeasuredHeight();

        boolean adjusted = false;

        if (params.x < 0) {
            params.x = 0;
            adjusted = true;
        } else if (params.x + viewWidth > screenWidth) {
            params.x = screenWidth - viewWidth;
            adjusted = true;
        }

        if (params.y < 0) {
            params.y = 0;
            adjusted = true;
        } else if (params.y + viewHeight > screenHeight) {
            params.y = screenHeight - viewHeight;
            adjusted = true;
        }

        if (adjusted) {
            windowManager.updateViewLayout(floatingView, params);
        }
    }

    private void startTimeUpdate() {
        String format = prefs.getString("time_format", "HH:mm:ss.SSS");
        timeFormat = new SimpleDateFormat(format, Locale.getDefault());

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateTime();
                    }
                });
            }
        }, 0, 10);
    }

    private void updateTime() {
        if (tvTime == null || timeFormat == null) return;

        Date now = new Date();
        String fullTime = timeFormat.format(now);

        boolean msColorEnabled = prefs.getBoolean("ms_color_enabled", true);

        if (msColorEnabled) {
            int dotIndex = fullTime.lastIndexOf('.');
            if (dotIndex >= 0) {
                tvTime.setText(fullTime.substring(0, dotIndex));
                String ms = fullTime.substring(dotIndex);
                tvMs.setText(ms);
            } else {
                tvTime.setText(fullTime);
                tvMs.setText("");
            }
        } else {
            tvTime.setText(fullTime);
            tvMs.setText("");
        }
    }
}

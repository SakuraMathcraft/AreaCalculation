package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class TrackingService extends Service {

    private static final String CHANNEL_ID = "tracking_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建通知渠道（针对 Android 8.0 及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "轨迹记录通知",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 构建通知内容
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("轨迹记录进行中")
                .setContentText("正在后台记录您的位置和步数")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 你可以替换成更合适的图标
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // 启动前台服务
        startForeground(1, notification);

        // 保持运行
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 服务销毁时可做清理操作
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不支持绑定
    }
}

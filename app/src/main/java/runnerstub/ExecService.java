package runnerstub;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static runnerstub.ExecActivity.newProcess;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;
import runner.stub.R;

import android.content.pm.PackageManager;
import android.Manifest;
import android.widget.Toast;

public class ExecService extends Service {
    public static final String NOTIFICATION_CHANNEL = "exec_service";
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_STOP = "runnerstub.ExecService.ACTION_STOP";
    private static final String TAG = "ExecService";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Thread workerThread;
    private NotificationManager notificationManager;
    private ShizukuRemoteProcess mProcess;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Android 13+ 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "缺少通知权限，无法显示通知");
                Toast.makeText(this, "请授予通知权限以显示通知", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            } else {
                Log.d(TAG, "已获得通知权限");
            }
        }
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(""), FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(""));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "Stopping service");
            if (mProcess != null) {
                mProcess.destroy();
            }
            if (workerThread != null && workerThread.isAlive())
                workerThread.interrupt();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.d(TAG, "Service started");
        if (workerThread == null || !workerThread.isAlive()) {
            workerThread = new Thread(() -> {
                if (!waitForBinder()) {
                    mHandler.post(() -> {
                        updateNotification("等待 Shizuku Binder 超时");
                        stopSelf();
                    });
                    return;
                }
                Config.load(getAssets());
                try {
                    //记录执行开始的时间
                    long time = System.currentTimeMillis();

                    //使用Shizuku执行命令
                    mProcess = newProcess(new String[]{"sh"});
                    if (mProcess == null) {
                        mHandler.post(() -> {
                            updateNotification("Shizuku 进程创建失败");
                            stopSelf();
                        });
                        return;
                    }
                    mHandler.post(this::updateExecNotification);

                    OutputStream out = mProcess.getOutputStream();
                    out.write((Config.COMMAND + "\nexit\n").getBytes());
                    out.flush();
                    out.close();

                    //等待命令运行完毕
                    String exitValue = String.valueOf(mProcess.waitFor());

                    //显示命令返回值和命令执行时长
                    mHandler.post(() -> {
                        overExecNotification(String.format(Locale.getDefault(), "返回值：%s\n执行用时：%.2f秒", exitValue, (System.currentTimeMillis() - time) / 1000f));
                        stopSelf();
                    });
                } catch (Exception ignored) {
                }
            });
            workerThread.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if (mProcess != null) {
            mProcess.destroy();
        }
        if (workerThread != null && workerThread.isAlive())
            workerThread.interrupt();
        // 不再调用 stopForeground(false)，避免通知被移除
        // super.onDestroy() 保持不变
        super.onDestroy();
    }

    private void createNotificationChannel() {
        Log.d(TAG, "Creating notification channel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    "执行命令服务",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setSound(null, null);
            channel.setShowBadge(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(false);
            }
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void overExecNotification(String content) {
        Log.d(TAG, "overExecNotification: " + content);
        // 先降级为普通服务，保留通知
//        stopForeground(false);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle("执行命令完成")
                .setContentText(content)
                .setSmallIcon(R.drawable.baseline_keyboard_arrow_right_24)
                .setOngoing(false);
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            Log.e(TAG, "notificationManager is null, 无法发布通知");
        }
    }

    private void updateExecNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle("正在执行命令")
                .setContentText("")
                .setSmallIcon(R.drawable.baseline_keyboard_arrow_right_24)
                .setOngoing(false);
        Intent stopIntent = new Intent(this, ExecService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.baseline_stop_circle_24, "停止", stopPendingIntent);
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            Log.e(TAG, "notificationManager is null, 无法发布通知");
        }
    }

    private void updateNotification(String content) {
        Log.d(TAG, "updateNotification: " + content);
        Notification notification = buildNotification(content);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            Log.e(TAG, "notificationManager is null, 无法发布通知");
        }
    }

    private Notification buildNotification(String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle("执行命令服务")
                .setContentText(content)
                .setSmallIcon(R.drawable.baseline_keyboard_arrow_right_24)
                .setOngoing(true);
        Intent stopIntent = new Intent(this, ExecService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.baseline_stop_circle_24, "停止", stopPendingIntent);
        return builder.build();
    }

    final CountDownLatch countDownLatch = new CountDownLatch(1);

    private boolean waitForBinder() {
        Log.d(TAG, "Waiting for Shizuku Binder...");
        updateNotification("等待 Shizuku Binder...");
        Shizuku.OnBinderReceivedListener listener = new Shizuku.OnBinderReceivedListener() {
            @Override
            public void onBinderReceived() {
                countDownLatch.countDown();
                Shizuku.removeBinderReceivedListener(this);
            }
        };
        Shizuku.addBinderReceivedListenerSticky(listener, null);
        try {
            countDownLatch.await(5, TimeUnit.SECONDS);
            Log.d(TAG, "Shizuku Binder received");
            Shizuku.removeBinderReceivedListener(listener);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Binder not received", e);
            Shizuku.removeBinderReceivedListener(listener);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Binder not received in 5s", e);
            Shizuku.removeBinderReceivedListener(listener);
            return false;
        }
    }
}

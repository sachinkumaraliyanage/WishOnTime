package com.techbirdssolutions.wishontime;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.telephony.SmsManager;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SMSForegroundService extends Service {
    public static final String CHANNEL_ID = "WishOnTime_Channel";
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;
    private boolean isSending = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String message = intent.getStringExtra("msg");
        ArrayList<String> recipients = intent.getStringArrayListExtra("list");
        long scheduledTime = intent.getLongExtra("scheduledTime", 0);

        createNotificationChannel();
        
        // We MUST call startForeground immediately to avoid "RemoteServiceException"
        Notification initialNotification = buildNotification("Preparing schedule...", 0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(101, initialNotification);
        }

        if (scheduledTime > System.currentTimeMillis()) {
            startCountdown(message, recipients, scheduledTime);
        } else {
            startSending(message, recipients);
        }

        return START_NOT_STICKY;
    }

    private void startCountdown(String message, ArrayList<String> recipients, long scheduledTime) {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingMillis = scheduledTime - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    startSending(message, recipients);
                } else {
                    String timeStr = formatTime(remainingMillis);
                    updateNotification("Starting in: " + timeStr, 0, 0);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void startSending(String message, ArrayList<String> recipients) {
        if (isSending) return;
        isSending = true;
        
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }

        new Thread(() -> {
            SmsManager smsManager = getSystemService(SmsManager.class);
            for (int i = 0; i < recipients.size(); i++) {
                updateNotification("Sending to " + (i + 1) + "/" + recipients.size(), i + 1, recipients.size());

                try {
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(recipients.get(i), null, parts, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }).start();
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateNotification(String text, int progress, int max) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(101, buildNotification(text, progress, max));
        }
    }

    private Notification buildNotification(String text, int progress, int max) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WishOnTime Active")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (max > 0) {
            builder.setProgress(max, progress, false);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (handler != null && countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
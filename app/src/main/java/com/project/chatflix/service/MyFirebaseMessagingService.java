package com.project.chatflix.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.project.chatflix.BuildConfig;
import com.project.chatflix.R;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {
            if (remoteMessage.getData() == null) {

                if (remoteMessage.getNotification() != null) {
                    String body = remoteMessage.getNotification().getBody() + "";
                    String title = remoteMessage.getNotification().getTitle() == null ? "Thông báo mới" : "Có thông báo";
                    sendNotification(title, body, null);
                }
            } else {

                Map<String, String> notification = remoteMessage.getData();
                String body = notification.get("body");
                String title = notification.get("title");
                if (title == null || body == null) {
                    sendNotification(getApplicationContext().getString(R.string.title_fbid_notification_new),
                            getApplicationContext().getString(R.string.body_fbid_notification_new), null);
                } else {
                    sendNotification(title, body, notification);
                }

            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.d("" + getClass().getName(), e.getMessage());
        }
    }

    private void sendNotification(String title, String messageBody, Map<String, String> map) {

        try {
            String channelId = "csell.com.vn.csell.ONE";
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Intent switchIntent = new Intent(this, switchButtonListener.class);


            PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(getApplication(), 0,
                    switchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.drawable.logo_1)
                            .setContentTitle(title)
                            .setContentText(messageBody)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingSwitchIntent);
//                            .addAction(android.R.drawable.ic_menu_compass, this.getString(R.string.text_title_detail_product), pendingSwitchIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.enableLights(false);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify("CSELL", 0 /* ID of notification */, notificationBuilder.build());
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("" + getClass().getName(), e.getMessage());
        }
    }

    public static class switchButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {

            } catch (Exception e) {
                if (BuildConfig.DEBUG) Log.e(getClass().getName(), e.toString());
                Crashlytics.logException(e);
            }
        }
    }

}
package com.smartfoxitsolutions.lockup.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.smartfoxitsolutions.lockup.AppLoaderActivity;
import com.smartfoxitsolutions.lockup.R;

import java.util.List;
import java.util.Map;

/**
 * Created by RAAJA on 31-12-2016.
 */

public class AppUpdateService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if(remoteMessage!=null) {
            Map<String, String> data = remoteMessage.getData();
            if (data != null && data.size() > 0) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    Log.d("LockFireService", entry.getKey() + " Key " + entry.getValue() + " Value");
                    String key = entry.getKey();
                    String value = entry.getValue();
                    String title="LockUp";
                    String body = "Open to lock apps";
                    if(key.equals(AppLoaderActivity.UPDATE_APP_CLOUD_KEY)) {
                        if (remoteMessage.getNotification() != null) {
                            RemoteMessage.Notification notification = remoteMessage.getNotification();
                            if (notification.getTitle() != null) {
                                Log.d("LockFireService", notification.getTitle());
                                title = notification.getTitle();
                            }
                            if (notification.getBody() != null) {
                                Log.d("LockFireService", notification.getBody());
                                body = notification.getBody();
                            }
                        }
                    }
                    sendNotification(value,title,body);
                }
            }
        }
        stopSelf();
    }

    Bitmap getLauncherIcon(){
        return BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
    }

    void sendNotification(String packageName,String title, String body){
        NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        List<ResolveInfo> marketInfo = getPackageManager().queryIntentActivities(marketIntent, PackageManager.GET_META_DATA);
       if(marketInfo.size()>0) {
           for (ResolveInfo info : marketInfo) {
               if (info.activityInfo.packageName.equals("com.android.vending")) {
                   break;
               } else {
                   marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName));
               }
           }
       }else{
           marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName));
       }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
        builder.setContentTitle(title);
        builder.setContentText(body)
                .setOngoing(false)
                .setAutoCancel(true)
                .setLargeIcon(getLauncherIcon())
                .setContentIntent(PendingIntent.getActivity(getBaseContext(),35,marketIntent
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        ,PendingIntent.FLAG_UPDATE_CURRENT))
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#2874F0"));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            builder.setSmallIcon(R.drawable.ic_notification_small);
        }else{
            builder.setSmallIcon(R.mipmap.ic_launcher);
        }
        notifManager.notify(7549682, builder.build());
    }
}

package com.smartfoxitsolutions.lockup.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 27-11-2016.
 */

public class AppLockForegroundService extends Service {

    public static final String FOREGROUND_SERVICE_TYPE = "foreground_service_type";

    public static final int APP_LOCK_SERVICE = 43;
    public static final int MEDIA_MOVE_SERVICE = 44;
    public static final int SHARE_MOVE_SERVICE = 45;

    int serviceType;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        serviceType = intent.getIntExtra(FOREGROUND_SERVICE_TYPE,0);
        if(serviceType == APP_LOCK_SERVICE) {
            startForeground(596804950, getNotification());
            stopForeground(true);
        }
        if(serviceType == MEDIA_MOVE_SERVICE){
            startForeground(25648751, getNotification());
            stopForeground(true);
        }
        if(serviceType == SHARE_MOVE_SERVICE){
            startForeground(215879634, getNotification());
            stopForeground(true);
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    Notification getNotification(){
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
        if(serviceType==APP_LOCK_SERVICE) {
            notifBuilder.setContentTitle("AppLock Running");
            notifBuilder.setContentText("Touch to disable AppLock");
            notifBuilder.setOngoing(true)
                    .setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOnlyAlertOnce(true)
                    .setColor(Color.parseColor("#2874F0"));
        }
        if(serviceType == MEDIA_MOVE_SERVICE){
            notifBuilder.setContentTitle("LockUp Vault");
            notifBuilder.setContentText("Moving Media");
            notifBuilder.setOngoing(true)
                    .setAutoCancel(false)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOnlyAlertOnce(true)
                    .setColor(Color.parseColor("#2874F0"));
        }
        if(serviceType == SHARE_MOVE_SERVICE){
            notifBuilder.setContentTitle("LockUp Vault");
            notifBuilder.setContentText("Moving Media");
            notifBuilder.setOngoing(true)
                    .setAutoCancel(false)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOnlyAlertOnce(true)
                    .setColor(Color.parseColor("#2874F0"));
        }
        return notifBuilder.build();
    }

}

package com.smartfoxitsolutions.lockup.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 27-11-2016.
 */

public class AppLockForegroundService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(596804950,getNotification());
        stopForeground(true);
        stopSelf();
        return START_NOT_STICKY;
    }

    Notification getNotification(){
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
         notifBuilder.setContentTitle("AppLock Running");
         notifBuilder.setContentText("Touch to disable AppLock");
        notifBuilder .setOngoing(true)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#ffffff"));
        return notifBuilder.build();
    }

}

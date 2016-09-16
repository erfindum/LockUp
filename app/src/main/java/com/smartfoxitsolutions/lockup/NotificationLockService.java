package com.smartfoxitsolutions.lockup;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by RAAJA on 08-09-2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationLockService extends NotificationListenerService {
    Gson gson;
    private Type checkedAppsMapToken;
    private ArrayList<String> checkedAppsList;
    private static boolean notificationServiceConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        checkedAppsList = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
        String checkedAppsJSONString = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                .getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,null);
        TreeMap<String,String> checkedAppsMap = gson.fromJson(checkedAppsJSONString,checkedAppsMapToken);
        if(checkedAppsMap!=null){
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }

        return START_STICKY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
            String packageName = sbn.getPackageName();
        if(checkedAppsList.contains(packageName) && !sbn.isOngoing()){
                clearAndPostNotification(sbn);
            }
        super.onNotificationPosted(sbn);
    }

    void clearAndPostNotification(StatusBarNotification sbn){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            cancelNotification(sbn.getKey());
        }
        else{
            cancelNotification(sbn.getPackageName(), sbn.getTag(),sbn.getId());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        Log.d("PatternLock","Notificaion Removed" + sbn.getPackageName());
        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        NotificationLockService.notificationServiceConnected = true;
        Log.d("PatternLock","Listener Connected");
    }



}

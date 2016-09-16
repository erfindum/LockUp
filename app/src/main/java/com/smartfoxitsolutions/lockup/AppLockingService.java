package com.smartfoxitsolutions.lockup;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class AppLockingService extends Service implements Handler.Callback{
    public static final String TAG = "AppLockingService";

    public static final int RECENT_APP_INFO =2;
    public static String recentlyUnlockedApp = "NIL";
    public static final String NIL_APPS_LOCKED = "NIL";

    private ScheduledExecutorService appLockService;
    private AppLockQueryTask appLockQueryTask;
    private Gson gson;
    private Type checkedAppsMapToken;
    private ArrayList<String> checkedAppsList, launcherAppsList;
    private String systemUIApp = "com.android.systemui";
    private AppLockBroadcastReceiver appLockBroadcastReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        checkedAppsList = new ArrayList<>();
        launcherAppsList = new ArrayList<>();
        checkedAppsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
        List<ResolveInfo> launcherInfo= getBaseContext().getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),PackageManager.GET_META_DATA);
        for (ResolveInfo info : launcherInfo ){
            launcherAppsList.add(info.activityInfo.packageName);
        }
        appLockQueryTask = new AppLockQueryTask(getApplicationContext(),this);
        appLockService = Executors.newScheduledThreadPool(3);
        appLockService.scheduleAtFixedRate(appLockQueryTask,0,500, TimeUnit.MILLISECONDS);
        appLockBroadcastReceiver = new AppLockBroadcastReceiver();
        registerReceiver(appLockBroadcastReceiver,new IntentFilter(Intent.ACTION_SCREEN_OFF));
        unregisterReceiver(appLockBroadcastReceiver);
        registerReceiver(appLockBroadcastReceiver,new IntentFilter(Intent.ACTION_PACKAGE_ADDED));
}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG,"Service Started");
        String checkedAppsJSONString = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                                    .getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,null);
        TreeMap<String,String> checkedAppsMap = gson.fromJson(checkedAppsJSONString,checkedAppsMapToken);
        if(checkedAppsMap!=null){
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }

        return START_STICKY;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == RECENT_APP_INFO){
            String foregroundApp = (String) msg.obj;
            if(checkedAppsList.contains(foregroundApp)){
                if(!foregroundApp.equals(recentlyUnlockedApp)){
                    recentlyUnlockedApp = NIL_APPS_LOCKED;
                    //Start Lock Screen
                }
                else if(launcherAppsList.contains(foregroundApp) || foregroundApp.equals(systemUIApp)){
                    recentlyUnlockedApp = NIL_APPS_LOCKED;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!appLockService.isShutdown()){
            appLockService.shutdown();
        }
        unregisterReceiver(appLockBroadcastReceiver);
        registerReceiver(appLockBroadcastReceiver,new IntentFilter(Intent.ACTION_SCREEN_ON));
        unregisterReceiver(appLockBroadcastReceiver);
    }


}

package com.smartfoxitsolutions.lockup;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class AppLockingService extends Service implements Handler.Callback{
    public static final String TAG = "AppLockingService";

    public static final int RECENT_APP_INFO =2;
    public static String recentlyUnlockedApp = "NIL";
    public static final String NIL_APPS_LOCKED = "NIL";
    public static final String CHECKED_APP_LOCK_PACKAGE_NAME = "checkedAppPackageName";
    public static final String CHECKED_APP_LOCK_COLOR = "checkedAppColor";

    private ScheduledExecutorService appLockService;
    private AppLockQueryTask appLockQueryTask;
    private Gson gson;
    private Type checkedAppsMapToken, checkedAppsColorMapToken;
    private ArrayList<String> checkedAppsList, launcherAppsList;
    private AppLockReceiver appLockReceiver;
    private int appLockMode;
    private TreeMap<String,Integer> checkedAppColorMap;

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
        checkedAppsColorMapToken = new TypeToken<TreeMap<String,Integer>>(){}.getType();
        checkedAppColorMap = new TreeMap<>();
        appLockMode = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                            .getInt(AppLockModel.APP_LOCK_LOCKMODE,54);
                List<ResolveInfo> launcherInfo= getBaseContext().getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.GET_META_DATA);
        for (ResolveInfo info : launcherInfo ){
            launcherAppsList.add(info.activityInfo.packageName);
        }

        appLockQueryTask = new AppLockQueryTask(getApplicationContext(),this);
        appLockService = Executors.newScheduledThreadPool(4);
        appLockService.scheduleAtFixedRate(appLockQueryTask,0,500, TimeUnit.MILLISECONDS);
        appLockReceiver = new AppLockReceiver();
        IntentFilter appLockFilter = new IntentFilter();
        appLockFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        registerReceiver(appLockReceiver,appLockFilter);
}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG,"Service Started");
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        String checkedAppsColorJSONString = prefs.getString(AppLockModel.CHECKED_APPS_COLOR_SHARED_PREF_KEY,null);
        String checkedAppsJSONString = prefs.getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,null);
        TreeMap<String,Integer> checkedAppsColor = gson.fromJson(checkedAppsColorJSONString,checkedAppsColorMapToken);
        TreeMap<String,String> checkedAppsMap = gson.fromJson(checkedAppsJSONString,checkedAppsMapToken);
        if(checkedAppsMap!=null){
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }
        if(checkedAppsColor!=null){
            checkedAppColorMap = checkedAppsColor;
        }

        for(Map.Entry<String,Integer> color : checkedAppColorMap.entrySet() ){
            Log.d("App Lock",color.getValue() + " ");
        }

        return START_STICKY;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == RECENT_APP_INFO){
            String systemUIApp = "com.android.systemui";
            String checkedAppPackage = (String) msg.obj;
            if(checkedAppsList.contains(checkedAppPackage)){
                if(!checkedAppPackage.equals(recentlyUnlockedApp)){
                    recentlyUnlockedApp = NIL_APPS_LOCKED;
                    if(appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN){
                        startActivity(new Intent(getBaseContext(),PatternLockActivity.class)
                                .putExtra(CHECKED_APP_LOCK_PACKAGE_NAME,checkedAppPackage)
                                .putExtra(CHECKED_APP_LOCK_COLOR,checkedAppColorMap.get(checkedAppPackage))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    }else{
                        startActivity(new Intent(getBaseContext(),PinLockActivity.class)
                                .putExtra(CHECKED_APP_LOCK_PACKAGE_NAME,checkedAppPackage)
                                .putExtra(CHECKED_APP_LOCK_COLOR,checkedAppColorMap.get(checkedAppPackage))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    }
                }
            }
            else if(launcherAppsList.contains(checkedAppPackage) || systemUIApp.equals(checkedAppPackage)){
                recentlyUnlockedApp = NIL_APPS_LOCKED;
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
            appLockQueryTask= null;
        }
        unregisterReceiver(appLockReceiver);
        Log.d("AppLock","Service Destroyed");
    }


}

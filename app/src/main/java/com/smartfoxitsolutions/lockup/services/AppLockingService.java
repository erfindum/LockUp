package com.smartfoxitsolutions.lockup.services;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.FingerPrintActivity;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.receivers.AppLockServiceRestartReceiver;
import com.smartfoxitsolutions.lockup.views.LockPatternView;
import com.smartfoxitsolutions.lockup.views.LockPatternViewFinger;
import com.smartfoxitsolutions.lockup.views.LockPinView;
import com.smartfoxitsolutions.lockup.views.LockPinViewFinger;
import com.smartfoxitsolutions.lockup.views.OnFingerScannerCancelListener;
import com.smartfoxitsolutions.lockup.views.OnPinLockUnlockListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class AppLockingService extends Service implements Handler.Callback,OnPinLockUnlockListener, OnFingerScannerCancelListener {
    public static final String TAG = "AppLockingService";

    public static final String STOP_APP_LOCK_SERVICE = "stopAppLockService";

    public static boolean isAppLockRunning = false;
    public static final int RECENT_APP_INFO_V21_DOWN =2;
    public static final int RECENT_APP_INFO_V21_UP =4;

    public static String recentlyLockedApp = "NIL";
    public static final String NIL_APPS_LOCKED = "NIL";

    private ScheduledExecutorService appLockService;
    private AppLockQueryTask appLockQueryTask;
    private Gson gson;
    private Type checkedAppsMapToken, checkedAppsColorMapToken, recommendedAppsMapToken;
    private ArrayList<String> checkedAppsList, launcherAppsList, recommendedAppsList;
    private AppLockReceiver appLockReceiver;
    private int appLockMode;
    private TreeMap<String,Integer> checkedAppColorMap;
    private String systemUIApp;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LockPinView lockPinView;
    private LockPinViewFinger lockPinViewFinger;
    private LockPatternView patternLockView;
    private LockPatternViewFinger patternLockViewFinger;
    private boolean isFingerPrintLockActive,isScreenOn ;
    private boolean stopAppLock, shouldLockOnScreenOn;
    int displayHeight;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AppLockService","Called Service OnCreate" + System.currentTimeMillis());
        gson = new Gson();
        systemUIApp = "com.android.systemui";
        windowManager = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);
        setWindowLayoutParams();
        checkedAppsList = new ArrayList<>();
        launcherAppsList = new ArrayList<>();
        recommendedAppsList = new ArrayList<>();
        checkedAppsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
        checkedAppsColorMapToken = new TypeToken<TreeMap<String,Integer>>(){}.getType();
        recommendedAppsMapToken = new TypeToken<LinkedHashMap<String,HashMap<String,Boolean>>>(){}.getType();
        checkedAppColorMap = new TreeMap<>();
        loadLaunchers();
        scheduleAppQuery();
        appLockReceiver = new AppLockReceiver();
        IntentFilter appLockFilter = new IntentFilter();
        appLockFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appLockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        appLockFilter.addAction(Intent.ACTION_SCREEN_ON);
        appLockFilter.addAction(Intent.ACTION_USER_PRESENT);
        appLockFilter.addAction(AppLockingService.STOP_APP_LOCK_SERVICE);
        registerReceiver(appLockReceiver,appLockFilter);
    }

        void setWindowLayoutParams(){
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.TOP| Gravity.START;
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
         displayHeight = metrics.heightPixels;
    }

    private void loadLaunchers(){
        List<ResolveInfo> launcherInfo= getBaseContext().getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.GET_META_DATA);
        for (ResolveInfo info : launcherInfo ){
            launcherAppsList.add(info.activityInfo.packageName);
        }
    }

    private void scheduleAppQuery() {
        appLockQueryTask = new AppLockQueryTask(getApplicationContext(), this);
        appLockService = Executors.newScheduledThreadPool(3);
        appLockService.scheduleAtFixedRate(appLockQueryTask, 300, 300, TimeUnit.MILLISECONDS);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(596804950,getNotification());
        startService(new Intent(getBaseContext(),AppLockForegroundService.class)
                    .putExtra(AppLockForegroundService.FOREGROUND_SERVICE_TYPE,
                            AppLockForegroundService.APP_LOCK_SERVICE));
        isAppLockRunning = true;
        appLockMode = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                .getInt(AppLockModel.APP_LOCK_LOCKMODE,54);
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        isFingerPrintLockActive = prefs.getBoolean(LockUpSettingsActivity.FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,false);
        shouldLockOnScreenOn = prefs.getBoolean(LockUpSettingsActivity.LOCK_SCREEN_ON_PREFERENCE_KEY,false);
        isScreenOn = false;
        String checkedAppsColorJSONString = prefs.getString(AppLockModel.CHECKED_APPS_COLOR_SHARED_PREF_KEY,null);
        String checkedAppsJSONString = prefs.getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,null);
        String recommendedAppsJSONString = prefs.getString(AppLockModel.RECOMMENDED_APPS_SHARED_PREF_KEY,null);
        TreeMap<String,Integer> checkedAppsColor = gson.fromJson(checkedAppsColorJSONString,checkedAppsColorMapToken);
        TreeMap<String,String> checkedAppsMap = gson.fromJson(checkedAppsJSONString,checkedAppsMapToken);
        LinkedHashMap<String,HashMap<String,Boolean>> recommendedAppsMap= gson.fromJson(recommendedAppsJSONString,recommendedAppsMapToken);
        if(checkedAppsMap!=null){
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }
        if(checkedAppsColor!=null){
            checkedAppColorMap = checkedAppsColor;
        }
        if(recommendedAppsMap != null){
            recommendedAppsList.clear();
            for(Map.Entry<String,HashMap<String,Boolean>> item : recommendedAppsMap.entrySet()){
                ArrayList<Boolean> tempList = new ArrayList<>(item.getValue().values());
                if(tempList.get(0)){
                    recommendedAppsList.add(item.getKey());
                }
            }
        }
        if(checkedAppsMap!=null && checkedAppsMap.isEmpty()){
            if(recommendedAppsMap !=null && recommendedAppsList.isEmpty()){
                stopAppLock = true;
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
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

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == RECENT_APP_INFO_V21_UP){
            String[] checkedAppPackage = (String[]) msg.obj;
            synchronized (this) {
                if(isScreenOn && checkedAppPackage[0]!=null){
                    if(!checkedAppsList.contains(checkedAppPackage[0]) && !recommendedAppsList.contains(checkedAppPackage[0])){
                        recentlyLockedApp = NIL_APPS_LOCKED;
                        isScreenOn = false;
                        return true;
                    }
                }
                if (checkedAppsList.contains(checkedAppPackage[0]) || recommendedAppsList.contains(checkedAppPackage[0])) {
                    if (!checkedAppPackage[0].equals(recentlyLockedApp)) {
                        removeView();
                        recentlyLockedApp = checkedAppPackage[0];
                        if (appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (Settings.canDrawOverlays(getBaseContext())) {
                                    FingerPrintActivity.updateService(this);
                                    patternLockViewFinger = new LockPatternViewFinger(getBaseContext(), this, this, isFingerPrintLockActive);
                                    patternLockViewFinger.setPackageName(checkedAppPackage[0]);
                                    patternLockViewFinger.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                                    windowManager.addView(patternLockViewFinger, params);
                                    startActivity(new Intent(getBaseContext(), FingerPrintActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                } else {
                                    String permissionMessage = getResources().getString(R.string.appLock_activity_usage_dialog_overlay_permission_request);
                                    Toast.makeText(getBaseContext(), permissionMessage, Toast.LENGTH_LONG).show();
                                    return true;
                                }
                            } else {
                                patternLockView = new LockPatternView(getBaseContext(), this);
                                patternLockView.setPackageName(checkedAppPackage[0]);
                                patternLockView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                                windowManager.addView(patternLockView, params);
                            }

                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (Settings.canDrawOverlays(getBaseContext())) {
                                    FingerPrintActivity.updateService(this);
                                    lockPinViewFinger = new LockPinViewFinger(getBaseContext(), this, this, isFingerPrintLockActive);
                                    lockPinViewFinger.setPackageName(checkedAppPackage[0]);
                                    lockPinViewFinger.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                                    windowManager.addView(lockPinViewFinger, params);
                                    startActivity(new Intent(getBaseContext(), FingerPrintActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                } else {
                                    String permissionMessage = getResources().getString(R.string.appLock_activity_usage_dialog_overlay_permission_request);
                                    Toast.makeText(getBaseContext(), permissionMessage, Toast.LENGTH_LONG).show();
                                    return true;
                                }
                            } else {
                                lockPinView = new LockPinView(getBaseContext(), this);
                                lockPinView.setPackageName(checkedAppPackage[0]);
                                lockPinView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                                windowManager.addView(lockPinView, params);
                            }
                        }
                    }
                } else if (launcherAppsList.contains(checkedAppPackage[0]) || systemUIApp.equals(checkedAppPackage[0])
                        || recentlyLockedApp.equals(checkedAppPackage[1])) {
                    if(checkedAppPackage[0]!=null) {
                        if (checkedAppPackage[0].equals(getPackageName())) {
                            return true;
                        }
                        recentlyLockedApp = NIL_APPS_LOCKED;
                        removeView();
                    }
                }
            }
            return true;
        }
        if(msg.what == RECENT_APP_INFO_V21_DOWN){
            String checkedAppPackage = (String) msg.obj;
            synchronized (this) {
                if (checkedAppsList.contains(checkedAppPackage) || recommendedAppsList.contains(checkedAppPackage)) {
                    if (!checkedAppPackage.equals(recentlyLockedApp)) {
                        recentlyLockedApp = checkedAppPackage;
                        if (appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN) {
                            if(patternLockView!=null){
                                removeView();
                            }
                            patternLockView = new LockPatternView(getBaseContext(), this);
                            patternLockView.setPackageName(checkedAppPackage);
                            patternLockView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage), displayHeight);
                            windowManager.addView(patternLockView, params);

                        } else {
                            if(lockPinView!=null){
                                removeView();
                            }
                            lockPinView = new LockPinView(getBaseContext(), this);
                            lockPinView.setPackageName(checkedAppPackage);
                            lockPinView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage), displayHeight);
                            windowManager.addView(lockPinView, params);
                        }
                    }

                } else {
                    recentlyLockedApp = NIL_APPS_LOCKED;
                    removeView();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onPinUnlocked() {
        removeView();
    }

    @Override
    public void onPinLocked(String packageName) {
        recentlyLockedApp = packageName;
    }

    void removeView(){
        if(windowManager!=null && lockPinView != null) {
            windowManager.removeView(lockPinView);
            lockPinView.removeView();
            lockPinView = null;
            return;
        }
        if(windowManager!=null && lockPinViewFinger != null) {
            windowManager.removeView(lockPinViewFinger);
            lockPinViewFinger.removeView();
            lockPinViewFinger = null;
            return;
        }
        if(windowManager!=null && patternLockView != null) {
            windowManager.removeView(patternLockView);
            patternLockView.removeView();
            patternLockView = null;
        }
        if(windowManager!=null && patternLockViewFinger != null) {
            windowManager.removeView(patternLockViewFinger);
            patternLockViewFinger.removeView();
            patternLockViewFinger = null;
        }
    }

    @Override
    public void onFingerScannerCanceled() {
        updateFingerScanner();
    }

    private final class AppLockReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_PACKAGE_ADDED)){
                loadLaunchers();
            }
            if(action.equals(Intent.ACTION_SCREEN_OFF)){
                if(!appLockService.isShutdown()){
                    appLockService.shutdown();
                }
            }
            if(action.equals(Intent.ACTION_SCREEN_ON)){
                if(!shouldLockOnScreenOn) {
                    isScreenOn = true;
                }
                scheduleAppQuery();
            }
            if(action.equals(Intent.ACTION_USER_PRESENT)){
                if(shouldLockOnScreenOn){
                    recentlyLockedApp = NIL_APPS_LOCKED;
                }
            }
            if(action.equals(AppLockingService.STOP_APP_LOCK_SERVICE)){
                stopAppLock = true;
                stopForeground(true);
                stopSelf();
            }
        }
    }

    void updateFingerScanner(){
        FingerPrintActivity.updateService(this);
        startActivity(new Intent(getBaseContext(),FingerPrintActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public void updateCancelSignal(){
        if((Build.VERSION.SDK_INT>= Build.VERSION_CODES.M )&& isFingerPrintLockActive){
            if(appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN && patternLockViewFinger!=null){
                patternLockViewFinger.updateCancelSignal();
            }else
            if(appLockMode == AppLockModel.APP_LOCK_MODE_PIN && lockPinViewFinger!=null)  {
                lockPinViewFinger.updateCancelSignal();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!appLockService.isShutdown()){
            appLockService.shutdown();
            appLockQueryTask= null;
        }
        unregisterReceiver(appLockReceiver);
        appLockReceiver = null;
        isAppLockRunning = false;
        if(!stopAppLock) {
            sendBroadcast(new Intent(AppLockServiceRestartReceiver.ACTION_LOCK_SERVICE_RESTART));
        }
        Log.d(TAG,"Service Destroyed");
    }

    /**
     * Created by RAAJA on 15-09-2016.
     */

}

package com.smartfoxitsolutions.lockup.services;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.FingerPrintActivity;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.UserLoyaltyReport;
import com.smartfoxitsolutions.lockup.receivers.AppLockServiceRestartReceiver;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusModel;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class AppLockingService extends Service implements Handler.Callback,OnPinLockUnlockListener, OnFingerScannerCancelListener
                ,MoPubNative.MoPubNativeNetworkListener{
    public static final String TAG = "AppLockingService";

    public static final String STOP_APP_LOCK_SERVICE = "stopAppLockService";
    private static final int UNLOCK_LOCKED_APP = 5;

    public static boolean isAppLockRunning = false;
    public static final int RECENT_APP_INFO_V21_DOWN =2;
    public static final int RECENT_APP_INFO_V21_UP =4;

    public static String recentlyLockedApp = "NIL";
    public static final String NIL_APPS_LOCKED = "NIL";

    private ScheduledExecutorService appLockService;
    private AppLockQueryTask appLockQueryTask;
    private Gson gson;
    private Type checkedAppsMapToken, checkedAppsColorMapToken, recommendedAppsMapToken;
    private Type userReportMapToken;
    private ArrayList<String> checkedAppsList, recommendedAppsList;
    private AppLockReceiver appLockReceiver;
    private int appLockMode;
    private long adRefreshInterval;
    private TreeMap<String,Integer> checkedAppColorMap;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LockPinView lockPinView;
    private LockPinViewFinger lockPinViewFinger;
    private LockPatternView patternLockView;
    private LockPatternViewFinger patternLockViewFinger;
    private boolean isFingerPrintLockActive, hasLockDisplayed ,isUserLoggedIn;
    private boolean stopAppLock, shouldLockOnScreenOn, isScreenOn, hasAdTracked,hasAdFailed, hasAdRequestComplete;
    private MoPubNative moPubNative;
    private NativeAd moPubNativeAd;
    private View adView;
    private UserLoyaltyReport userLoyaltyReport;
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
        windowManager = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);
        setWindowLayoutParams();
        checkedAppsList = new ArrayList<>();
        recommendedAppsList = new ArrayList<>();
        checkedAppsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
        checkedAppsColorMapToken = new TypeToken<TreeMap<String,Integer>>(){}.getType();
        recommendedAppsMapToken = new TypeToken<LinkedHashMap<String,HashMap<String,Boolean>>>(){}.getType();
        userReportMapToken = new TypeToken<LinkedHashMap<String,UserLoyaltyReport>>(){}.getType();
        checkedAppColorMap = new TreeMap<>();
        hasAdTracked = true;
        hasAdRequestComplete = false;
        requestAd();
        scheduleAppQuery();
        appLockReceiver = new AppLockReceiver();
        IntentFilter appLockFilter = new IntentFilter();
        appLockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        appLockFilter.addAction(Intent.ACTION_SCREEN_ON);
        appLockFilter.addAction(AppLockingService.STOP_APP_LOCK_SERVICE);
        registerReceiver(appLockReceiver,appLockFilter);

    }

        void setWindowLayoutParams(){
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.TOP| Gravity.START;
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
         displayHeight = metrics.heightPixels;
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
        isScreenOn = false;
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        appLockMode = prefs.getInt(AppLockModel.APP_LOCK_LOCKMODE,54);
        isFingerPrintLockActive = prefs.getBoolean(LockUpSettingsActivity.FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,false);
        shouldLockOnScreenOn = prefs.getBoolean(LockUpSettingsActivity.LOCK_SCREEN_ON_PREFERENCE_KEY,false);
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
        SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
        String userReportString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT,null);
        LinkedHashMap<String,UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportString, userReportMapToken);
        if(userLoyaltyReportMap!=null && !userLoyaltyReportMap.isEmpty()){
            ArrayList<String> dateKeyList = new ArrayList<>(userLoyaltyReportMap.keySet());
            String dateKey = dateKeyList.get(dateKeyList.size()-1);
            userLoyaltyReport = userLoyaltyReportMap.get(dateKey);
        }
        isUserLoggedIn = loyaltyPrefs.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
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

    void requestAd(){
        moPubNative = new MoPubNative(this
                ,getResources().getString(R.string.pin_lock_activity_ad_unit_id),this);

        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.lock_native_ad)
                .mainImageId(R.id.native_ad_main_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .callToActionId(R.id.native_ad_call_to_action)
                .build();

        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        moPubNative.registerAdRenderer(adRenderer);
        moPubNative.makeRequest();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == RECENT_APP_INFO_V21_UP){
            String[] checkedAppPackage = (String[]) msg.obj;
            if(isScreenOn){
               // checkedAppPackage[0] = NIL_APPS_LOCKED;
                //checkedAppPackage[1] = NIL_APPS_LOCKED;
                isScreenOn=false;
                return true;
            }
            if (hasAdTracked && hasAdRequestComplete) {
                if(hasAdFailed && (System.currentTimeMillis() > adRefreshInterval)) {
                    hasAdRequestComplete = false;
                    requestAd();
                }
                if(!hasAdFailed){
                    hasAdRequestComplete = false;
                    requestAd();
                }
            }

            if(checkedAppPackage[0]!=null && checkedAppPackage[1]!=null) {
                if (!checkedAppsList.contains(checkedAppPackage[0]) && !recommendedAppsList.contains(checkedAppPackage[0])) {
                    if(checkedAppPackage[0].equals(checkedAppPackage[1])) {
                        recentlyLockedApp = NIL_APPS_LOCKED;
                        Log.d("AppLock",checkedAppPackage[0] + " FOREGROUND " + checkedAppPackage[1] + " BACKGROUND ");
                        if(hasLockDisplayed){
                            removeView();
                        }
                        return true;
                    }
                }
                    lockUnlockApp_V21_UP(checkedAppPackage);
            }
            return true;
        }
        if(msg.what == RECENT_APP_INFO_V21_DOWN){
            String checkedAppPackage = (String) msg.obj;
            if (hasAdTracked && hasAdRequestComplete) {
                if(hasAdFailed && (System.currentTimeMillis() > adRefreshInterval)) {
                    hasAdRequestComplete = false;
                    requestAd();
                }
                if(!hasAdFailed){
                    hasAdRequestComplete = false;
                    requestAd();
                }
            }
            if(checkedAppPackage!=null) {
                lockUnlockApp_V21_Down(checkedAppPackage);
            }
            return true;
        }

        if(msg.what == UNLOCK_LOCKED_APP){
            removeView();
            hasLockDisplayed = false;
            Log.d("AppLock",System.currentTimeMillis() + " Unlocked Time");
            return true;
        }
        return false;
    }

    private void lockUnlockApp_V21_UP(String[] checkedAppPackage){
        synchronized (this) {
            if (checkedAppsList.contains(checkedAppPackage[0]) || recommendedAppsList.contains(checkedAppPackage[0])) {
                if (!recentlyLockedApp.equals(checkedAppPackage[0])) {
                    removeView();
                    recentlyLockedApp = checkedAppPackage[0];
                    if (appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Settings.canDrawOverlays(getBaseContext())) {
                                patternLockViewFinger = new LockPatternViewFinger(getBaseContext(), this, this, isFingerPrintLockActive);
                                patternLockViewFinger.setPackageName(checkedAppPackage[0]);
                                patternLockViewFinger.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                                patternLockViewFinger.addRenderedAd(adView, moPubNativeAd);
                                windowManager.addView(patternLockViewFinger, params);
                                hasLockDisplayed = true;
                                if (isFingerPrintLockActive) {
                                    FingerPrintActivity.updateService(this);
                                    startActivity(new Intent(getBaseContext(), FingerPrintActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }
                            } else {
                                String permissionMessage = getResources().getString(R.string.appLock_activity_usage_dialog_overlay_permission_request);
                                Toast.makeText(getBaseContext(), permissionMessage, Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else {
                            patternLockView = new LockPatternView(getBaseContext(), this);
                            patternLockView.setPackageName(checkedAppPackage[0]);
                            patternLockView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                            patternLockView.addRenderedAd(adView, moPubNativeAd);
                            windowManager.addView(patternLockView, params);
                            hasLockDisplayed = true;

                        }

                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Settings.canDrawOverlays(getBaseContext())) {
                                lockPinViewFinger = new LockPinViewFinger(getBaseContext(), this, this, isFingerPrintLockActive);
                                lockPinViewFinger.setPackageName(checkedAppPackage[0]);
                                lockPinViewFinger.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                                lockPinViewFinger.addRenderedAd(adView, moPubNativeAd);
                                windowManager.addView(lockPinViewFinger, params);
                                hasLockDisplayed = true;
                                if (isFingerPrintLockActive) {
                                    FingerPrintActivity.updateService(this);
                                    startActivity(new Intent(getBaseContext(), FingerPrintActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }
                            } else {
                                String permissionMessage = getResources().getString(R.string.appLock_activity_usage_dialog_overlay_permission_request);
                                Toast.makeText(getBaseContext(), permissionMessage, Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else {
                            lockPinView = new LockPinView(getBaseContext(), this);
                            lockPinView.setPackageName(checkedAppPackage[0]);
                            lockPinView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                            lockPinView.addRenderedAd(adView, moPubNativeAd);
                            windowManager.addView(lockPinView, params);
                            hasLockDisplayed = true;
                        }
                    }
                    Log.d("AppLock",System.currentTimeMillis() + " Locked Time");

                }
            } else if (recentlyLockedApp.equals(checkedAppPackage[1])) {
                if (checkedAppPackage[0].equals(getPackageName())) {
                    return;
                }
                recentlyLockedApp = NIL_APPS_LOCKED;
                removeView();
                hasLockDisplayed = false;

            }
        }
    }

    private void lockUnlockApp_V21_Down(String checkedAppPackage){
        synchronized (this) {
            if (checkedAppsList.contains(checkedAppPackage) || recommendedAppsList.contains(checkedAppPackage)) {
                if (!checkedAppPackage.equals(recentlyLockedApp)) {
                    recentlyLockedApp = checkedAppPackage;
                    removeView();
                    if (appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN) {
                        patternLockView = new LockPatternView(getBaseContext(), this);
                        patternLockView.setPackageName(checkedAppPackage);
                        patternLockView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage), displayHeight);
                        patternLockView.addRenderedAd(adView, moPubNativeAd);
                        windowManager.addView(patternLockView, params);
                        hasLockDisplayed = true;

                    } else {
                        lockPinView = new LockPinView(getBaseContext(), this);
                        lockPinView.setPackageName(checkedAppPackage);
                        lockPinView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage), displayHeight);
                        lockPinView.addRenderedAd(adView, moPubNativeAd);
                        windowManager.addView(lockPinView, params);
                        hasLockDisplayed = true;
                    }
                }

            } else {
                recentlyLockedApp = NIL_APPS_LOCKED;
                removeView();
                hasLockDisplayed = false;
            }
        }
    }

    @Override
    public void onPinUnlocked() {
        Handler mainHandler = new Handler(this);
        mainHandler.sendEmptyMessageDelayed(UNLOCK_LOCKED_APP,600);
    }

    @Override
    public void onPinLocked(String packageName) {
        recentlyLockedApp = packageName;
    }

    @Override
    public void onAdImpressed() {
        if(isUserLoggedIn && userLoyaltyReport!=null) {
            userLoyaltyReport.setTotalImpression(Integer.parseInt(userLoyaltyReport.getTotalImpression()) + 1);
            SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
            String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT,null);
            LinkedHashMap<String,UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
            if(userLoyaltyReportMap!=null && !userLoyaltyReportMap.isEmpty()){
                userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(),userLoyaltyReport);
            }
            SharedPreferences.Editor edit = loyaltyPrefs.edit();
            String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
            edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT,userReportUpdateString);
            edit.apply();
        }
        hasAdTracked = true;
    }

    @Override
    public void onAdClicked() {
        if(isUserLoggedIn && userLoyaltyReport!=null) {
            userLoyaltyReport.setTotalClicked(Integer.parseInt(userLoyaltyReport.getTotalClicked()) + 1);
            SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
            String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT,null);
            LinkedHashMap<String,UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
            if(userLoyaltyReportMap!=null && !userLoyaltyReportMap.isEmpty()){
                userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(),userLoyaltyReport);
            }
            SharedPreferences.Editor edit = loyaltyPrefs.edit();
            String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
            edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT,userReportUpdateString);
            edit.apply();
        }
        hasAdTracked = true;
    }

    void removeView(){
        if(windowManager!=null && lockPinView != null) {
            windowManager.removeView(lockPinView);
            lockPinView.removeView();
            lockPinView = null;
            return;
        }
        if(windowManager!=null && lockPinViewFinger != null) {
            lockPinViewFinger.removeView();
            windowManager.removeView(lockPinViewFinger);
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

    @Override
    public void onNativeLoad(NativeAd nativeAd) {
        Log.d("LockUpMopub","Called onNativeLoad Finger");
        moPubNativeAd = null;
        moPubNativeAd = nativeAd;
        View adViewRender = moPubNativeAd.createAdView(this, null);
        adView = null;
        adView = adViewRender;
        hasAdFailed = false;
        hasAdTracked = false;
        hasAdRequestComplete = true;
    }

    @Override
    public void onNativeFail(NativeErrorCode errorCode) {
        Log.d("LockUpMopub",errorCode+ " errorcode");
        hasAdFailed = true;
        adRefreshInterval = System.currentTimeMillis() + 120000;
        hasAdRequestComplete = true;
    }

    private final class AppLockReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(Intent.ACTION_SCREEN_OFF)){
                if(!appLockService.isShutdown()){
                    appLockService.shutdown();
                }
            }
            if(action.equals(Intent.ACTION_SCREEN_ON)){
                if(hasLockDisplayed){
                    removeView();
                    recentlyLockedApp = NIL_APPS_LOCKED;
                }
                if(shouldLockOnScreenOn){
                    recentlyLockedApp = NIL_APPS_LOCKED;
                    isScreenOn = true;
                }
                scheduleAppQuery();
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
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(getBaseContext()).trimMemory(level);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!appLockService.isShutdown()){
            appLockService.shutdown();
            appLockQueryTask= null;
        }
        if(moPubNative!=null){
            moPubNative.destroy();
        }
        unregisterReceiver(appLockReceiver);
        appLockReceiver = null;
        isAppLockRunning = false;
        if(!stopAppLock) {
            sendBroadcast(new Intent(AppLockServiceRestartReceiver.ACTION_LOCK_SERVICE_RESTART));
        }
        Log.d(TAG,"Service Destroyed");
    }

}

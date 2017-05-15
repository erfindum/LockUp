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
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;
import com.smartfoxitsolutions.lockup.AppLoaderActivity;
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
import com.startapp.android.publish.adsCommon.StartAppSDK;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class AppLockingService extends Service implements Handler.Callback, OnPinLockUnlockListener, OnFingerScannerCancelListener {
    public static final String TAG = "AppLockingService";

    public static final String STOP_APP_LOCK_SERVICE = "stopAppLockService";
    private static final int UNLOCK_LOCKED_APP = 5;
    private static final int SHOW_INTERSTITIAL = 6;


    public static boolean isAppLockRunning = false;
    public static final int RECENT_APP_INFO_V21_DOWN = 2;
    public static final int RECENT_APP_INFO_V21_UP = 4;

    public static String recentlyLockedApp = "NIL";
    public static final String NIL_APPS_LOCKED = "NIL";

    private ScheduledExecutorService appLockService;
    private AppLockQueryTask appLockQueryTask;
    private Gson gson;
    private Type checkedAppsMapToken, checkedAppsColorMapToken, recommendedAppsMapToken;
    private Type userReportMapToken;
    private ArrayList<String> checkedAppsList, recommendedAppsList;
    private AppLockReceiver appLockReceiver;
    private ConcurrentLinkedQueue<View> adViewQueue;
    private int appLockMode, interstitialDisplayCounter = 0;
    private TreeMap<String, Integer> checkedAppColorMap;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LockPinView lockPinView;
    private LockPinViewFinger lockPinViewFinger;
    private LockPatternView patternLockView;
    private LockPatternViewFinger patternLockViewFinger;
    private boolean isFingerPrintLockActive, hasLockDisplayed, isUserLoggedIn;
    private boolean stopAppLock, shouldLockOnScreenOn, isScreenOn, isAdClicked;
    private boolean hasAdOneFailed, hasAdOneRequestComplete,hasAdTwoFailed, hasAdTwoRequestComplete, isInterstitialAdFailed,
                     shouldShowInterstitial,shouldDisplayInterstitial;
    private MoPubNative moPubNativeOne, moPubNativeTwo;
    private UserLoyaltyReport userLoyaltyReport;
    int displayHeight;
    private InterstitialAd interstitialAd;
    private long adOneTimer, adTwoTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AppLockService", "Called Service OnCreate" + System.currentTimeMillis());
        gson = new Gson();
        windowManager = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);
        setWindowLayoutParams();
        checkedAppsList = new ArrayList<>();
        recommendedAppsList = new ArrayList<>();
        checkedAppsMapToken = new TypeToken<TreeMap<String, String>>() {
        }.getType();
        checkedAppsColorMapToken = new TypeToken<TreeMap<String, Integer>>() {
        }.getType();
        recommendedAppsMapToken = new TypeToken<LinkedHashMap<String, HashMap<String, Boolean>>>() {
        }.getType();
        userReportMapToken = new TypeToken<LinkedHashMap<String, UserLoyaltyReport>>() {
        }.getType();
        checkedAppColorMap = new TreeMap<>();
        hasAdOneRequestComplete = false;
        hasAdTwoRequestComplete = false;
        adViewQueue = new ConcurrentLinkedQueue<>();
        requestFirstAd();
        requestSecondAd();
        setupInterstitialAd();
        scheduleAppQuery();
        appLockReceiver = new AppLockReceiver();
        IntentFilter appLockFilter = new IntentFilter();
        appLockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        appLockFilter.addAction(Intent.ACTION_SCREEN_ON);
        appLockFilter.addAction(AppLockingService.STOP_APP_LOCK_SERVICE);
        registerReceiver(appLockReceiver, appLockFilter);
        //launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        StartAppSDK.init(this,"204970014",false);
    }

    void setWindowLayoutParams() {
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.TOP | Gravity.START;
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
        startForeground(596804950, getNotification());
        startService(new Intent(getBaseContext(), AppLockForegroundService.class)
                .putExtra(AppLockForegroundService.FOREGROUND_SERVICE_TYPE,
                        AppLockForegroundService.APP_LOCK_SERVICE));
        isAppLockRunning = true;
        isScreenOn = false;
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME, MODE_PRIVATE);
        appLockMode = prefs.getInt(AppLockModel.APP_LOCK_LOCKMODE, 54);
        isFingerPrintLockActive = prefs.getBoolean(LockUpSettingsActivity.FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY, false);
        shouldLockOnScreenOn = prefs.getBoolean(LockUpSettingsActivity.LOCK_SCREEN_ON_PREFERENCE_KEY, false);
        String checkedAppsColorJSONString = prefs.getString(AppLockModel.CHECKED_APPS_COLOR_SHARED_PREF_KEY, null);
        String checkedAppsJSONString = prefs.getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY, null);
        String recommendedAppsJSONString = prefs.getString(AppLockModel.RECOMMENDED_APPS_SHARED_PREF_KEY, null);
        TreeMap<String, Integer> checkedAppsColor = gson.fromJson(checkedAppsColorJSONString, checkedAppsColorMapToken);
        TreeMap<String, String> checkedAppsMap = gson.fromJson(checkedAppsJSONString, checkedAppsMapToken);
        LinkedHashMap<String, HashMap<String, Boolean>> recommendedAppsMap = gson.fromJson(recommendedAppsJSONString, recommendedAppsMapToken);
        if (checkedAppsMap != null) {
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }
        if (checkedAppsColor != null) {
            checkedAppColorMap = checkedAppsColor;
        }
        if (recommendedAppsMap != null) {
            recommendedAppsList.clear();
            for (Map.Entry<String, HashMap<String, Boolean>> item : recommendedAppsMap.entrySet()) {
                ArrayList<Boolean> tempList = new ArrayList<>(item.getValue().values());
                if (tempList.get(0)) {
                    recommendedAppsList.add(item.getKey());
                }
            }
        }
        if (checkedAppsMap != null && checkedAppsMap.isEmpty()) {
            if (recommendedAppsMap != null && recommendedAppsList.isEmpty()) {
                stopAppLock = true;
                stopForeground(true);
                stopSelf();
            }
        }
        SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
        String userReportString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
        LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportString, userReportMapToken);
        if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
            ArrayList<String> dateKeyList = new ArrayList<>(userLoyaltyReportMap.keySet());
            String dateKey = dateKeyList.get(dateKeyList.size() - 1);
            userLoyaltyReport = userLoyaltyReportMap.get(dateKey);
        }
        isUserLoggedIn = loyaltyPrefs.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY, false);
        return START_STICKY;
    }

    Notification getNotification() {
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
        notifBuilder.setContentTitle("AppLock Running");
        notifBuilder.setContentText("Touch to disable AppLock");
        notifBuilder.setOngoing(true)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#ffffff"));
        return notifBuilder.build();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == RECENT_APP_INFO_V21_UP) {
            String[] checkedAppPackage = (String[]) msg.obj;
            if (isScreenOn) {
                if (shouldLockOnScreenOn) {
                    checkedAppPackage[0] = NIL_APPS_LOCKED;
                    checkedAppPackage[1] = NIL_APPS_LOCKED;
                }
                if (hasLockDisplayed) {
                    removeView();
                    if (checkedAppPackage[0] == null) {
                        checkedAppPackage[0] = NIL_APPS_LOCKED;
                    }
                    if (checkedAppPackage[1] == null) {
                        checkedAppPackage[1] = NIL_APPS_LOCKED;
                    }
                    hasLockDisplayed = false;
                }
                isScreenOn = false;
            }

            if (checkedAppPackage[0] != null && checkedAppPackage[1] != null) {
                if (!checkedAppsList.contains(checkedAppPackage[0]) && !recommendedAppsList.contains(checkedAppPackage[0])) {
                    if (checkedAppPackage[0].equals(checkedAppPackage[1])) {
                        recentlyLockedApp = NIL_APPS_LOCKED;
                        if(isAdClicked) {
                            checkedAppPackage[0] = NIL_APPS_LOCKED;
                            checkedAppPackage[1] = NIL_APPS_LOCKED;
                            isAdClicked = false;
                        }
                        //Log.d("AppLock", checkedAppPackage[0] + " FOREGROUND " + checkedAppPackage[1] + " BACKGROUND ");
                    }
                }
                lockUnlockApp_V21_UP(checkedAppPackage);

            }
            return true;

        }
        if (msg.what == RECENT_APP_INFO_V21_DOWN) {
            String checkedAppPackage = (String) msg.obj;
            Log.d("AppLock",recentlyLockedApp + " recent "+checkedAppPackage);
            if (checkedAppPackage != null) {
                lockUnlockApp_V21_Down(checkedAppPackage);
            }
            return true;
        }

        if(msg.what == UNLOCK_LOCKED_APP){
            removeView();
            hasLockDisplayed = false;
            Log.d("AppLock", System.currentTimeMillis() + " Unlocked Time");
            return true;
        }

        if(msg.what == SHOW_INTERSTITIAL){
            interstitialAd.show();
            setupInterstitialAd();

        }
        return false;
    }

    private void lockUnlockApp_V21_UP(String[] checkedAppPackage) {
        Log.d("LockupAppLock", "Locked App : "+checkedAppPackage[0] +" : " + checkedAppPackage[1]);
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
                            patternLockViewFinger.addRenderedAd(adViewQueue.peek());
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
                        patternLockView.addRenderedAd(adViewQueue.peek());
                        windowManager.addView(patternLockView, params);
                        hasLockDisplayed = true;

                    }

                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(getBaseContext())) {
                            lockPinViewFinger = new LockPinViewFinger(getBaseContext(), this, this, isFingerPrintLockActive);
                            lockPinViewFinger.setPackageName(checkedAppPackage[0]);
                            lockPinViewFinger.setWindowBackground(checkedAppColorMap.get(checkedAppPackage[0]), displayHeight);
                            lockPinViewFinger.addRenderedAd(adViewQueue.peek());
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
                        lockPinView.addRenderedAd(adViewQueue.peek());
                        windowManager.addView(lockPinView, params);
                        hasLockDisplayed = true;
                    }
                }
                if (adViewQueue.isEmpty()) {
                    if (hasAdOneRequestComplete) {
                        hasAdOneRequestComplete = false;
                        requestFirstAd();
                    }
                    if (hasAdTwoRequestComplete) {
                        hasAdTwoRequestComplete = false;
                        requestSecondAd();
                    }
                } else {
                    View adView = adViewQueue.peek();
                    if (adView.getTag(R.id.AD_VIEW_TYPE).equals("one")
                            || hasAdOneFailed) {
                        if (hasAdOneRequestComplete) {
                            hasAdOneRequestComplete = false;
                            requestFirstAd();
                        }
                    }
                    if (adView.getTag(R.id.AD_VIEW_TYPE).equals("two")
                            || hasAdTwoFailed) {
                        if (hasAdTwoRequestComplete) {
                            hasAdTwoRequestComplete = false;
                            requestSecondAd();
                        }
                    }
                    Log.d("LockUpMopub", "Queuw Size --- " + adViewQueue.size());
                    adViewQueue.poll();
                    Log.d("LockUpMopub", "Native Ad ---- Removed");
                }
                Log.d("AppLock", System.currentTimeMillis() + " Locked Time");
                if (isInterstitialAdFailed && !interstitialAd.isLoading()) {
                    requestInterstitialAd();
                }
                interstitialDisplayCounter = interstitialDisplayCounter + 1;
                shouldShowInterstitial = true;

            }
        } else if (recentlyLockedApp.equals(checkedAppPackage[1])) {
            if (interstitialDisplayCounter > 9 && shouldShowInterstitial) {
                if (interstitialAd.isLoaded() && shouldDisplayInterstitial) {
                    Log.d("AdsLockup",interstitialAd.isLoaded() + " Loaded");
                    Handler mainHandler = new Handler(this);
                    mainHandler.sendEmptyMessageDelayed(SHOW_INTERSTITIAL,1500);
                    shouldDisplayInterstitial = false;
                }
                shouldShowInterstitial = false;
            }
            if (checkedAppPackage[0].equals(getPackageName())) {
                if (isAdClicked) {
                    removeLock();
                }
                isAdClicked = false;
                return;
            }
            removeLock();
        }

    }

    private void lockUnlockApp_V21_Down(String checkedAppPackage) {
            if (checkedAppsList.contains(checkedAppPackage) || recommendedAppsList.contains(checkedAppPackage)) {
                if (!recentlyLockedApp.equals(checkedAppPackage)) {
                    recentlyLockedApp = checkedAppPackage;
                    removeView();
                    if (appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN) {
                        patternLockView = new LockPatternView(getBaseContext(), this);
                        patternLockView.setPackageName(checkedAppPackage);
                        patternLockView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage), displayHeight);
                        patternLockView.addRenderedAd(adViewQueue.peek());
                        windowManager.addView(patternLockView, params);
                        hasLockDisplayed = true;

                    } else {
                        lockPinView = new LockPinView(getBaseContext(), this);
                        lockPinView.setPackageName(checkedAppPackage);
                        lockPinView.setWindowBackground(checkedAppColorMap.get(checkedAppPackage), displayHeight);
                        lockPinView.addRenderedAd(adViewQueue.peek());
                        windowManager.addView(lockPinView, params);
                        hasLockDisplayed = true;
                    }
                    if(adViewQueue.isEmpty()){
                        if(hasAdOneRequestComplete){
                            hasAdOneRequestComplete = false;
                            requestFirstAd();
                        }
                        if(hasAdTwoRequestComplete){
                            hasAdTwoRequestComplete = false;
                            requestSecondAd();
                        }
                    }else{
                        View adView = adViewQueue.peek();
                        if(adView.getTag(R.id.AD_VIEW_TYPE).equals("one")
                                || hasAdOneFailed){
                            if(hasAdOneRequestComplete) {
                                hasAdOneRequestComplete = false;
                                requestFirstAd();
                            }
                        }
                        if(adView.getTag(R.id.AD_VIEW_TYPE).equals("two")
                                || hasAdTwoFailed){
                            if(hasAdTwoRequestComplete){
                                hasAdTwoRequestComplete = false;
                                requestSecondAd();
                            }
                        }
                        Log.d("LockUpMopub", "Queuw Size --- " + adViewQueue.size());
                        adViewQueue.poll();
                        Log.d("LockUpMopub", "Native Ad ---- Removed");
                    }
                    Log.d("AppLock"," Locked and Removed "+ System.currentTimeMillis() + " "+checkedAppPackage.equals(recentlyLockedApp));
                    if(isInterstitialAdFailed && !interstitialAd.isLoading()) {
                        requestInterstitialAd();
                    }
                    interstitialDisplayCounter = interstitialDisplayCounter +1;
                    shouldShowInterstitial = true;
                }
            } else {
               removeLock();
                if (interstitialDisplayCounter > 9 && shouldShowInterstitial) {
                    if (interstitialAd.isLoaded() && shouldDisplayInterstitial) {
                        Log.d("AdsLockup",interstitialAd.isLoaded() + " Loaded");
                        Handler mainHandler = new Handler(this);
                        mainHandler.sendEmptyMessageDelayed(SHOW_INTERSTITIAL,1500);
                        shouldDisplayInterstitial = false;
                    }
                    shouldShowInterstitial = false;
                }
            }
    }

    @Override
    public void onPinUnlocked() {
        Handler mainHandler = new Handler(this);
        mainHandler.sendEmptyMessageDelayed(UNLOCK_LOCKED_APP, 600);
    }

    private void removeLock(){
        removeView();
        hasLockDisplayed = false;
        recentlyLockedApp = NIL_APPS_LOCKED;
    }

    @Override
    public void onPinLocked(String packageName) {
        recentlyLockedApp = packageName;
    }

    public synchronized void onAdImpressed() {
        if (isUserLoggedIn && userLoyaltyReport != null) {
            userLoyaltyReport.setTotalImpression(Integer.parseInt(userLoyaltyReport.getTotalImpression()) + 1);
            SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
            String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
            LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
            if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
                userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(), userLoyaltyReport);
            }
            SharedPreferences.Editor edit = loyaltyPrefs.edit();
            String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
            edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT, userReportUpdateString);
            edit.apply();
        }
        Log.d("LockUpMopub", "Native Ad  ---- Impressed");

    }

    public synchronized void onAdClicked() {
        if (isUserLoggedIn && userLoyaltyReport != null) {
            userLoyaltyReport.setTotalClicked(Integer.parseInt(userLoyaltyReport.getTotalClicked()) + 1);
            SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
            String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
            LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
            if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
                userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(), userLoyaltyReport);
            }
            SharedPreferences.Editor edit = loyaltyPrefs.edit();
            String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
            edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT, userReportUpdateString);
            edit.apply();
        }

        Log.d("LockUpMopub", "Native Ad  ---- Clicked");

    }

    void removeView() {
        if (windowManager != null && lockPinView != null) {
            lockPinView.removeView();
            windowManager.removeView(lockPinView);
            lockPinView = null;
            return;
        }
        if (windowManager != null && lockPinViewFinger != null) {
            lockPinViewFinger.removeView();
            windowManager.removeView(lockPinViewFinger);
            lockPinViewFinger = null;
            return;
        }
        if (windowManager != null && patternLockView != null) {
            patternLockView.removeView();
            windowManager.removeView(patternLockView);
            patternLockView = null;
        }
        if (windowManager != null && patternLockViewFinger != null) {
            patternLockViewFinger.removeView();
            windowManager.removeView(patternLockViewFinger);
            patternLockViewFinger = null;
        }
    }

    @Override
    public void onFingerScannerCanceled() {
        updateFingerScanner();
    }

    private void requestFirstAd() {
        if(System.currentTimeMillis()<adOneTimer){
            hasAdOneRequestComplete = true;
            return;
        }
        Log.d("LockUpMopub", "Native Ad One Request");
        moPubNativeOne = new MoPubNative(this
                , getResources().getString(R.string.lock_ad_unit_id_one), new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                Log.d("LockUpMopub", "Native Ad One Success");
                View adViewRender = nativeAd.createAdView(getBaseContext(), null);
                nativeAd.renderAdView(adViewRender);
                nativeAd.prepare(adViewRender);
                setAdTrackListener(nativeAd);
                adViewRender.setTag(R.id.AD_VIEW_TYPE, "one");
                adViewQueue.add(adViewRender);
                hasAdOneFailed = false;
                hasAdOneRequestComplete = true;
                adOneTimer = System.currentTimeMillis()+120000;
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                Log.d("LockUpMopub", errorCode + " errorcode -  Native Ad One");
                hasAdOneFailed = true;
                hasAdOneRequestComplete = true;
                adOneTimer = System.currentTimeMillis()+60000;
            }
        });

        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.lock_native_ad)
                .mainImageId(R.id.native_ad_main_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .callToActionId(R.id.native_ad_call_to_action)
                .privacyInformationIconImageId(R.id.native_ad_daa_icon_image)
                .build();

        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        moPubNativeOne.registerAdRenderer(adRenderer);
        moPubNativeOne.makeRequest();
    }

    private void requestSecondAd() {
        if(System.currentTimeMillis()<adTwoTimer){
            hasAdTwoRequestComplete = true;
            return;
        }
        Log.d("LockUpMopub", "Native Ad Two Request");
        moPubNativeTwo = new MoPubNative(this
                , getResources().getString(R.string.lock_ad_unit_id_two), new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                Log.d("LockUpMopub", "Native Ad Two Success");
                View adViewRender = nativeAd.createAdView(getBaseContext(), null);
                nativeAd.renderAdView(adViewRender);
                nativeAd.prepare(adViewRender);
                setAdTrackListener(nativeAd);
                adViewRender.setTag(R.id.AD_VIEW_TYPE, "two");
                adViewQueue.add(adViewRender);
                hasAdTwoFailed = false;
                hasAdTwoRequestComplete = true;
                adTwoTimer = System.currentTimeMillis()+120000;
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                Log.d("LockUpMopub", errorCode + " errorcode -  Native Ad Two");
                hasAdTwoFailed = true;
                hasAdTwoRequestComplete = true;
                adTwoTimer = System.currentTimeMillis()+60000;

            }
        });

        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.lock_native_ad)
                .mainImageId(R.id.native_ad_main_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .callToActionId(R.id.native_ad_call_to_action)
                .privacyInformationIconImageId(R.id.native_ad_daa_icon_image)
                .build();

        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        moPubNativeTwo.registerAdRenderer(adRenderer);
        moPubNativeTwo.makeRequest();
    }

    private void setAdTrackListener(NativeAd nativeAd) {
        nativeAd.setMoPubNativeEventListener(new NativeAd.MoPubNativeEventListener() {
            @Override
            public void onImpression(View view) {
                onAdImpressed();
            }

            @Override
            public void onClick(View view) {
                onAdClicked();
            }
        });
    }

    private void setupInterstitialAd(){
        interstitialAd = new InterstitialAd(getBaseContext());
        interstitialAd.setAdUnitId(getString(R.string.lock_interstitial_ad));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d("LockUpAdmob","Ad has been closed");
                //requestInterstitialAd();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                Log.d("LockUpAdmob","Ad failed : " + i);
                isInterstitialAdFailed = true;
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                if (isUserLoggedIn && userLoyaltyReport != null) {
                    userLoyaltyReport.setTotalClicked2(Integer.parseInt(userLoyaltyReport.getTotalClicked2()) + 1);
                    SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
                    String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
                    LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
                    if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
                        userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(), userLoyaltyReport);
                    }
                    SharedPreferences.Editor edit = loyaltyPrefs.edit();
                    String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
                    edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT, userReportUpdateString);
                    edit.apply();
                }
                Log.d("LockUpAdmob", "Interstitial Ad  ---- Clicked");
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                if (isUserLoggedIn && userLoyaltyReport != null) {
                    userLoyaltyReport.setTotalImpression2(Integer.parseInt(userLoyaltyReport.getTotalImpression2()) + 1);
                    SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
                    String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
                    LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
                    if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
                        userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(), userLoyaltyReport);
                    }
                    SharedPreferences.Editor edit = loyaltyPrefs.edit();
                    String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
                    edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT, userReportUpdateString);
                    edit.apply();
                }
                Log.d("LockUpAdmob","Interstitial Ad  ---- Impressed");
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d("LockUpAdmob","Ad has been loaded");
                interstitialDisplayCounter =0;
                isInterstitialAdFailed = false;
                shouldDisplayInterstitial = true;
            }
        });

        requestInterstitialAd();
    }

    private void requestInterstitialAd(){
        AdRequest adRequest = new AdRequest.Builder()//.addTestDevice("A04B6DE7DDFED7231620C0AA9BCF67EC")
                //.addTestDevice("C4BA58CBC40E4ECD07EC147C825FB9D4")
                .build();
        interstitialAd.loadAd(adRequest);
    }

    private final class AppLockReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (!appLockService.isShutdown()) {
                    appLockService.shutdown();
                }
            }
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (hasLockDisplayed || shouldLockOnScreenOn) {
                    recentlyLockedApp = NIL_APPS_LOCKED;
                }
                isScreenOn = true;
                scheduleAppQuery();
            }
            if (action.equals(AppLockingService.STOP_APP_LOCK_SERVICE)) {
                stopAppLock = true;
                stopForeground(true);
                stopSelf();
            }
        }
    }

    void updateFingerScanner() {
        FingerPrintActivity.updateService(this);
        startActivity(new Intent(getBaseContext(), FingerPrintActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public void updateCancelSignal() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && isFingerPrintLockActive) {
            if (appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN && patternLockViewFinger != null) {
                patternLockViewFinger.updateCancelSignal();
            } else if (appLockMode == AppLockModel.APP_LOCK_MODE_PIN && lockPinViewFinger != null) {
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
        if (!appLockService.isShutdown()) {
            appLockService.shutdown();
            appLockQueryTask = null;
        }
        if (moPubNativeOne != null) {
            moPubNativeOne.destroy();
        }
        if (moPubNativeTwo != null) {
            moPubNativeTwo.destroy();
        }
        unregisterReceiver(appLockReceiver);
        appLockReceiver = null;
        isAppLockRunning = false;
        if (!stopAppLock) {
            sendBroadcast(new Intent(AppLockServiceRestartReceiver.ACTION_LOCK_SERVICE_RESTART));
        }
        Log.d(TAG, "Service Destroyed");
    }

}

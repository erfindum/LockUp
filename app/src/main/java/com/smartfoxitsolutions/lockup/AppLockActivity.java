package com.smartfoxitsolutions.lockup;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockActivity extends AppCompatActivity {
    private static final String TAG = "AppLockActivity ";

    public static final int USAGE_ACCESS_PERMISSION_REQUEST=3;
    public static final int USAGE_ACCESS_PERMISSION_REQUEST_LINEAR=4;
    public static final int OVERLAY_PERMISSION_REQUEST = 5;
    public static final int OVERLAY_PERMISSION_REQUEST_LINEAR = 7;
    public static final int NOTIFICATION_PERMISSION_REQUEST = 6;
    private static final String USAGE_ACCESS_DIALOG_TAG = "usageAccessPermissionDialog";
    private static final String OVERLAY_ACCESS_DIALOG_TAG = "overlay_permission_dialog";
    private static final String NOTIFICATION_ACCESS_DIALOG_TAG = "notification_permission_dialog";
    private static final String RECOMMENDED_APPS_ALERT_DIALOG_TAG = "recommended_apps_alert_dialog";
    private static final String START_APP_LOG_DIALOG_TAG = "start_app_lock_dialog";
    public static final String APP_LOCK_FIRST_START_PREFERENCE_KEY = "app_lock_first_start";

    static boolean shouldStartAppLock;

    Toolbar appLockActivityToolbar;
    RecyclerView appLockRecyclerView;
    AppLockRecyclerAdapter appLockRecyclerAdapter;
    private AppLockModel appLockModel;
    private static boolean usagePermissionGranted,overlayPermissionGranted;
    private DialogFragment notificationPermissionDialog,overlayPermissionDialog,usageDialog;
    private SharedPreferences prefs;
    private NotificationMapUpdateReceiver notifUpdateReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_lock_activity);
        appLockModel = new AppLockModel(this.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE));
        appLockRecyclerView = (RecyclerView) findViewById(R.id.app_lock_activity_recycler_view);
        appLockActivityToolbar = (Toolbar) findViewById(R.id.app_lock_activity_tool_bar);
        notifUpdateReceiver = new NotificationMapUpdateReceiver(new WeakReference<>(this));
        setSupportActionBar(appLockActivityToolbar);
        appLockActivityToolbar.setTitleTextColor(Color.WHITE);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.appLock_activity_title);
        }
        checkAndSetUsagePermissions(USAGE_ACCESS_PERMISSION_REQUEST);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            overlayPermissionGranted = true;
        }
        calculateMarginHeader();
        displayRecyclerView();
        prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME, MODE_PRIVATE);
        boolean isAppLockFirstStart = prefs.getBoolean(APP_LOCK_FIRST_START_PREFERENCE_KEY,true);
        if(isAppLockFirstStart){
            shouldStartAppLock = true;
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(APP_LOCK_FIRST_START_PREFERENCE_KEY,false);
            edit.apply();
        }
        else{
           shouldStartAppLock = prefs.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        }

    }

    private void setUsageAccessPermissionGranted(boolean granted){
        usagePermissionGranted = granted;
    }

    private void setOverlayPermissionGranted(boolean isGranted){
        overlayPermissionGranted = isGranted;
    }

    static boolean getUsageAccessPermissionGranted(){
        return usagePermissionGranted;
    }

    static boolean getOverlayPermissionGranted(){
        return overlayPermissionGranted;
    }



    private void displayRecyclerView(){
        appLockRecyclerAdapter = new AppLockRecyclerAdapter(appLockModel, this);
        appLockRecyclerView.setAdapter(appLockRecyclerAdapter);
        appLockRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.VERTICAL,false));
    }

    void calculateMarginHeader(){
       DisplayMetrics metrices =  getResources().getDisplayMetrics();
        AppLockRecyclerAdapter.HEADER_MARGIN_SIZE_TEN = 10 * (metrices.densityDpi/DisplayMetrics.DENSITY_DEFAULT);
        AppLockRecyclerAdapter.HEADER_MARGIN_SIZE_FIFTEEN = 15 * (metrices.densityDpi/DisplayMetrics.DENSITY_DEFAULT);

    }

    void startUsagePermissionDialog(){
        usageDialog = new GrantUsageAccessDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(USAGE_ACCESS_DIALOG_TAG);
        usageDialog.show(fragmentTransaction,USAGE_ACCESS_DIALOG_TAG);
    }

    void startOverlayPermissionDialog(){
        overlayPermissionDialog = new OverlayPermissionDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(OVERLAY_ACCESS_DIALOG_TAG);
        overlayPermissionDialog.show(fragmentTransaction,OVERLAY_ACCESS_DIALOG_TAG);
    }

    void startNotificationPermissionDialog(){
        if(!NotificationLockService.isNotificationServiceConnected) {
            notificationPermissionDialog = new NotificationPermissionDialog();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack(NOTIFICATION_ACCESS_DIALOG_TAG);
            notificationPermissionDialog.show(fragmentTransaction, NOTIFICATION_ACCESS_DIALOG_TAG);
        }
    }

    void startRecommendedAlertDialog(AppLockRecyclerViewItem itemView,int position){
        RecommendedAppsAlertDialog recommendedAlertDialog = new RecommendedAppsAlertDialog();
        recommendedAlertDialog.setItemView(itemView);
        recommendedAlertDialog.setPosition(position);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(RECOMMENDED_APPS_ALERT_DIALOG_TAG);
        recommendedAlertDialog.show(fragmentTransaction,RECOMMENDED_APPS_ALERT_DIALOG_TAG);
    }

    void startAppLockSwitchOnDialog(){
        StartAppLockDialog startDialog = new StartAppLockDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(START_APP_LOG_DIALOG_TAG);
        startDialog.show(fragmentTransaction,START_APP_LOG_DIALOG_TAG);
    }

    void unlockRecommendedApp(AppLockRecyclerViewItem item,int position){
        if(appLockRecyclerAdapter!=null){
            appLockRecyclerAdapter.removeRecommendedApp(item,position);
        }
    }

    @TargetApi(21)
    void checkAndSetUsagePermissions(int requestFlag){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
            if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED) {
                setUsageAccessPermissionGranted(true);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                    if(requestFlag == USAGE_ACCESS_PERMISSION_REQUEST){
                    checkAndSetOverlayPermission(OVERLAY_PERMISSION_REQUEST);
                    }
                    if(requestFlag == USAGE_ACCESS_PERMISSION_REQUEST_LINEAR){
                        checkAndSetOverlayPermission(OVERLAY_PERMISSION_REQUEST_LINEAR);
                    }
                }
                Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName())
                        == AppOpsManager.MODE_ALLOWED));
            } else {
                setUsageAccessPermissionGranted(false);
                Log.d(AppLockingService.TAG,"No Usage");
            }
        }
    }

    @TargetApi(23)
    void checkAndSetOverlayPermission(int requestFlag){
        AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
        if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED) {
            setOverlayPermissionGranted(true);
            Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW
                    , Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED));
        }
        else
        {
            if(requestFlag == OVERLAY_PERMISSION_REQUEST) {
                setOverlayPermissionGranted(false);
            }
            if(requestFlag == OVERLAY_PERMISSION_REQUEST_LINEAR){
                startOverlayPermissionDialog();
            }
            Log.d(AppLockingService.TAG,"No Overlay");
        }
    }

    @TargetApi(21)
    void startUsageAccessSettingActivity(){
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),USAGE_ACCESS_PERMISSION_REQUEST);
    }

    @TargetApi(23)
    void requestOverlayPermission(){
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),OVERLAY_PERMISSION_REQUEST);
    }

    void requestNotificationPermission(){
        startActivityForResult(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),NOTIFICATION_PERMISSION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == USAGE_ACCESS_PERMISSION_REQUEST){
            usageDialog.dismiss();
            checkAndSetUsagePermissions(USAGE_ACCESS_PERMISSION_REQUEST_LINEAR);
        }else
        if(requestCode == OVERLAY_PERMISSION_REQUEST){
            overlayPermissionDialog.dismiss();
            checkAndSetOverlayPermission(OVERLAY_PERMISSION_REQUEST);
        }
        if(requestCode == NOTIFICATION_PERMISSION_REQUEST){
            notificationPermissionDialog.dismiss();
            return;
        }
    }

    void updateNotificationMap(){
        if(appLockRecyclerAdapter!=null){
            appLockRecyclerAdapter.loadNotificationAppsMap();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"Called onStart");
        IntentFilter filter = new IntentFilter(NotificationLockService.UPDATE_LOCK_PACKAGES);
        LocalBroadcastManager.getInstance(this).registerReceiver(notifUpdateReceiver,filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appLockRecyclerAdapter!=null) {
            appLockRecyclerAdapter.notifyDataSetChanged();
            appLockRecyclerAdapter.updateAppModel();
            Log.d(TAG,"");
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getUsageAccessPermissionGranted()){
            startService(new Intent(this,AppLockingService.class));
            startService(new Intent(this,GetPaletteColorService.class));
        }
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            startService(new Intent(this,AppLockingService.class));
            startService(new Intent(this,GetPaletteColorService.class));
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notifUpdateReceiver);

        Log.d(TAG,"Called onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"Called onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appLockRecyclerAdapter!=null){
            appLockRecyclerAdapter.closeAppLockRecyclerAdapter();
        }
        Log.d(TAG,"Called onDestroy");
    }

    static class NotificationMapUpdateReceiver extends BroadcastReceiver{

        WeakReference<AppLockActivity> activityReference;

        NotificationMapUpdateReceiver(WeakReference<AppLockActivity> activity){
            this.activityReference = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(NotificationLockService.UPDATE_LOCK_PACKAGES)){
                activityReference.get().updateNotificationMap();
            }
        }
    }
}

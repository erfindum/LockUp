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

import com.smartfoxitsolutions.lockup.dialogs.GrantUsageAccessDialog;
import com.smartfoxitsolutions.lockup.dialogs.NotificationPermissionDialog;
import com.smartfoxitsolutions.lockup.dialogs.OverlayPermissionDialog;
import com.smartfoxitsolutions.lockup.dialogs.RecommendedAppsAlertDialog;
import com.smartfoxitsolutions.lockup.dialogs.StartAppLockDialog;
import com.smartfoxitsolutions.lockup.services.AppLockForegroundService;
import com.smartfoxitsolutions.lockup.services.AppLockingService;
import com.smartfoxitsolutions.lockup.services.GetPaletteColorService;
import com.smartfoxitsolutions.lockup.services.NotificationLockService;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockActivity extends AppCompatActivity {
    private static final String TAG = "AppLockActivity ";

    public static final int NOTIFICATION_PERMISSION_REQUEST = 6;
    private static final String NOTIFICATION_ACCESS_DIALOG_TAG = "notification_permission_dialog";
    private static final String RECOMMENDED_APPS_ALERT_DIALOG_TAG = "recommended_apps_alert_dialog";
    private static final String START_APP_LOG_DIALOG_TAG = "start_app_lock_dialog";
    public static final String APP_LOCK_FIRST_START_PREFERENCE_KEY = "app_lock_first_start";

    public static boolean shouldStartAppLock;

    Toolbar appLockActivityToolbar;
    RecyclerView appLockRecyclerView;
    AppLockRecyclerAdapter appLockRecyclerAdapter;
    private AppLockModel appLockModel;
    private static boolean usagePermissionGranted,overlayPermissionGranted;
    private DialogFragment notificationPermissionDialog;
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

    public void unlockRecommendedApp(AppLockRecyclerViewItem item,int position){
        if(appLockRecyclerAdapter!=null){
            appLockRecyclerAdapter.removeRecommendedApp(item,position);
        }
    }

    public void requestNotificationPermission(){
        startActivityForResult(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),NOTIFICATION_PERMISSION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
        startService(new Intent(this,AppLockingService.class));
        startService(new Intent(this,GetPaletteColorService.class));
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

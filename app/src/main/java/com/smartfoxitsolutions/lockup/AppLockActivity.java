package com.smartfoxitsolutions.lockup;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockActivity extends AppCompatActivity {
    private static final String TAG = "AppLockActivity ";

    public static final int USAGE_ACCESS_PERMISSION_REQUEST=3;
    public static final int OVERLAY_PERMISSION_REQUEST = 5;
    private static final String USAGE_ACCESS_DIALOG_TAG = "usageAccessPermissionDialog";

    Toolbar appLockActivityToolbar;
    RecyclerView appLockRecyclerView;
    AppLockRecyclerAdapter appLockRecyclerAdapter;
    private AppLockModel appLockModel;
    private static boolean usagePermissionGranted,overlayPermissionGranted;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_lock_activity);
        appLockModel = new AppLockModel(this.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE));
        appLockRecyclerView = (RecyclerView) findViewById(R.id.app_lock_activity_recycler_view);
        appLockActivityToolbar = (Toolbar) findViewById(R.id.app_lock_activity_tool_bar);
        setSupportActionBar(appLockActivityToolbar);
        appLockActivityToolbar.setTitleTextColor(Color.WHITE);
        getSupportActionBar().setTitle("AppLock");
        checkAndSetUsagePermissions();
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            overlayPermissionGranted = true;
        }
        calculateMarginHeader();
        displayRecyclerView();


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
        DialogFragment usageDialog = new GrantUsageAccessDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        usageDialog.show(fragmentTransaction,USAGE_ACCESS_DIALOG_TAG);
    }

    void startOverlayPermissionDialog(){
        DialogFragment overlayPermissionDialog = new OverlayPermissionDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        overlayPermissionDialog.show(fragmentTransaction,"overlay_permission_dialog");
    }

    @TargetApi(21)
    void checkAndSetUsagePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
            if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED) {
                setUsageAccessPermissionGranted(true);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                    checkAndSetOverlayPermission();
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
    void checkAndSetOverlayPermission(){
        AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
        if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED) {
            setOverlayPermissionGranted(true);
            Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW
                    , Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED));
        }
        else
        {
            setOverlayPermissionGranted(false);
            startOverlayPermissionDialog();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == USAGE_ACCESS_PERMISSION_REQUEST){
            checkAndSetUsagePermissions();
        }else
        if(requestCode == OVERLAY_PERMISSION_REQUEST){
            checkAndSetOverlayPermission();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"Called onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appLockRecyclerAdapter!=null) {
            appLockRecyclerAdapter.notifyDataSetChanged();
            Log.d(TAG,"");
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getUsageAccessPermissionGranted()
                && getOverlayPermissionGranted()){
            startService(new Intent(this,AppLockingService.class));
            startService(new Intent(this,GetPaletteColorService.class));
        }
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            startService(new Intent(this,AppLockingService.class));
            startService(new Intent(this,GetPaletteColorService.class));
        }

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
}

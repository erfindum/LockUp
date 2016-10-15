package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 07-09-2016.
 */
public class AppLoaderActivity extends AppCompatActivity {

    private static final int REQUEST_START_ACTIVITY_FIRST_LOAD =2;
    private static final int REQUEST_START_LOCKUP_ACTIVITY = 3;
    private SharedPreferences prefs;
    private static boolean isFirstLoad;
    private ArrayList<String> recommendedAppList;
    private TreeMap<String,String> installedAppMap,checkedAppMap;
    private TreeMap<String,Boolean> recommendedAppMap;
    private AppLockModel appLockModel;
    private ExecutorService appListLoadExecutor;

    static boolean isLockUpFirstLoad(){
        return isFirstLoad;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_loader_activity);
        appLockModel = new AppLockModel(getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE));
        prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        isFirstLoad = prefs.getBoolean(AppLockModel.LOCK_UP_FIRST_LOAD_PREF_KEY,true);
        List<String> array1Resource = Arrays.asList(this.getResources().getStringArray(R.array.recommended_app_list));
        recommendedAppList = new ArrayList<>(array1Resource);

    }

    void queryInstalledApps(){

        appListLoadExecutor = Executors.newSingleThreadExecutor();
        appListLoadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                installedAppMap = appLockModel.getInstalledAppsMap();
                checkedAppMap = appLockModel.getCheckedAppsMap();
                recommendedAppMap = appLockModel.getRecommendedAppsMap();
                for(String entries: recommendedAppList){
                    if(!recommendedAppMap.containsKey(entries)){
                        recommendedAppMap.put(entries,false);
                    }
                }
                PackageManager pkgManager = getPackageManager();
                List<ResolveInfo> mainPackages = pkgManager.queryIntentActivities(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER),PackageManager.GET_META_DATA);
                StringBuilder packageName = new StringBuilder();
                StringBuilder appName = new StringBuilder();
                ApplicationInfo appNameInfo = new ApplicationInfo();
                for (ResolveInfo appInfo : mainPackages){
                    packageName.delete(0,packageName.length());
                    packageName.append(appInfo.activityInfo.packageName);
                    if(!installedAppMap.containsKey(packageName.toString()) &&
                            !checkedAppMap.containsKey(packageName.toString())){
                        appName.delete(0,packageName.length());
                        try {
                            appNameInfo = pkgManager.getApplicationInfo(packageName.toString(), PackageManager.GET_META_DATA);
                        }catch (PackageManager.NameNotFoundException e){
                            e.printStackTrace();
                        }
                        appName.append(pkgManager.getApplicationLabel(appNameInfo));
                        installedAppMap.put(packageName.toString(),appName.toString());
                    }
                }
                if(installedAppMap.containsKey(getPackageName())){
                    installedAppMap.remove(getPackageName());
                }
                appLockModel.updateRecommendedAppPackages(recommendedAppMap);
                appLockModel.updateAppPackages(installedAppMap,AppLockModel.INSTALLED_APPS_PACKAGE);
                appLockModel.loadAppPackages(AppLockModel.RECOMMENDED_APPS_PACKAGE);
                int loadComplete = appLockModel.loadAppPackages(AppLockModel.INSTALLED_APPS_PACKAGE);
                Log.d("AppLoclLoader","Task complete " + loadComplete);
                if(loadComplete == AppLockModel.APP_LIST_UPDATED) {
                    startMainActivity();
                }
            }
        });

    }

    void startMainActivity(){
        if (isLockUpFirstLoad()){
            startActivityForResult(new Intent(this,SetPinPatternActivity.class),REQUEST_START_ACTIVITY_FIRST_LOAD);
        }else{
            startActivityForResult(new Intent(this,LockUpMainActivity.class),REQUEST_START_LOCKUP_ACTIVITY);
        }

    }
    @Override
    protected void onResume() {
       queryInstalledApps();
        super.onResume();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_START_ACTIVITY_FIRST_LOAD){
            finish();
        }else if(requestCode == REQUEST_START_LOCKUP_ACTIVITY){
            finish();
        }
    }
}

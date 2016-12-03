package com.smartfoxitsolutions.lockup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by RAAJA on 07-09-2016.
 */
public class AppLoaderActivity extends AppCompatActivity {

    private static final int REQUEST_START_ACTIVITY_FIRST_LOAD =2;
    private static final int REQUEST_START_LOCKUP_ACTIVITY = 3;
    public static final String MEDIA_THUMBNAIL_WIDTH_KEY = "thumbnail_width_key";
    public static final String MEDIA_THUMBNAIL_HEIGHT_KEY = "thumbnail_height_key";
    public static final String ALBUM_THUMBNAIL_WIDTH = "album_thumbnail_width";
    private SharedPreferences prefs;
    private static boolean isFirstLoad;
    private ArrayList<String> recommendedAppList;
    private TreeMap<String,String> installedAppMap,checkedAppMap;
    private LinkedHashMap<String,HashMap<String,Boolean>> recommendedAppMap;
    private AppLockModel appLockModel;

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
        List<String> array1Resource;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
            String[] array1 = {getResources().getString(R.string.recommended_text_sixteen_down_one)};
            array1Resource = Arrays.asList(array1);
        }else{
            String[] array1 = {getResources().getString(R.string.recommended_text_sixteen_up_one)
                    ,getResources().getString(R.string.recommended_text_sixteen_up_two)};
            array1Resource = Arrays.asList(array1);
        }
        recommendedAppList = new ArrayList<>(array1Resource);
        measureItemView();
        queryInstalledApps();

    }

    void measureItemView(){
        Context ctxt = getBaseContext();
        int viewWidth = Math.round(DimensionConverter.convertDpToPixel(155,ctxt));
        int viewHeight = Math.round(DimensionConverter.convertDpToPixel(115,ctxt));
        int itemWidth = Math.round(DimensionConverter.convertDpToPixel(165,ctxt));
        SharedPreferences.Editor edit = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE).edit();
        edit.putInt(MEDIA_THUMBNAIL_WIDTH_KEY,viewWidth);
        edit.putInt(MEDIA_THUMBNAIL_HEIGHT_KEY,viewHeight);
        edit.putInt(ALBUM_THUMBNAIL_WIDTH,itemWidth);
        edit.apply();
    }

    void queryInstalledApps(){
        installedAppMap = appLockModel.getInstalledAppsMap();
        checkedAppMap = appLockModel.getCheckedAppsMap();
        recommendedAppMap = appLockModel.getRecommendedAppsMap();
        for(int i=0;i<recommendedAppList.size();i++){
            if(recommendedAppMap.containsKey(recommendedAppList.get(i))){
                continue;
            }
            HashMap<String,Boolean> stringMap = new HashMap<>();

            if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
                if(i==0) {
                    stringMap.put("Prevent Force Stop",false);
                    recommendedAppMap.put(recommendedAppList.get(i),stringMap);
                }
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                if(i==0 ) {
                    stringMap.put("Lock Notifications",false);
                    recommendedAppMap.put(recommendedAppList.get(i), stringMap);
                }
                if(i==1){
                    stringMap.put("Prevent Force Stop",false);
                    recommendedAppMap.put(recommendedAppList.get(i),stringMap);
                }
            }
        }
        if(!recommendedAppMap.containsKey("com.android.packageinstaller")){
            HashMap<String,Boolean> stringMap = new HashMap<>();
                stringMap.put("Install/Uninstall Apps",true);
                recommendedAppMap.put("com.android.packageinstaller",stringMap);
        }
        PackageManager pkgManager = getPackageManager();
        List<ResolveInfo> mainPackages = pkgManager.queryIntentActivities(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER),PackageManager.GET_META_DATA);
        for (ResolveInfo appInfo : mainPackages){
            String packageName = appInfo.activityInfo.packageName;
            if(!installedAppMap.containsKey(packageName) &&
                    !checkedAppMap.containsKey(packageName)){

                    if(packageName.equalsIgnoreCase("com.android.settings") && !recommendedAppMap.containsKey(packageName)){
                        try {
                        ApplicationInfo appNameInfo = pkgManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                        String appName = (String) pkgManager.getApplicationLabel(appNameInfo);
                        HashMap<String,Boolean> recommendTemp = new HashMap<>();
                        recommendTemp.put(appName,true);
                        recommendedAppMap.put(packageName,recommendTemp);
                        }catch (PackageManager.NameNotFoundException e){
                            e.printStackTrace();
                        }
                        continue;
                    }
                    if(packageName.equalsIgnoreCase("com.android.vending") && !recommendedAppMap.containsKey(packageName)){
                        try {
                        ApplicationInfo appNameInfo = pkgManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                        String appName = (String) pkgManager.getApplicationLabel(appNameInfo);
                        HashMap<String,Boolean> recomendTemp = new HashMap<>();
                        recomendTemp.put(appName,false);
                        recommendedAppMap.put(packageName,recomendTemp);
                        }catch (PackageManager.NameNotFoundException e){
                            e.printStackTrace();
                        }
                        continue;
                    }
                    if(packageName.equalsIgnoreCase("com.android.vending") || packageName.equalsIgnoreCase("com.android.settings") ){
                        continue;
                    }
                        try {
                    ApplicationInfo appNameInfo = pkgManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                    String appName = (String) pkgManager.getApplicationLabel(appNameInfo);
                    installedAppMap.put(packageName,appName);
                        }catch (PackageManager.NameNotFoundException e){
                            e.printStackTrace();
                        }

            }
        }
        if(installedAppMap.containsKey(getPackageName())){
            installedAppMap.remove(getPackageName());
        }
        appLockModel.updateRecommendedAppPackages(recommendedAppMap);
        appLockModel.updateAppPackages(installedAppMap,AppLockModel.INSTALLED_APPS_PACKAGE);
        startMainActivity();
    }

    void startMainActivity(){
        if (isLockUpFirstLoad()){
            startActivityForResult(new Intent(this,SetPinPatternActivity.class)
                    .putExtra(SetPinPatternActivity.INTENT_PIN_PATTERN_START_TYPE_KEY,SetPinPatternActivity.INTENT_APP_LOADER)
                    ,REQUEST_START_ACTIVITY_FIRST_LOAD);
        }else{
            startActivityForResult(new Intent(this,LockUpMainActivity.class),REQUEST_START_LOCKUP_ACTIVITY);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        if(isLockUpFirstLoad()){
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,true);
            edit.apply();
        }
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

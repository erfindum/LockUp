package com.smartfoxitsolutions.lockup;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

    public static final String UPDATE_APP_CLOUD_KEY = "update_lockup_app";
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
        boolean isHandled = queryIncomingIntent();
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
        if(!isHandled) {
            queryInstalledApps();
        }
    }

    boolean queryIncomingIntent(){
        if(getIntent().getExtras()!=null){
            for(String key:getIntent().getExtras().keySet()){
                Log.d("LockFire","KEY : " + key);
                if(key.equals(UPDATE_APP_CLOUD_KEY)) {
                    String value = (String) getIntent().getExtras().get(key);
                    if (value != null) {
                        Log.d("LockFire", "VALUE : " + value);
                    }
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + value))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                        return true;
                    }catch (ActivityNotFoundException e){
                        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("http://play.google.com/store/apps/details?id="+value))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                        return true;
                    }
                }

            }
        }
        return false;
    }

    void queryInstalledApps(){
        PackageManager pkgManager = getPackageManager();
        if(isLockUpFirstLoad()){
            ComponentName mediaMoveActivityComponent = new ComponentName(this, MediaMoveActivity.class);
            pkgManager.setComponentEnabledSetting(mediaMoveActivityComponent,PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    ,PackageManager.DONT_KILL_APP);
        }
        LinkedList<String> recommendedAddedList = new LinkedList<>();
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
                    stringMap.put(getResources().getString(R.string.recommended_text_sixteen_down_one),false);
                    recommendedAppMap.put(recommendedAppList.get(i),stringMap);
                }
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                if(i==0 ) {
                    stringMap.put(getResources().getString(R.string.recommended_text_sixteen_up_one),false);
                    recommendedAppMap.put(recommendedAppList.get(i), stringMap);
                }
                if(i==1){
                    stringMap.put(getResources().getString(R.string.recommended_text_sixteen_up_two),false);
                    recommendedAppMap.put(recommendedAppList.get(i),stringMap);
                }
            }
        }
        List<ResolveInfo> installerPackages = pkgManager.queryIntentActivities(new Intent(Intent.ACTION_INSTALL_PACKAGE)
                                            .setDataAndType(Uri.parse("file:///"),"application/vnd.android.package-archive")
                                                                                ,PackageManager.GET_META_DATA);
        if(installerPackages!=null && !installerPackages.isEmpty()){
            ResolveInfo installerInfo = installerPackages.get(0);
            String installerPackage = installerInfo.activityInfo.packageName;
            HashMap<String,Boolean> stringMap = new HashMap<>();
                stringMap.put("Install/Uninstall Apps",true);
                recommendedAppMap.put(installerPackage,stringMap);
            recommendedAddedList.add(installerPackage);
            Log.d("AppLoader","Added Installer " + installerPackage);
        }

        List<ResolveInfo> settingsPackages = pkgManager.queryIntentActivities(new Intent(Settings.ACTION_DATE_SETTINGS)
                                                                            ,PackageManager.GET_META_DATA);
        if(settingsPackages!=null && !settingsPackages.isEmpty()){
            try{
                ResolveInfo settingsInfo = settingsPackages.get(0);
                    String settingsPackage = settingsInfo.activityInfo.packageName;
                    ApplicationInfo appNameInfo = pkgManager.getApplicationInfo(settingsPackage, PackageManager.GET_META_DATA);
                    String settingsAppName = pkgManager.getApplicationLabel(appNameInfo).toString();
                    HashMap<String,Boolean> recomendTemp = new HashMap<>();
                    recomendTemp.put(settingsAppName,true);
                    recommendedAppMap.put(settingsPackage,recomendTemp);
                recommendedAddedList.add(settingsPackage);
                    Log.d("AppLoader","Added Settings " + settingsPackage);
                }catch (PackageManager.NameNotFoundException e){
                    e.printStackTrace();
                }

        }

        Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id="));

        List<ResolveInfo> marketPackages = pkgManager.queryIntentActivities(marketIntent,PackageManager.GET_META_DATA);
        if(marketPackages!=null && !marketPackages.isEmpty()){
            for(ResolveInfo marketInfo : marketPackages){
                if(marketInfo.activityInfo.packageName.startsWith("com.android")) {
                    try {
                        String marketPackage = marketInfo.activityInfo.packageName;
                        ApplicationInfo appNameInfo = pkgManager.getApplicationInfo(marketPackage, PackageManager.GET_META_DATA);
                        String marketAppName = pkgManager.getApplicationLabel(appNameInfo).toString();
                        HashMap<String, Boolean> recomendTemp = new HashMap<>();
                        recomendTemp.put(marketAppName, false);
                        recommendedAppMap.put(marketPackage, recomendTemp);
                        recommendedAddedList.add(marketPackage);
                        Log.d("AppLoader", "Added Installer " + marketPackage);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        List<ResolveInfo> mainPackages = pkgManager.queryIntentActivities(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER),PackageManager.GET_META_DATA);
        for (ResolveInfo appInfo : mainPackages){
            String packageName = appInfo.activityInfo.packageName;
            if(!installedAppMap.containsKey(packageName) &&
                    !checkedAppMap.containsKey(packageName)){

                    if(recommendedAddedList.contains(packageName)){
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
            startActivityForResult(new Intent(this,MainLockActivity.class),REQUEST_START_LOCKUP_ACTIVITY);
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

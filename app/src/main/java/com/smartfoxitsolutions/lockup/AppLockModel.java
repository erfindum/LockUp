package com.smartfoxitsolutions.lockup;

import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockModel {
    static final String INSTALLED_APPS_SHARED_PREF_KEY = "installedAppsMap";
    static final String CHECKED_APPS_SHARED_PREF_KEY = "checkedAppsMap";
    public static final String NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY = "notificationAppsMap";
    static final String RECOMMENDED_APPS_SHARED_PREF_KEY = "recommendedInstallerLock";
    static final String CHECKED_APPS_COLOR_SHARED_PREF_KEY = "checkedAppsColorMap";
    static final String USER_SET_LOCK_PASS_CODE = "userLockPasscode";

    static final int QUERY_TASK_TIME = 6000;

    static final String LOCK_UP_FIRST_LOAD_PREF_KEY = "lockUp_is_first_load";
    static final String VIBRATOR_ENABLED_PREF_KEY = "app_lock_vibrator_enabled";
    static final String APP_LOCK_LOCKMODE = "app_lock_lockmode";

    static final int INSTALLED_APPS_PACKAGE =1;
    static final int CHECKED_APPS_PACKAGE=2;
    static final int RECOMMENDED_APPS_PACKAGE=5;
    static final int NOTIFICATION_CHECKED_APPS_PACKAGE = 6;
    public static final String APP_LOCK_PREFERENCE_NAME="lockUp_general_preferences";
    static final int APP_LIST_UPDATED =3;
    static final int APP_LOCK_MODE_PATTERN = 54;
    static final int APP_LOCK_MODE_PIN = 55;

    private SharedPreferences sharedPreferences;
    private TreeMap<String,String> installedAppsMap,checkedAppsMap,notificationCheckedAppMap;
    private HashMap<String,Boolean> recommendedAppsMap;
    private ArrayList<String> installedAppsPackage,installedAppsName,checkedAppsPackage,checkedAppsName,
                                recommendedAppList,notificationCheckedAppName,notificationCheckedAppPackage;
    private ArrayList<Boolean> recommendedAppLocked;
    private Type appsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
    private Type recommendMapToken = new TypeToken<HashMap<String,Boolean>>(){}.getType();
    private Gson gson;

    public AppLockModel(SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
        this.installedAppsPackage = new ArrayList<>();
        this.installedAppsName = new ArrayList<>();
        this.checkedAppsName = new ArrayList<>();
        this.checkedAppsPackage = new ArrayList<>();
        this.checkedAppsMap = new TreeMap<>();
        this.notificationCheckedAppMap = new TreeMap<>();
        this.notificationCheckedAppName = new ArrayList<>();
        this.notificationCheckedAppPackage = new ArrayList<>();
        this.installedAppsMap = new TreeMap<>();
        this.recommendedAppList = new ArrayList<>();
        this.recommendedAppLocked = new ArrayList<>();
        recommendedAppsMap = new HashMap<>();
        gson = new Gson();
        loadAppPackages(INSTALLED_APPS_PACKAGE);
        loadAppPackages(RECOMMENDED_APPS_PACKAGE);
        loadAppPackages(CHECKED_APPS_PACKAGE);
        loadAppPackages(NOTIFICATION_CHECKED_APPS_PACKAGE);
    }

    private void setInstalledAppsMap(TreeMap<String,String> installedMap){
        this.installedAppsMap = installedMap;
    }

    private void setCheckedAppsMap(TreeMap<String,String> checkedMap){
        this.checkedAppsMap =checkedMap;
    }

    private void setRecommendedAppsMap(HashMap<String,Boolean> recommendedMap){
        this.recommendedAppsMap = recommendedMap;
    }

    private void setNotificationCheckedAppMap(TreeMap<String,String> notificationCheckedAppMap){
        this.notificationCheckedAppMap = notificationCheckedAppMap;
    }


    TreeMap<String,String> getInstalledAppsMap(){
        if(installedAppsMap!=null){
        return this.installedAppsMap;
        }else{
            return new TreeMap<>();
        }
    }

    TreeMap<String,String> getCheckedAppsMap(){
        if(checkedAppsMap!=null){
            return this.checkedAppsMap;
        }else{
            return new TreeMap<>();
        }
    }

    TreeMap<String,String> getNotificationCheckedAppMap(){
        if(notificationCheckedAppMap !=null){
            return this.notificationCheckedAppMap;
        }else{
            return  new TreeMap<>();
        }
    }

    HashMap<String,Boolean> getRecommendedAppsMap(){
        if(recommendedAppsMap!=null){
            return this.recommendedAppsMap;
        }else{
            return new HashMap<>();
        }
    }

    ArrayList<String> getInstalledAppsPackage(){
        if(installedAppsPackage!=null){
            return this.installedAppsPackage;
        }else{
            return new ArrayList<>();
        }
    }
    ArrayList<String> getInstalledAppsName(){
        if(installedAppsName!=null){
            return this.installedAppsName;
        }else{
            return new ArrayList<>();
        }
    }

    ArrayList<String> getCheckedAppsPackage(){
        if(checkedAppsPackage!=null){
            return this.checkedAppsPackage;
        }else{
            return new ArrayList<>();
        }
    }

    ArrayList<String> getCheckedAppsName(){
        if(checkedAppsName!=null){
            return this.checkedAppsName;
        }else{
            return new ArrayList<>();
        }
    }

    ArrayList<String> getNotificationCheckedAppName(){
        if(notificationCheckedAppName!=null){
            return this.notificationCheckedAppName;
        }else{
            return new ArrayList<>();
        }
    }

    ArrayList<String> getNotificationCheckedAppPackage(){
        if(notificationCheckedAppPackage!=null){
            return this.notificationCheckedAppPackage;
        }else{
            return new ArrayList<>();
        }
    }

    ArrayList<String> getRecommendedAppList(){
        if(recommendedAppList!=null){
            return this.recommendedAppList;
        }else{
            return new ArrayList<>();
        }
    }

    ArrayList<Boolean>getRecommendedAppLocked(){
        if(recommendedAppLocked!=null){
            return this.recommendedAppLocked;
        }else{
            return new ArrayList<>();
        }
    }

    int loadAppPackages(int flag){

            if(flag == INSTALLED_APPS_PACKAGE){
                String installedAppsJSONString = sharedPreferences.getString(INSTALLED_APPS_SHARED_PREF_KEY,null);
                TreeMap<String,String> installedApps = new TreeMap<>();
                if(installedAppsJSONString!=null) {
                     installedApps = gson.fromJson(installedAppsJSONString, appsMapToken);
                }
                if(installedApps!=null){
                    setInstalledAppsMap(installedApps);
                    setAppsLockList(installedApps.entrySet(),INSTALLED_APPS_PACKAGE);
                }
            }
            if (flag==CHECKED_APPS_PACKAGE){
                String checkedAppsJSONString = sharedPreferences.getString(CHECKED_APPS_SHARED_PREF_KEY,null);
                TreeMap<String,String> checkedApps=new TreeMap<>();
                if(checkedAppsJSONString!=null) {
                    checkedApps = gson.fromJson(checkedAppsJSONString, appsMapToken);
                }
                if(checkedApps!=null){
                    setCheckedAppsMap(checkedApps);
                    setAppsLockList(checkedApps.entrySet(),CHECKED_APPS_PACKAGE);
                }
            }
            if(flag==RECOMMENDED_APPS_PACKAGE){
                String recommendedAppsJSONString = sharedPreferences.getString(RECOMMENDED_APPS_SHARED_PREF_KEY,null);
                HashMap<String,Boolean> recommendedApps = new HashMap<>();
                if(recommendedAppsJSONString!=null) {
                    recommendedApps = gson.fromJson(recommendedAppsJSONString, recommendMapToken);
                }
                if(recommendedApps!=null){
                    setRecommendedAppsMap(recommendedApps);
                    setRecommendedList(recommendedApps.entrySet());
                }
            }

        if(flag==NOTIFICATION_CHECKED_APPS_PACKAGE){
            String notificationAppsJSONString = sharedPreferences.getString(NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY,null);
            TreeMap<String,String> notificationApps= new TreeMap<>();
            if(notificationAppsJSONString!=null) {
                notificationApps = gson.fromJson(notificationAppsJSONString, appsMapToken);
            }
            if(notificationApps!=null){
                setNotificationCheckedAppMap(notificationApps);
                setAppsLockList(notificationApps.entrySet(),NOTIFICATION_CHECKED_APPS_PACKAGE);
            }
        }


        return APP_LIST_UPDATED;
    }

    void updateAppPackages(TreeMap<String,String> updatedMap,int flag){
        SharedPreferences.Editor edit = sharedPreferences.edit();
        if(flag==INSTALLED_APPS_PACKAGE){
            String installedAppsJSONString = gson.toJson(updatedMap,appsMapToken);
            edit.putString(INSTALLED_APPS_SHARED_PREF_KEY,installedAppsJSONString);
            edit.apply();
        }else if(flag==CHECKED_APPS_PACKAGE) {
            String checkedAppsJSONString = gson.toJson(updatedMap, appsMapToken);
            edit.putString(CHECKED_APPS_SHARED_PREF_KEY, checkedAppsJSONString);
            edit.apply();
        }
        else if(flag == NOTIFICATION_CHECKED_APPS_PACKAGE){
            String notificationAppsJSONString = gson.toJson(updatedMap, appsMapToken);
            edit.putString(NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY, notificationAppsJSONString);
            edit.apply();
        }
    }

    void updateRecommendedAppPackages(HashMap<String,Boolean> recommendMap){
        SharedPreferences.Editor edit = sharedPreferences.edit();
        String recommendedAppsJSONString = gson.toJson(recommendMap,recommendMapToken);
        edit.putString(RECOMMENDED_APPS_SHARED_PREF_KEY,recommendedAppsJSONString);
        edit.apply();
    }

   private void setAppsLockList(Set<Map.Entry<String,String>> entrySet, int flag){

        ArrayList<Map.Entry<String,String>> entryList = new ArrayList<>(entrySet);

        Collections.sort(entryList, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> entryOne, Map.Entry<String, String> entryTwo) {
                            return entryOne.getValue().compareTo(entryTwo.getValue());
            }
        });

        if(flag==INSTALLED_APPS_PACKAGE){
            installedAppsPackage.clear();
            installedAppsName.clear();
            for(Map.Entry<String,String> entry : entryList){
                if(installedAppsPackage!=null){
                    this.installedAppsPackage.add(entry.getKey());
                   // Log.d("NotificationLock",entry.getKey());
                }
                if(installedAppsName!=null){
                    this.installedAppsName.add(entry.getValue());
                }
            }
        }
        if(flag==CHECKED_APPS_PACKAGE){
            checkedAppsName.clear();
            checkedAppsPackage.clear();
            for(Map.Entry<String,String> entry : entryList){
                if(checkedAppsPackage!=null){
                    this.checkedAppsPackage.add(entry.getKey());
                   // Log.d("NotificationLock",entry.getKey());
                }
                if(checkedAppsName!=null){
                    this.checkedAppsName.add(entry.getValue());
                }
            }
        }

        if(flag == NOTIFICATION_CHECKED_APPS_PACKAGE){
            notificationCheckedAppName.clear();
            notificationCheckedAppPackage.clear();
            for(Map.Entry<String,String> entry : entryList){
                if(notificationCheckedAppPackage!=null){
                    this.notificationCheckedAppPackage.add(entry.getKey());
                    // Log.d("NotificationLock",entry.getKey());
                }
                if(notificationCheckedAppName!=null){
                    this.notificationCheckedAppName.add(entry.getValue());
                }
            }
        }
    }

    private void setRecommendedList(Set<Map.Entry<String,Boolean>> entrySet){
        for (Map.Entry<String,Boolean> entry : entrySet){
            if(recommendedAppList!=null &&!this.recommendedAppList.contains(entry.getKey())) {
                this.recommendedAppList.add(entry.getKey());
            }
            if(recommendedAppLocked!=null && recommendedAppLocked.size()<3){
                recommendedAppLocked.add(entry.getValue());
            }
        }
    }

}

package com.smartfoxitsolutions.lockup;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockModel {
    public static final String INSTALLED_APPS_SHARED_PREF_KEY = "installedAppsMap";
    public static final String CHECKED_APPS_SHARED_PREF_KEY = "checkedAppsMap";
    public static final String NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY = "notificationAppsMap";
    public static final String RECOMMENDED_APPS_SHARED_PREF_KEY = "recommendedInstallerLock";
    public static final String CHECKED_APPS_COLOR_SHARED_PREF_KEY = "checkedAppsColorMap";

    public static final String USER_SET_LOCK_PASS_CODE = "userLockPasscode";
    public static final String DEFAULT_APP_BACKGROUND_COLOR_KEY = "defaultAppBackgroundColor";

    static final String NOTIFICATION_ACTIVITY_CHECKED_APPS_NAME_KEY = "notificationCheckedAppsName";
    static final String NOTIFICATION_ACTIVITY_CHECKED_APPS_PACKAGE_KEY = "notificationCheckedAppsPackage";

    public static final int QUERY_TASK_TIME = 6000;

    public static final String LOCK_UP_FIRST_LOAD_PREF_KEY = "lockUp_is_first_load";
    public static final String LOCKUP_VERSION_CODE = "lockup_version_code";
    public static final String SWIPE_LOCK_AD_DISPLAY = "swipe_lock_ad";


    static final int INSTALLED_APPS_PACKAGE =1;
    static final int CHECKED_APPS_PACKAGE=2;
    static final int RECOMMENDED_APPS_PACKAGE=5;
    static final int NOTIFICATION_CHECKED_APPS_PACKAGE = 6;

    public static final String APP_LOCK_PREFERENCE_NAME="lockUp_general_preferences";
    public static final int APP_LIST_UPDATED =3;

    public static final String APP_LOCK_LOCKMODE = "app_lock_lockmode";
    public static final int APP_LOCK_MODE_PATTERN = 54;
    public static final int APP_LOCK_MODE_PIN = 55;

    private SharedPreferences sharedPreferences;
    private TreeMap<String,String> installedAppsMap,checkedAppsMap,notificationCheckedAppMap;
    private LinkedHashMap<String,HashMap<String,Boolean>> recommendedAppsMap;
    private ArrayList<String> installedAppsPackage,installedAppsName,checkedAppsPackage,checkedAppsName,
            recommendedAppPackageList;
    private LinkedHashMap<String,Boolean> recommendedAppLocked;
    private Type appsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
    private Type recommendMapToken = new TypeToken<LinkedHashMap<String,HashMap<String,Boolean>>>(){}.getType();
    private Gson gson;

    public AppLockModel(SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
        this.installedAppsPackage = new ArrayList<>();
        this.installedAppsName = new ArrayList<>();
        this.checkedAppsName = new ArrayList<>();
        this.checkedAppsPackage = new ArrayList<>();
        this.checkedAppsMap = new TreeMap<>();
        this.notificationCheckedAppMap = new TreeMap<>();
        this.installedAppsMap = new TreeMap<>();
        this.recommendedAppPackageList = new ArrayList<>();
        this.recommendedAppLocked = new LinkedHashMap<>(0,8,false);
        recommendedAppsMap = new LinkedHashMap<>(0,8,false);
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

        private void setRecommendedAppsMap(LinkedHashMap<String,HashMap<String,Boolean>> recommendedMap){
            this.recommendedAppsMap = recommendedMap;
        }

    private void setNotificationCheckedAppMap(TreeMap<String,String> notificationCheckedAppMap){
        this.notificationCheckedAppMap = notificationCheckedAppMap;
    }


    TreeMap<String,String> getInstalledAppsMap(){
        if(installedAppsMap==null){
            return new TreeMap<>();
        }else{
            return this.installedAppsMap;
        }
    }

    public TreeMap<String,String> getCheckedAppsMap(){
        if(checkedAppsMap ==null){
            return new TreeMap<>();
        }else{
            return this.checkedAppsMap;
        }
    }

    TreeMap<String,String> getNotificationCheckedAppMap(){
        if(notificationCheckedAppMap ==null){
            return  new TreeMap<>();
        }else{
            return this.notificationCheckedAppMap;
        }
    }

    public LinkedHashMap<String,HashMap<String,Boolean>> getRecommendedAppsMap(){
        if(recommendedAppsMap==null){
            return new LinkedHashMap<>();
        }else{
            return this.recommendedAppsMap;
        }
    }

    ArrayList<String> getInstalledAppsPackage(){
        if(installedAppsPackage==null){
            return new ArrayList<>();
        }else{

            return this.installedAppsPackage;
        }
    }
    ArrayList<String> getInstalledAppsName(){
        if(installedAppsName==null){
            return new ArrayList<>();
        }else{
            return this.installedAppsName;
        }
    }

    ArrayList<String> getCheckedAppsPackage(){
        if(checkedAppsPackage==null){
            return new ArrayList<>();
        }else{
            return this.checkedAppsPackage;
        }
    }

    ArrayList<String> getCheckedAppsName(){
        if(checkedAppsName==null){
            return new ArrayList<>();
        }else{
            return this.checkedAppsName;
        }
    }

    ArrayList<String> getRecommendedAppPackageList(){
        if(recommendedAppPackageList ==null){
            return new ArrayList<>();
        }else{
            return this.recommendedAppPackageList;
        }
    }

    HashMap<String,Boolean> getRecommendedAppLocked(){
        if(recommendedAppLocked==null){
            return new HashMap<>();
        }else{
            return this.recommendedAppLocked;
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
               LinkedHashMap<String,HashMap<String,Boolean>> recommendedApps = new LinkedHashMap<>();
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

    void updateRecommendedAppPackages(LinkedHashMap<String,HashMap<String,Boolean>> recommendMap){
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
    }

    private void setRecommendedList(Set<Map.Entry<String,HashMap<String,Boolean>>> entrySet){
        for (Map.Entry<String,HashMap<String,Boolean>> entry : entrySet){
            if(recommendedAppPackageList !=null &&!this.recommendedAppPackageList.contains(entry.getKey())) {
                this.recommendedAppPackageList.add(entry.getKey());
            }
            ArrayList<String> strList = new ArrayList<>(entry.getValue().keySet());
            String str = strList.get(0);
            if(recommendedAppLocked!=null && !this.recommendedAppLocked.containsKey(str)){
                ArrayList<Boolean> boolList = new ArrayList<>(entry.getValue().values());
                recommendedAppLocked.put(str,boolList.get(0));
            }
        }
    }


}

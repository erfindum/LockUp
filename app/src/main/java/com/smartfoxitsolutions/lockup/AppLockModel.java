package com.smartfoxitsolutions.lockup;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockModel {
    public static final String INSTALLED_APPS_SHARED_PREF_KEY = "installedAppsMap";
    public static final String CHECKED_APPS_SHARED_PREF_KEY = "checkedAppsMap";
    public static final String RECOMMENDED_APPS_SHARED_PREF_KEY = "recommendedInstallerLock";
    public static final String LOCK_UP_FIRST_LOAD_PREF_KEY = "lockUp_is_first_load";
    public static final String VIBRATOR_ENABLED_PREF_KEY = "app_lock_vibrator_enabled";

    public static final int INSTALLED_APPS_PACKAGE =1;
    public  static final int CHECKED_APPS_PACKAGE=2;
    public static final int RECOMMENDED_APPS_PACKAGE=5;
    public static final String APP_LOCK_PREFERENCE_NAME="lockUp_general_preferences";
    public static final int APP_LIST_UPDATED =3;

    private SharedPreferences sharedPreferences;
    private TreeMap<String,String> installedAppsMap,checkedAppsMap;
    private TreeMap<String,Boolean> recommendedAppsMap;
    private ArrayList<String> installedAppsPackage,installedAppsName,checkedAppsPackage,checkedAppsName,
                                recommendedAppList;
    private ArrayList<Boolean> recommendedAppLocked;
    private Type appsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
    private Type recommendMapToken = new TypeToken<TreeMap<String,Boolean>>(){}.getType();
    private Gson gson;

    public AppLockModel(SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
        this.installedAppsPackage = new ArrayList<>();
        this.installedAppsName = new ArrayList<>();
        this.checkedAppsName = new ArrayList<>();
        this.checkedAppsPackage = new ArrayList<>();
        this.checkedAppsMap = new TreeMap<>();
        this.installedAppsMap = new TreeMap<>();
        this.recommendedAppList = new ArrayList<>();
        this.recommendedAppLocked = new ArrayList<>();
        recommendedAppsMap = new TreeMap<>();
        gson = new Gson();
        loadAppPackages(INSTALLED_APPS_PACKAGE);
        loadAppPackages(RECOMMENDED_APPS_PACKAGE);
        loadAppPackages(CHECKED_APPS_PACKAGE);
    }

    private void setInstalledAppsMap(TreeMap<String,String> installedMap){
        this.installedAppsMap = installedMap;
    }

    private void setCheckedAppsMap(TreeMap<String,String> checkedMap){
        this.checkedAppsMap =checkedMap;
    }

    void setRecommendedAppsMap(TreeMap<String,Boolean> recommendedMap){
        this.recommendedAppsMap = recommendedMap;
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
    TreeMap<String,Boolean> getRecommendedAppsMap(){
        if(recommendedAppsMap!=null){
            return this.recommendedAppsMap;
        }else{
            return new TreeMap<>();
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
                TreeMap<String,String> installedApps = ((TreeMap<String, String>) gson.fromJson(installedAppsJSONString, appsMapToken));
                if(installedApps!=null){
                    setInstalledAppsMap(installedApps);
                    setAppsLockList(installedApps.entrySet(),INSTALLED_APPS_PACKAGE);
                }
            }
            if (flag==CHECKED_APPS_PACKAGE){
                String checkedAppsJSONString = sharedPreferences.getString(CHECKED_APPS_SHARED_PREF_KEY,null);
                TreeMap<String,String> checkedApps = ((TreeMap<String, String>) gson.fromJson(checkedAppsJSONString, appsMapToken));
                if(checkedApps!=null){
                    setCheckedAppsMap(checkedApps);
                    setAppsLockList(checkedApps.entrySet(),CHECKED_APPS_PACKAGE);
                }
            }
            if(flag==RECOMMENDED_APPS_PACKAGE){
                String recommendedAppsJSONString = sharedPreferences.getString(RECOMMENDED_APPS_SHARED_PREF_KEY,null);
                TreeMap<String,Boolean> recommendedApps = ((TreeMap<String, Boolean>) gson.fromJson(recommendedAppsJSONString, recommendMapToken));
                if(recommendedApps!=null){
                    setRecommendedAppsMap(recommendedApps);
                    setRecommendedList(recommendedApps.entrySet());
                }
            }
        return APP_LIST_UPDATED;
    }

    public void updateAppPackages(TreeMap<String,String> updatedMap,int flag){
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
    }

    public void updateRecommendedAppPackages(TreeMap<String,Boolean> recommendMap){
        SharedPreferences.Editor edit = sharedPreferences.edit();
        String recommendedAppsJSONString = gson.toJson(recommendMap,recommendMapToken);
        edit.putString(RECOMMENDED_APPS_SHARED_PREF_KEY,recommendedAppsJSONString);
        edit.apply();
    }

    void setAppsLockList(Set<Map.Entry<String,String>> entrySet, int flag){

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
                }
                if(checkedAppsName!=null){
                    this.checkedAppsName.add(entry.getValue());
                }
            }
        }
    }

    void setRecommendedList(Set<Map.Entry<String,Boolean>> entrySet){
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

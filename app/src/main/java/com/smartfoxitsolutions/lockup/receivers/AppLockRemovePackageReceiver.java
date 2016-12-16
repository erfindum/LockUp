package com.smartfoxitsolutions.lockup.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.AppLockModel;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * Created by RAAJA on 24-10-2016.
 */

public class AppLockRemovePackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && !intent.getBooleanExtra(Intent.EXTRA_REPLACING,false)){
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            Gson gson = new Gson();
            Type appToken = new TypeToken<TreeMap<String,String>>(){}.getType();
            Type recommendMapToken = new TypeToken<LinkedHashMap<String,HashMap<String,Boolean>>>(){}.getType();

            SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
            String checkedAppString = prefs.getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,null);
            String installedAppString = prefs.getString(AppLockModel.INSTALLED_APPS_SHARED_PREF_KEY,null);
            String notificationAppString = prefs.getString(AppLockModel.NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY,null);
            String recommendedAppString = prefs.getString(AppLockModel.RECOMMENDED_APPS_SHARED_PREF_KEY,null);
            TreeMap<String,String> checkedAppsMap = gson.fromJson(checkedAppString,appToken);
            TreeMap<String,String> installedAppsMap = gson.fromJson(installedAppString,appToken);
            TreeMap<String,String> notificationAppsMap = gson.fromJson(notificationAppString,appToken);
            LinkedHashMap<String,HashMap<String,Boolean>> recommendedAppsMap = gson.fromJson(recommendedAppString,
                                                recommendMapToken);

            if(checkedAppsMap!=null && checkedAppsMap.containsKey(packageName)){
                SharedPreferences.Editor edit = prefs.edit();
                checkedAppsMap.remove(packageName);
                checkedAppString = gson.toJson(checkedAppsMap,appToken);
                edit.putString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,checkedAppString);
                edit.apply();
            }
            if(installedAppsMap != null && installedAppsMap.containsKey(packageName)){
                SharedPreferences.Editor edit = prefs.edit();
                installedAppsMap.remove(packageName);
                installedAppString = gson.toJson(installedAppsMap,appToken);
                edit.putString(AppLockModel.INSTALLED_APPS_SHARED_PREF_KEY,installedAppString);
                edit.apply();
            }
            if(recommendedAppsMap !=null && recommendedAppsMap.containsKey(packageName)){
                SharedPreferences.Editor edit = prefs.edit();
                recommendedAppsMap.remove(packageName);
                recommendedAppString = gson.toJson(recommendedAppsMap,recommendMapToken);
                edit.putString(AppLockModel.RECOMMENDED_APPS_SHARED_PREF_KEY,recommendedAppString);
                edit.apply();
            }
            if(notificationAppsMap != null && notificationAppsMap.containsKey(packageName)){
                SharedPreferences.Editor edit = prefs.edit();
                notificationAppsMap.remove(packageName);
                notificationAppString = gson.toJson(notificationAppsMap,appToken);
                edit.putString(AppLockModel.NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY,notificationAppString);
                edit.apply();
            }

        }
    }
}

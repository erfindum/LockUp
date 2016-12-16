package com.smartfoxitsolutions.lockup.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.AppLockModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 15-09-2016.
 */
public class GetPaletteColorService extends Service implements Handler.Callback {

    static final int UPDATE_APP_COLOR = 4;
    static final int FINISHED_APP_COLOR = 6;

    private Gson gson;
    private Type checkedAppsMapToken,checkedAppsColorMapToken;
    private ArrayList<String> checkedAppsList;
    private TreeMap<String,Integer> checkedAppColorMap;
    private ExecutorService getColorService;
    private GetPaletteColorTask getColorTask;
    private LinkedList<String> recommendedAppsList;

    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        checkedAppsList = new ArrayList<>();
        recommendedAppsList = new LinkedList<>();
        checkedAppsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
        checkedAppsColorMapToken = new TypeToken<TreeMap<String,Integer>>(){}.getType();
        Type recommendedAppsMapToken = new TypeToken<LinkedHashMap<String,HashMap<String,Boolean>>>(){}.getType();
        checkedAppColorMap = new TreeMap<>();
        SharedPreferences preferences = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        String recommendedAppsMapString = preferences.getString(AppLockModel.RECOMMENDED_APPS_SHARED_PREF_KEY,null);
        LinkedHashMap<String,HashMap<String,Boolean>> recommendedAppsMap =
                new Gson().fromJson(recommendedAppsMapString,recommendedAppsMapToken);
        if(recommendedAppsMap!=null && !recommendedAppsMap.isEmpty()){
         recommendedAppsList.addAll(recommendedAppsMap.keySet());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);

        String checkedAppsJSONString = prefs.getString(AppLockModel.CHECKED_APPS_SHARED_PREF_KEY,null);
        String checkedAppsColorJSONString = prefs.getString(AppLockModel.CHECKED_APPS_COLOR_SHARED_PREF_KEY,null);
        TreeMap<String,String> checkedAppsMap = gson.fromJson(checkedAppsJSONString,checkedAppsMapToken);
        TreeMap<String,Integer> checkedAppsColor = gson.fromJson(checkedAppsColorJSONString,checkedAppsColorMapToken);
        if(checkedAppsMap!=null){
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }
        if(checkedAppsColor != null){
            checkedAppColorMap = checkedAppsColor;
        }
        for(Map.Entry<String,Integer> color : checkedAppColorMap.entrySet() ){
            Log.d("AppLockColor",color.getValue() + " " + color.getKey());
        }
        getColorsFromPalette(checkedAppsList);
        return START_STICKY;
    }

    void getColorsFromPalette(ArrayList<String> checkedApps){
        getColorTask = new GetPaletteColorTask(checkedApps,recommendedAppsList,checkedAppColorMap,getBaseContext(),this);
        getColorService = Executors.newFixedThreadPool(1);
        getColorService.submit(getColorTask);
    }


    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == UPDATE_APP_COLOR){
            String appPackage = (String) msg.obj;
            if(appPackage!=null) {
                checkedAppColorMap.put(appPackage, msg.arg1);
                Log.d("AppLockColor", msg.arg1 + " Color");
            }

        }else
        if(msg.what == FINISHED_APP_COLOR){
            String checkedAppsColorMapString = gson.toJson(checkedAppColorMap,checkedAppsColorMapToken);
            SharedPreferences.Editor  edit = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                    .edit();
            edit.putString(AppLockModel.CHECKED_APPS_COLOR_SHARED_PREF_KEY,checkedAppsColorMapString);
            edit.apply();
            startService(new Intent(getBaseContext(),AppLockingService.class));
            stopSelf();
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(getColorService!=null && !getColorService.isShutdown()){
            getColorService.shutdown();
            getColorService = null;
        }
        if(getColorTask!=null){
            getColorTask.closeTask();
        }

    }
}

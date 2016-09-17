package com.smartfoxitsolutions.lockup;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by RAAJA on 15-09-2016.
 */
public class GetPaletteColorService extends Service {

    private Gson gson;
    private Type checkedAppsMapToken,checkedAppsColorMapToken;
    private ArrayList<String> checkedAppsList;
    private TreeMap<String,Integer> checkedAppColorMap;

    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        checkedAppsList = new ArrayList<>();
        checkedAppsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
        checkedAppsColorMapToken = new TypeToken<TreeMap<String,Integer>>(){}.getType();
        checkedAppColorMap = new TreeMap<>();
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
        TreeMap<String,Integer> checkedAppsColor = gson.fromJson(checkedAppsColorJSONString,checkedAppsMapToken);
        if(checkedAppsMap!=null){
            checkedAppsList = new ArrayList<>(checkedAppsMap.keySet());
        }
        if(checkedAppsColor != null){
            checkedAppColorMap = checkedAppsColor;
        }
        getColorsFromPalette(checkedAppsList);
        return START_STICKY;
    }

    void getColorsFromPalette(ArrayList<String> checkedApps){

        PackageManager packageManager = getPackageManager();
        for(final String appPackage: checkedApps){
            try{
               Drawable appIcon = packageManager.getApplicationIcon(appPackage);
                BitmapDrawable appBitmapDrawable = (BitmapDrawable) appIcon;
                if(appBitmapDrawable.getBitmap()!=null && !checkedAppColorMap.containsKey(appPackage)){
                    Palette.from(appBitmapDrawable.getBitmap()).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            Palette.Swatch swatch  = palette.getVibrantSwatch();
                            if(swatch!=null) {
                                checkedAppColorMap.put(appPackage, swatch.getRgb());
                            }
                            else {
                                checkedAppColorMap.put(appPackage,Color.parseColor("#2874F0"));
                            }
                        }
                    });
                }
                else{
                    if(!checkedAppColorMap.containsKey(appPackage)){
                        checkedAppColorMap.put(appPackage,Color.parseColor("#2874F0"));
                    }
                }
            }catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();;
            }
        }

        String checkedAppsColorMapString = gson.toJson(checkedAppColorMap,checkedAppsColorMapToken);
        SharedPreferences.Editor  edit = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                                            .edit();
        edit.putString(AppLockModel.CHECKED_APPS_COLOR_SHARED_PREF_KEY,checkedAppsColorMapString);
        edit.apply();
        startService(new Intent(getBaseContext(),AppLockingService.class));
        stopSelf();
    }
}

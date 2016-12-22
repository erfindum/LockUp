package com.smartfoxitsolutions.lockup.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.graphics.Palette;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * Created by RAAJA on 21-10-2016.
 */

public class GetPaletteColorTask implements Runnable {

    private ArrayList<String> checkedAppList;
    private Context ctxt;
    private Handler uiHandler;
    private LinkedList<String> recommendedAppsList;
    private TreeMap<String,Integer> checkedAppColorMap;

    public GetPaletteColorTask(ArrayList<String> checkedAppList,LinkedList<String> recommendedApps
            ,TreeMap<String,Integer> checkedAppColorMap, Context context, Handler.Callback callback) {
        this.checkedAppList = checkedAppList;
        this.recommendedAppsList = recommendedApps;
        this.checkedAppColorMap = checkedAppColorMap;
        ctxt = context;
        uiHandler = new Handler(Looper.getMainLooper(),callback);
    }

    @Override
    public void run() {
        PackageManager packageManager = ctxt.getPackageManager();
        for(final String recommendedPackage : recommendedAppsList){
            if(!checkedAppColorMap.containsKey(recommendedPackage)) {
                Message msg = uiHandler.obtainMessage();
                msg.what = GetPaletteColorService.UPDATE_APP_COLOR;
                if(Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN_MR2
                        && recommendedAppsList.get(1).equals(recommendedPackage)){
                    msg.arg1 = Color.parseColor("#2874F0");
                    msg.obj = recommendedPackage;
                    msg.sendToTarget();
                    continue;
                }
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2
                        && recommendedAppsList.get(2).equals(recommendedPackage)){
                    msg.arg1 = Color.parseColor("#2874F0");
                    msg.obj = recommendedPackage;
                    msg.sendToTarget();
                    continue;
                }
                try {
                    Drawable appIcon = packageManager.getApplicationIcon(recommendedPackage);
                    BitmapDrawable appBitmapDrawable = (BitmapDrawable) appIcon;
                    if (appBitmapDrawable.getBitmap() != null) {
                        Palette palette = Palette.from(appBitmapDrawable.getBitmap()).generate();
                        Palette.Swatch swatchVibrant = palette.getVibrantSwatch();
                        if (swatchVibrant != null) {
                            msg.arg1 = swatchVibrant.getRgb();
                        }
                        if (swatchVibrant == null) {
                            msg.arg1 = Color.parseColor("#2874F0");
                        }
                        msg.obj = recommendedPackage;
                        msg.sendToTarget();
                    } else if (appBitmapDrawable.getBitmap() == null) {
                        msg.what = GetPaletteColorService.UPDATE_APP_COLOR;
                        msg.arg1 = Color.parseColor("#2874F0");
                        msg.obj = recommendedPackage;
                        msg.sendToTarget();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        for(final String appPackage: checkedAppList){
            if(!checkedAppColorMap.containsKey(appPackage)) {
                Message msg = uiHandler.obtainMessage();
                msg.what = GetPaletteColorService.UPDATE_APP_COLOR;
                try {
                    Drawable appIcon = packageManager.getApplicationIcon(appPackage);
                    BitmapDrawable appBitmapDrawable = (BitmapDrawable) appIcon;
                    if (appBitmapDrawable.getBitmap() != null) {
                        Palette palette = Palette.from(appBitmapDrawable.getBitmap()).generate();
                        Palette.Swatch swatchVibrant = palette.getVibrantSwatch();
                        if (swatchVibrant != null) {
                            msg.arg1 = swatchVibrant.getRgb();
                        }
                        if (swatchVibrant == null) {
                            msg.arg1 = Color.parseColor("#2874F0");
                        }
                        msg.obj = appPackage;
                        msg.sendToTarget();
                    } else if (appBitmapDrawable.getBitmap() == null) {
                        msg.what = GetPaletteColorService.UPDATE_APP_COLOR;
                        msg.arg1 = Color.parseColor("#2874F0");
                        msg.obj = appPackage;
                        msg.sendToTarget();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        Message msg = uiHandler.obtainMessage(GetPaletteColorService.FINISHED_APP_COLOR);
        msg.sendToTarget();
    }

    void closeTask(){
        uiHandler = null;
        ctxt = null;
    }
}

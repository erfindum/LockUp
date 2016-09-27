package com.smartfoxitsolutions.lockup;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.List;

/**
 * Created by RAAJA on 15-09-2016.
 */
public class AppLockQueryTask implements Runnable {
    Context applicaionContext;
    Handler appLockUIHandler;
    UsageStatsManager usageStatsManager;
    ActivityManager activityManager;

    public AppLockQueryTask(Context context,Handler.Callback callback) {
        this.applicaionContext = context;
        appLockUIHandler = new Handler(Looper.getMainLooper(),callback);
        setQueryForDeviceAPI();

    }

    void setQueryForDeviceAPI(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            usageStatsManager =
                    (UsageStatsManager) applicaionContext.getSystemService(Context.USAGE_STATS_SERVICE);
        }else{
            activityManager = (ActivityManager) applicaionContext.getSystemService(Context.ACTIVITY_SERVICE);
        }
    }

    @Override
    public void run() {
        queryRecentApp();
    }

    @TargetApi(21)
    void queryRecentApp(){
        Message appQuery = appLockUIHandler.obtainMessage(AppLockingService.RECENT_APP_INFO);
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
               long currentTime = System.currentTimeMillis();
               UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime- 2000, currentTime + 300);
               UsageEvents.Event recentAppEvent = new UsageEvents.Event();
               while (usageEvents.hasNextEvent()) {
                   usageEvents.getNextEvent(recentAppEvent);
                   if (recentAppEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                       String packageName = recentAppEvent.getPackageName();
                       appQuery.obj = packageName;
                       Log.d("AppLockTask",packageName + " " +System.currentTimeMillis());
                   }
               }
           }
           else {
              try{ List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(10);
               appQuery.obj = recentTasks.get(0).topActivity.getPackageName();
              }catch (Exception e){
                  e.printStackTrace();
              }
           }

        if(appLockUIHandler != null){
            appQuery.sendToTarget();
        }

    }

}

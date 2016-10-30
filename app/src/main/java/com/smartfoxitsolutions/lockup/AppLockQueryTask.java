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

import java.util.List;



/**
 * Created by RAAJA on 30-10-2016.
 */

    public class AppLockQueryTask implements Runnable {
        Handler appLockUIHandler;
        UsageStatsManager usageStatsManager;
        ActivityManager activityManager;
        String[] packages;

        public AppLockQueryTask(Context context, Handler.Callback callback) {
            appLockUIHandler = new Handler(Looper.getMainLooper(),callback);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                usageStatsManager =
                        (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                packages = new String[2];
            }else{
                activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            }

        }


        @Override
        public void run() {
            queryRecentApp();
        }

        @TargetApi(21)
        void queryRecentApp(){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Message appQuery = appLockUIHandler.obtainMessage(AppLockingService.RECENT_APP_INFO_V21_UP);
                long currentTime = System.currentTimeMillis();
                UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime- AppLockModel.QUERY_TASK_TIME, currentTime);
                UsageEvents.Event recentAppEvent = new UsageEvents.Event();
                //Log.d("AppLockService","Queried Background "+ System.currentTimeMillis());
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(recentAppEvent);
                    if (recentAppEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        String packageName = recentAppEvent.getPackageName();
                        packages[0] = packageName;
                        // Log.d(AppLockingService.TAG,"Queried Foreground "+ packageName);
                    }
                    if (recentAppEvent.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        String packageName = recentAppEvent.getPackageName();
                        packages[1] = packageName;
                        // Log.d(AppLockingService.TAG,"Queried Background "+ packageName);
                    }
                }
                //Log.d("AppLockService","Queried Background Complete"+ System.currentTimeMillis());
                appQuery.obj = packages;
                if(appLockUIHandler != null){
                    appQuery.sendToTarget();
                }
            }
            else {
                Message appQuery = appLockUIHandler.obtainMessage(AppLockingService.RECENT_APP_INFO_V21_DOWN);
                try{ List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(10);
                    appQuery.obj = recentTasks.get(0).topActivity.getPackageName();
                }catch (Exception e){
                    e.printStackTrace();
                }
                if(appLockUIHandler != null){
                    appQuery.sendToTarget();
                }
            }

        }



    }

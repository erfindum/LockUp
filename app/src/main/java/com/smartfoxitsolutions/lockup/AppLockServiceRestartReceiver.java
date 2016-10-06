package com.smartfoxitsolutions.lockup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class AppLockServiceRestartReceiver extends BroadcastReceiver {
    public static final String ACTION_LOCK_SERVICE_RESTART = "com.smartfoxitsolutions.lockup.RESTART_LOCK_SERVICE";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_LOCK_SERVICE_RESTART)){
            Log.d(AppLockingService.TAG,"Called Restart Receiver " + " " + intent.getAction()+ " "+ System.currentTimeMillis());
            context.startService(new Intent(context,AppLockingService.class));
            Log.d(AppLockingService.TAG,"Called Restart Receiver Service Complete " + " " + intent.getAction()+ " "+ System.currentTimeMillis());
        }
    }
}

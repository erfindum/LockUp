package com.smartfoxitsolutions.lockup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by RAAJA on 13-09-2016.
 */
public class AppLockBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_SCREEN_OFF)){
            Log.d("AppLock","Screen Off");
        }
        else if(action.equals(Intent.ACTION_SCREEN_ON)){
            Log.d("AppLock","Screen Off");
        }
        else if(action.equals(Intent.ACTION_PACKAGE_ADDED)){

        }
    }
}

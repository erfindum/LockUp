package com.smartfoxitsolutions.lockup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by RAAJA on 13-09-2016.
 */
public class AppLockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
         if(action.equals(Intent.ACTION_PACKAGE_ADDED)){
            Log.d("AppLock","Package Added");
        }
    }
}

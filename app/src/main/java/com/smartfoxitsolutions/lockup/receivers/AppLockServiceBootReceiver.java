package com.smartfoxitsolutions.lockup.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.services.AppLockingService;


/**
 * Created by RAAJA on 05-10-2016.
 */

public class AppLockServiceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean shouldStartAppLock = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE)
                                    .getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        Log.d("AppLockService","should start lock " + String.valueOf(shouldStartAppLock));
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals("android.intent.action.QUICKBOOT_POWERON")
                ){
            if(shouldStartAppLock) {
                context.startService(new Intent(context, AppLockingService.class));
            }
        }
        if(action.equals(Intent.ACTION_USER_PRESENT)){
            if(shouldStartAppLock && !AppLockingService.isAppLockRunning) {
                context.startService(new Intent(context, AppLockingService.class));
            }
        }
    }
}

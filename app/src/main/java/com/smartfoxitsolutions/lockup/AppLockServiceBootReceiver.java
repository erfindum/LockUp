package com.smartfoxitsolutions.lockup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * Created by RAAJA on 05-10-2016.
 */

public class AppLockServiceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals("android.intent.action.QUICKBOOT_POWERON")
                || action.equals(Intent.ACTION_USER_PRESENT)){
            Log.d(AppLockingService.TAG,"Called Boot Receiver " + " " + intent.getAction()+ " "+ System.currentTimeMillis());
            context.startService(new Intent(context,AppLockingService.class));
            Log.d(AppLockingService.TAG,"Called Boot Receiver Service Complete " + " " + intent.getAction()+ " "+ System.currentTimeMillis());
        }
    }
}

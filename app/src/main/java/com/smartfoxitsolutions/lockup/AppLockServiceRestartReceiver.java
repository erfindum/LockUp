package com.smartfoxitsolutions.lockup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class AppLockServiceRestartReceiver extends BroadcastReceiver {
    public static final String ACTION_LOCK_SERVICE_RESTART = "com.smartfoxitsolutions.lockup.RESTART_LOCK_SERVICE";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_LOCK_SERVICE_RESTART) ||
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON")){
            context.startService(new Intent(context,AppLockingService.class));
        }
    }
}

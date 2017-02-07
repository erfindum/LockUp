package com.smartfoxitsolutions.lockup.loyaltybonus.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.smartfoxitsolutions.lockup.loyaltybonus.services.UserReportIntentService;

/**
 * Created by RAAJA on 06-02-2017.
 */

public class UserReportBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("LockupUserReport","Alarm fired --------");
        startWakefulService(context,new Intent(context, UserReportIntentService.class));
    }
}

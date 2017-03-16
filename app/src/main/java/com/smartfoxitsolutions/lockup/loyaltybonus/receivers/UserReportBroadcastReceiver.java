package com.smartfoxitsolutions.lockup.loyaltybonus.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusModel;
import com.smartfoxitsolutions.lockup.loyaltybonus.UserLoyaltyReport;
import com.smartfoxitsolutions.lockup.loyaltybonus.services.UserReportIntentService;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

/**
 * Created by RAAJA on 06-02-2017.
 */

public class UserReportBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("LockupUserReport","Alarm fired --------");
        SharedPreferences preferences = context.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
        if(isLoggedIn) {
            startWakefulService(context, new Intent(context, UserReportIntentService.class));
        }
    }
}

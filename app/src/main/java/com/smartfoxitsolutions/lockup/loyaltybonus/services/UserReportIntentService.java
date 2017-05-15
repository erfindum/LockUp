package com.smartfoxitsolutions.lockup.loyaltybonus.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.AppLockActivity;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusModel;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusRequest;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyServiceGenerator;
import com.smartfoxitsolutions.lockup.loyaltybonus.UserLoyaltyReport;
import com.smartfoxitsolutions.lockup.loyaltybonus.UserLoyaltyReportResponse;
import com.smartfoxitsolutions.lockup.loyaltybonus.receivers.UserReportBroadcastReceiver;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by RAAJA on 06-02-2017.
 */

public class UserReportIntentService extends IntentService {

    private boolean shouldStartAppLockOn,isAppLockFirstLoad;

    public UserReportIntentService(){
        super("UserReportService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        setReportAlarm(getBaseContext(),intent);
    }

    private void setReportAlarm(Context context, Intent intent){
        SharedPreferences preferences = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        shouldStartAppLockOn = preferences.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = preferences.getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,true);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if(calendar.get(Calendar.HOUR_OF_DAY)>=14 && calendar.get(Calendar.MINUTE)>=0 &&
                calendar.get(Calendar.SECOND)>0) {
            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 1);
            calendar.set(Calendar.SECOND, 0);
        }else{
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 1);
            calendar.set(Calendar.SECOND, 0);
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent reportPendingIntent = PendingIntent.getBroadcast(
                context,23,new Intent(context,UserReportBroadcastReceiver.class),0
        );
        alarmManager.cancel(reportPendingIntent);
        Log.d("LockupUserReport","Report Alarm Cancelled --------");
        checkAndSaveNewUserReport(context,calendar);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
            alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }else{
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }
        Log.d("LockupUserReport","Report Alarm Set --------");

        if(shouldStartAppLockOn && !isAppLockFirstLoad){
            startService(new Intent(getBaseContext(), AppLockingService.class));
        }
        UserReportBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void checkAndSaveNewUserReport(Context context, Calendar calendar){
        SharedPreferences preferences = context.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME
                ,Context.MODE_PRIVATE);
        String reportDate = String.valueOf(calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"
                +calendar.get(Calendar.DAY_OF_MONTH));
        Log.d("LockupUserReport",reportDate);
        Gson gson = new Gson();
        Type userReportToken = new TypeToken<LinkedHashMap<String,UserLoyaltyReport>>(){}.getType();
        String userReportMapCurrentString = preferences.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT,null);
        LinkedHashMap<String,UserLoyaltyReport> userReportCurrentMap = gson.fromJson(userReportMapCurrentString,userReportToken);
        if(userReportCurrentMap!=null && !userReportCurrentMap.isEmpty()){
            ArrayList<String> dateKeyList = new ArrayList<>(userReportCurrentMap.keySet());
            String dateKey = dateKeyList.get(dateKeyList.size()-1);
            UserLoyaltyReport userCurrentReport = userReportCurrentMap.get(dateKey);
            if(!reportDate.equals(userCurrentReport.getReportDate())){
                UserLoyaltyReport userReport = new UserLoyaltyReport(reportDate,0,0,0,0);
                userReportCurrentMap.put(reportDate,userReport);
                saveUserReportMap(userReportCurrentMap,gson,preferences,userReportToken);
                Log.d("LockupUserReport","New Report Saved --------");
            }
        }else{
            createNewUserReport(reportDate,gson,userReportToken,preferences);
        }
    }

    private void createNewUserReport(String reportDate, Gson gson, Type reportToken, SharedPreferences preferences){
        UserLoyaltyReport userReport = new UserLoyaltyReport(reportDate,0,0,0,0);
        LinkedHashMap<String,UserLoyaltyReport> userReportNewMap = new LinkedHashMap<>();
        userReportNewMap.put(reportDate,userReport);
        saveUserReportMap(userReportNewMap,gson,preferences,reportToken);
    }

    private void saveUserReportMap(LinkedHashMap<String,UserLoyaltyReport> userReportMap, Gson gson, SharedPreferences preferences
            ,Type reportToken){
        String userReportMapNewString = gson.toJson(userReportMap,reportToken);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT,userReportMapNewString);
        edit.apply();
    }
}

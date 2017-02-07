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
import com.smartfoxitsolutions.lockup.loyaltybonus.receivers.UserReportBroadcastReceiver;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
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
        SharedPreferences preferences = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        shouldStartAppLockOn = preferences
                .getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = preferences
                .getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,true);

        final SharedPreferences loyaltyPreference = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME
                                                    ,MODE_PRIVATE);
        final boolean isUserLoggedIn = loyaltyPreference.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final Gson gson = new Gson();
        final Type userReportToken = new TypeToken<LinkedHashMap<String, UserLoyaltyReport>>() {
        }.getType();
        NetworkInfo connectivityInfo = connectivityManager.getActiveNetworkInfo();
        final Calendar calendar = getReportCalendarInstance();
        if(connectivityInfo != null) {
            if (connectivityInfo.isConnected()) {
                String userReportMapCurrentString = loyaltyPreference.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
                LinkedHashMap<String, UserLoyaltyReport> userReportCurrentMap = gson.fromJson(userReportMapCurrentString, userReportToken);
                if (userReportCurrentMap != null && !userReportCurrentMap.isEmpty()) {
                    for(UserLoyaltyReport report : userReportCurrentMap.values()){
                        Log.d("LockupUserReport",report.getReportDate()+" Impressions: " + report.getTotalImpression()+" Clicks: "+ report.getTotalClicked());
                    }
                    Type reportDataToken = new TypeToken<ArrayList<UserLoyaltyReport>>(){}.getType();
                    String reportDataString = gson.toJson(new ArrayList<>(userReportCurrentMap.values()),reportDataToken);
                    LoyaltyBonusRequest userReportRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
                    Call<UserLoyaltyReportResponse> userReportCall = userReportRequest.sendUserLoyaltyReport(
                            "Report_add",
                            preferences.getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"nomail"),
                            loyaltyPreference.getString(LoyaltyBonusModel.LOYALTY_SEND_REQUEST,"noCode"),
                            reportDataString
                    );

                    try {
                        Log.d("LockupUserReport",userReportCall.request().url().toString());
                        Response<UserLoyaltyReportResponse> report = userReportCall.execute();
                        UserLoyaltyReportResponse reportResponse = report.body();
                        if(reportResponse.code.equals("200")){
                            Log.d("LockupUserReport","Report Success --------" + " " + reportResponse.totalPoint);
                            String reportDate = String.valueOf(calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"
                                    +calendar.get(Calendar.DAY_OF_MONTH));
                            Log.d("LockupUserReport",reportDate);
                            createNewUserReport(reportDate, gson, userReportToken, loyaltyPreference);
                            if (isUserLoggedIn) {
                                setAlarm(calendar);
                            }
                            closeReportService(intent);
                        }else
                        if(reportResponse.code.equals("100")){
                            addNewUserReport(loyaltyPreference, calendar);
                            if(isUserLoggedIn) {
                                setAlarm(calendar);
                            }
                            Log.d("LockupUserReport","Report Failure --------");
                            closeReportService(intent);
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                        addNewUserReport(loyaltyPreference, calendar);
                        if(isUserLoggedIn) {
                            setAlarm(calendar);
                        }
                        closeReportService(intent);
                    }
                }

            } else {
                addNewUserReport(loyaltyPreference, calendar);
                if(isUserLoggedIn) {
                    setAlarm(calendar);
                }
                closeReportService(intent);
            }
        }else{
            addNewUserReport(loyaltyPreference,calendar);
            if(isUserLoggedIn) {
                setAlarm(calendar);
            }
            closeReportService(intent);
        }

        Log.d("LockupUserReport","Report Complete --------");
    }

    private Calendar getReportCalendarInstance(){
        Calendar calendar = Calendar.getInstance();
        if(calendar.get(Calendar.HOUR_OF_DAY)>21 && calendar.get(Calendar.MINUTE)>0 &&
                    calendar.get(Calendar.SECOND)>0) {
            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 21);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }else{
            calendar.set(Calendar.HOUR_OF_DAY, 21);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }
        return calendar;
    }

    private void addNewUserReport(SharedPreferences preferences, Calendar calendar){
        String reportDate = String.valueOf(calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"
                +calendar.get(Calendar.DAY_OF_MONTH));
        Log.d("LockupUserReport",reportDate);
        Gson gson = new Gson();
        Type userReportToken = new TypeToken<LinkedHashMap<String,UserLoyaltyReport>>(){}.getType();
        String userReportMapCurrentString = preferences.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT,null);
        LinkedHashMap<String,UserLoyaltyReport> userReportCurrentMap = gson.fromJson(userReportMapCurrentString,userReportToken);
        if(userReportCurrentMap!=null && !userReportCurrentMap.isEmpty()){
            UserLoyaltyReport userReport = new UserLoyaltyReport(reportDate);
            userReport.setTotalImpression(0);
            userReport.setTotalClicked(0);
            userReportCurrentMap.put(reportDate,userReport);
            saveUserReportMap(userReportCurrentMap,gson,preferences,userReportToken);

        }else{
            createNewUserReport(reportDate,gson,userReportToken,preferences);
        }
    }

    private void createNewUserReport(String reportDate, Gson gson, Type userReportToken, SharedPreferences preferences){
        UserLoyaltyReport userReport = new UserLoyaltyReport(reportDate);
        userReport.setTotalImpression(0);
        userReport.setTotalClicked(0);
        LinkedHashMap<String,UserLoyaltyReport> userReportNewMap = new LinkedHashMap<>();
        userReportNewMap.put(reportDate,userReport);
        saveUserReportMap(userReportNewMap,gson,preferences,userReportToken);
    }

    private void saveUserReportMap(LinkedHashMap<String,UserLoyaltyReport> userReportMap, Gson gson, SharedPreferences preferences
            ,Type reportToken){
        String userReportMapNewString = gson.toJson(userReportMap,reportToken);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT,userReportMapNewString);
        edit.apply();
    }

    private void setAlarm(Calendar calendar){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent reportPendingIntent = PendingIntent.getBroadcast(
                this,23,new Intent(this,UserReportBroadcastReceiver.class),0
        );
        alarmManager.cancel(reportPendingIntent);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
            alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }else{
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }
        Log.d("LockupUserReport","Report Alarm Set --------");
    }

    private void closeReportService(Intent intent){
        if(shouldStartAppLockOn && !isAppLockFirstLoad){
            startService(new Intent(getBaseContext(), AppLockingService.class));
        }
        UserReportBroadcastReceiver.completeWakefulIntent(intent);
    }
}

package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.receivers.UserReportBroadcastReceiver;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by RAAJA on 29-01-2017.
 */

public class LoyaltyUserProfileFragment extends Fragment {

    private TextView fullName, noOfAppsLocked, appLockInfo,pointsEarned, redeemInfo;
    private CardView paypalCard, paytmCard;
    private FloatingActionButton appLockButton;
    private LoyaltyUserActivity activity;
    private ProgressBar loadingProgress;
    public static int lockedRecommendApps,lockedApps;
    private String pointUpdateTime;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_user_main,container,false);
        fullName = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_welcome);
        noOfAppsLocked = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_lock_info_one);
        appLockInfo = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_lock_info_two);
        pointsEarned = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_point);
        loadingProgress = (ProgressBar) parent.findViewById(R.id.loyalty_bonus_user_main_progress);
        redeemInfo = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_redeem_info_two);

        appLockButton = (FloatingActionButton) parent.findViewById(R.id.loyalty_bonus_user_main_lock_fab);

        paypalCard = (CardView) parent.findViewById(R.id.loyalty_bonus_user_paypal_group);
        paytmCard = (CardView) parent.findViewById(R.id.loyalty_bonus_user_paytm_group);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyUserActivity) getActivity();
        setListeners();
        setAppLockInfo();
    }

    void setListeners(){
        paypalCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startRedeemFragment("paypal");
            }
        });

        paytmCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startRedeemFragment("paytm");
            }
        });

        appLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    activity.checkAndSetUsagePermissions();
                }else{
                   activity.startLockActivity();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences loyaltyPrefs = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME
                , MODE_PRIVATE);
        String welcome = getString(R.string.loyalty_user_main_hello)+"! " +
                loyaltyPrefs.getString(LoyaltyBonusModel.LOGIN_USER_NAME_KEY,"Unknown");
        fullName.setText(welcome);
        pointsEarned.setText(loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_BONUS,"00.00"));
        Date date = getRequestCalendarInstance().getTime();
        DateFormat format = new SimpleDateFormat("hh:mm a");
        pointUpdateTime = format.format(date);
        String redeemInfoString = getString(R.string.loyalty_user_main_points_info_two)+" "+ pointUpdateTime;
        redeemInfo.setText(redeemInfoString);

        String lockInfo =(lockedRecommendApps+lockedApps)+" "+getString(R.string.loyalty_user_main_lock_info_one);
        noOfAppsLocked.setText(lockInfo);
        if((lockedRecommendApps + lockedApps)<=3){
            appLockInfo.setVisibility(View.VISIBLE);
            appLockInfo.setText(getString(R.string.loyalty_user_main_lock_info_two));
        }else{
            appLockInfo.setVisibility(View.INVISIBLE);
        }
        getLoyaltyPoints();

    }

    private void setAppLockInfo(){
        SharedPreferences appPrefs = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME
                , MODE_PRIVATE);
        AppLockModel appModel = new AppLockModel(appPrefs);

        String installerPackage = getAppInstallerPackage();
        LinkedHashMap<String,HashMap<String,Boolean>> recommendedMap = appModel.getRecommendedAppsMap();
        for(Map.Entry<String,HashMap<String,Boolean>> entry:recommendedMap.entrySet()){
            if(!entry.getKey().equals(installerPackage)){
                ArrayList<Boolean> tempList = new ArrayList<>(entry.getValue().values());
                if(tempList.get(0)){
                    lockedRecommendApps++;
                }
            }
        }
        TreeMap<String,String> checkedAppMap = appModel.getCheckedAppsMap();
        lockedApps = checkedAppMap.size();
    }

    private String getAppInstallerPackage(){
        Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        installIntent.addCategory(Intent.CATEGORY_DEFAULT);
        installIntent.setDataAndType(Uri.parse("file:///"),"application/vnd.android.package-archive");
        List<ResolveInfo> installerPackages = activity.getPackageManager().queryIntentActivities(installIntent, PackageManager.GET_META_DATA);

        if(installerPackages!=null && !installerPackages.isEmpty()){
            ResolveInfo installerInfo = installerPackages.get(0);
            return installerInfo.activityInfo.packageName;
        }else{
            return "none";
        }
    }

    private void getLoyaltyPoints(){
        requestLoyaltyPoints();
    }

    private void requestLoyaltyPoints(){
        final SharedPreferences preferences = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME
                ,MODE_PRIVATE);

        final SharedPreferences loyaltyPreference = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME
                ,MODE_PRIVATE);
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(CONNECTIVITY_SERVICE);
        final Gson gson = new Gson();
        final Type userReportToken = new TypeToken<LinkedHashMap<String, UserLoyaltyReport>>() {
        }.getType();
        final Calendar calendar = getReportCalendarInstance();
        NetworkInfo connectivityInfo = connectivityManager.getActiveNetworkInfo();
        String userReportMapCurrentString = loyaltyPreference.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
        final LinkedHashMap<String, UserLoyaltyReport> userReportCurrentMap = gson.fromJson(userReportMapCurrentString, userReportToken);
        if(userReportCurrentMap == null)
        {
            String reportDate = String.valueOf(calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"
                    +calendar.get(Calendar.DAY_OF_MONTH));
            createNewUserReport(reportDate,gson,userReportToken,loyaltyPreference);
            setAlarm(calendar);
            displayPoints();
            return;
        }
        long nextReportedDate = loyaltyPreference.getLong(LoyaltyBonusModel.NEXT_REPORTED_DATE,00000000);
        Log.d("LockupUserReport","Recent Request Date "+nextReportedDate+" : Current Date " + System.currentTimeMillis());
        if(System.currentTimeMillis()<nextReportedDate){
            setAlarm(calendar);
            displayPoints();
            String requestInfo = String.format(getString(R.string.loyalty_user_main_request_info),pointUpdateTime);
            displayInfoToast(requestInfo);
            return;
        }
        if(connectivityInfo != null) {
            if (connectivityInfo.isConnected()) {
                loadingProgress.setVisibility(View.VISIBLE);
                pointsEarned.setVisibility(View.INVISIBLE);
                if (!userReportCurrentMap.isEmpty()) {
                    ArrayList<UserLoyaltyReport> userReportCompleteList = new ArrayList<>(userReportCurrentMap.values());
                    final UserLoyaltyReport presetDayReport = userReportCompleteList.remove(userReportCompleteList.size()-1);
                    ArrayList<String> userReportDateList = new ArrayList<>(userReportCurrentMap.keySet());
                    final String presentDateString = userReportDateList.get(userReportDateList.size()-1);
                    for(UserLoyaltyReport report : userReportCompleteList){
                        Log.d("LockupUserReport",report.getReportDate()+" Impressions: " + report.getTotalImpression()+" Clicks: "+ report.getTotalClicked());
                    }
                    if(userReportCompleteList.isEmpty()){
                        setAlarm(calendar);
                        displayPoints();
                        return;
                    }

                    Type reportDataToken = new TypeToken<ArrayList<UserLoyaltyReport>>(){}.getType();
                    String reportDataString = gson.toJson(userReportCompleteList,reportDataToken);
                    LoyaltyBonusRequest userReportRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
                    Call<UserLoyaltyReportResponse> userReportCall = userReportRequest.sendUserLoyaltyReport(
                            "Report_add",
                            preferences.getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"nomail"),
                            loyaltyPreference.getString(LoyaltyBonusModel.LOYALTY_SEND_REQUEST,"noCode"),
                            reportDataString
                    );


                    Log.d("LockupUserReport",userReportCall.request().url().toString());
                    userReportCall.enqueue(new Callback<UserLoyaltyReportResponse>() {
                        @Override
                        public void onResponse(Call<UserLoyaltyReportResponse> call, Response<UserLoyaltyReportResponse> response) {
                            if(response.isSuccessful()){
                                UserLoyaltyReportResponse reportResponse = response.body();
                                if(reportResponse.code.equals("200")){
                                    Log.d("LockupUserReport","Report Success --------" + " " + reportResponse.totalPoint);
                                    loyaltyPreference.edit().putString(LoyaltyBonusModel.USER_LOYALTY_BONUS
                                            ,reportResponse.totalPoint).apply();
                                    userReportCurrentMap.clear();
                                    userReportCurrentMap.put(presentDateString,presetDayReport);
                                    saveUserReportMap(userReportCurrentMap,gson,loyaltyPreference,userReportToken);
                                    loyaltyPreference.edit().putLong(LoyaltyBonusModel.NEXT_REPORTED_DATE,getRequestCalendarInstance()
                                            .getTimeInMillis())
                                            .apply();
                                    pointsEarned.setText(reportResponse.totalPoint);
                                    setAlarm(calendar);
                                    displayPoints();
                                    return;
                                }
                                if(reportResponse.code.equals("100")){
                                    setAlarm(calendar);
                                    displayPoints();
                                    Log.d("LockupUserReport","Report Failure --------");
                                }
                            }else{
                                setAlarm(calendar);
                                displayPoints();
                                displayInfoToast(getString(R.string.loyalty_bonus_signup_unknown_error));
                                Log.d("LockupUserReport","Report Failure --------");
                            }
                        }

                        @Override
                        public void onFailure(Call<UserLoyaltyReportResponse> call, Throwable t) {
                            setAlarm(calendar);
                            displayPoints();
                            displayInfoToast(getString(R.string.loyalty_bonus_signup_unknown_error));
                            Log.d("LockupUserReport","Report Failure --------");
                        }
                    });

                }else{
                    loadingProgress.setProgress(View.INVISIBLE);
                    pointsEarned.setVisibility(View.VISIBLE);
                }
            } else {
                setAlarm(calendar);
                displayPoints();
                displayInfoToast(getString(R.string.loyalty_bonus_signup_no_connection));
            }
        }else{
            setAlarm(calendar);
            displayPoints();
            displayInfoToast(getString(R.string.loyalty_bonus_signup_no_connection));
        }

        Log.d("LockupUserReport","Report Complete --------");
    }

    private void displayPoints(){
        pointsEarned.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.INVISIBLE);
    }

    private void displayInfoToast(String message){
        if(activity!=null){
            Toast.makeText(activity,message,Toast.LENGTH_LONG).show();
        }
    }

    private Calendar getReportCalendarInstance(){
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
        return calendar;
    }

    private Calendar getRequestCalendarInstance(){
        Calendar requestCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        requestCalendar.add(Calendar.DAY_OF_MONTH,1);
        requestCalendar.set(Calendar.HOUR_OF_DAY,14);
        requestCalendar.set(Calendar.MINUTE,30);
        return requestCalendar;
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
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        PendingIntent reportPendingIntent = PendingIntent.getBroadcast(
                activity,23,new Intent(activity,UserReportBroadcastReceiver.class),0
        );
        alarmManager.cancel(reportPendingIntent);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
            alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }else{
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }
        Log.d("LockupUserReport","Report Alarm Set --------");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
        lockedRecommendApps = 0;
        lockedApps = 0;
    }
}

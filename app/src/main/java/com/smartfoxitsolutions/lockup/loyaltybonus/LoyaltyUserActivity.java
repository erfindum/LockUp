package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfoxitsolutions.lockup.AppLockActivity;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.dialogs.GrantUsageAccessDialog;
import com.smartfoxitsolutions.lockup.dialogs.OverlayPermissionDialog;
import com.smartfoxitsolutions.lockup.loyaltybonus.receivers.UserReportBroadcastReceiver;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by RAAJA on 29-01-2017.
 */

public class LoyaltyUserActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private FragmentManager fragmentManager;
    boolean shouldTrackUserPresence, shouldCloseAffinity, stopTrackAfterPermission
            ,shouldStartAppLockOn,isAppLockFirstLoad, isUserLoggedIn;
    private LoyaltyUserScreenOffReceiver loyaltyUserScreenOffReceiver;
    private GrantUsageAccessDialog usageDialog;
    private OverlayPermissionDialog overlayPermissionDialog;
    private boolean hasPermissionReturned;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loyalty_bonus_user_activity);
        toolbar = (Toolbar) findViewById(R.id.loyalty_bonus_user_activity_tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragmentManager = getFragmentManager();
        shouldTrackUserPresence = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(fragmentManager.findFragmentByTag("loyaltyProfileFragment")==null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            toolbar.setTitle(getString(R.string.loyalty_user_main_title));
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.loyalty_bonus_user_activity_container, new LoyaltyUserProfileFragment(), "loyaltyProfileFragment");
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack("loyaltyProfileFragment");
            transaction.commit();
        }

        loyaltyUserScreenOffReceiver = new LoyaltyUserScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(loyaltyUserScreenOffReceiver,filter);

        SharedPreferences loyaltyPreferences = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
        isUserLoggedIn = loyaltyPreferences.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);

        SharedPreferences preferences = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        shouldStartAppLockOn = preferences.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = preferences.getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,true);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(!stopTrackAfterPermission) {
            shouldTrackUserPresence = true;
        }else{
            stopTrackAfterPermission = false;
            hasPermissionReturned = false;
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity=true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        SubMenu subMenu = menu.addSubMenu(24,1,Menu.NONE,getString(R.string.loyalty_user_main_logout));
        subMenu.setIcon(R.drawable.selector_menu_dot);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(item.getItemId()==1){
            Log.d("LoyaltyUserMain","LogoutRequested");
            SharedPreferences preferences = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,
                    MODE_PRIVATE);
            SharedPreferences.Editor edit = preferences.edit();
            edit.putBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
            edit.apply();
            shouldTrackUserPresence = false;
            isUserLoggedIn = false;
            startActivity(new Intent(this,LoyaltyBonusMain.class).putExtra("userLoggedOut",25));
            finish();
            return true;
        }
        return false;
    }

    void startRedeemFragment(String type){
        toolbar.setTitle(getString(R.string.loyalty_bonus_redeem_title));
        Bundle args = new Bundle();
        args.putString("redeemType",type);
        LoyaltyUserRedeemFragment redeemFragment = new LoyaltyUserRedeemFragment();
        redeemFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.loyalty_bonus_user_activity_container,redeemFragment,"loyaltyRedeemFragment");
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack("loyaltyRedeemFragment");
        transaction.commit();
    }

    void startRedeemFinal(String type, int selection){
        toolbar.setTitle(getString(R.string.loyalty_redeem_final_redeem_title));
        Bundle args = new Bundle();
        args.putString("redeemFinalType",type);
        args.putInt("redeemFinalSelection",selection);
        LoyaltyUserRedeemFinalFragment redeemFinalFragment = new LoyaltyUserRedeemFinalFragment();
        redeemFinalFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.loyalty_bonus_user_activity_container,redeemFinalFragment,"loyaltyRedeemFinalFragment");
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack("loyaltyRedeemFinalFragment");
        transaction.commit();
    }

    @TargetApi(21)
    void checkAndSetUsagePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
            if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                    checkAndSetOverlayPermission();
                }else{
                    if(!hasPermissionReturned) {
                        shouldTrackUserPresence = false;
                        startActivity(new Intent(getBaseContext(), AppLockActivity.class));
                    }else{
                        shouldTrackUserPresence = false;
                        stopTrackAfterPermission = true;
                        startActivity(new Intent(getBaseContext(), AppLockActivity.class));
                    }
                }
                Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS
                        , Process.myUid(), getPackageName())
                        == AppOpsManager.MODE_ALLOWED));
            } else {
                startUsagePermissionDialog();
                Log.d(AppLockingService.TAG,"No Usage");
            }
        }
    }

    @TargetApi(23)
    void checkAndSetOverlayPermission(){
        if(Settings.canDrawOverlays(this)){
            if(!hasPermissionReturned) {
                shouldTrackUserPresence = false;
                startActivity(new Intent(getBaseContext(), AppLockActivity.class));
            }else{
                shouldTrackUserPresence = false;
                stopTrackAfterPermission = true;
                startActivity(new Intent(getBaseContext(), AppLockActivity.class));
            }
        }else{
            startOverlayPermissionDialog();
        }
    }

    void startUsagePermissionDialog(){
        Bundle usageBundle = new Bundle();
        usageBundle.putString("grandUsageStartType","loyaltyBonusStart");
        usageDialog = new GrantUsageAccessDialog();
        usageDialog.setArguments(usageBundle);
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("loyaltyUsagePermission");
        usageDialog.show(fragmentTransaction,"loyaltyUsagePermission");
    }

    void startOverlayPermissionDialog(){
        Bundle overlayBundle = new Bundle();
        overlayBundle.putString("overlayStartType","loyaltyBonusStart");
        overlayPermissionDialog = new OverlayPermissionDialog();
        overlayPermissionDialog.setArguments(overlayBundle);
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("loyaltyOverlayPermission");
        overlayPermissionDialog.show(fragmentTransaction,"loyaltyOverlayPermission");
    }

    @TargetApi(21)
    public void startUsageAccessSettingActivity(){
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),43);
        shouldTrackUserPresence = false;
    }

    @TargetApi(23)
    public void requestOverlayPermission(){
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),44);
        shouldTrackUserPresence = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 43){
            checkAndSetUsagePermissions();
            hasPermissionReturned = true;
        }else
        if(requestCode == 44){
            checkAndSetOverlayPermission();
            hasPermissionReturned = true;
        }
    }

    private void setReportAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent reportPendingIntent = PendingIntent.getBroadcast(
                this,23,new Intent(this,UserReportBroadcastReceiver.class),0
        );
        alarmManager.cancel(reportPendingIntent);
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
        String reportDate = String.valueOf(calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"
                                        +calendar.get(Calendar.DAY_OF_MONTH));
        Log.d("LockupUserReport",reportDate);
        saveInitialUserReport(reportDate);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
            alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }else{
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),reportPendingIntent);
        }
        Log.d("LockupUserReport","Report Alarm Set --------");
    }

    private void saveInitialUserReport(String reportDate){
        SharedPreferences preferences = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
        Gson gson = new Gson();
        Type userReportToken = new TypeToken<LinkedHashMap<String,UserLoyaltyReport>>(){}.getType();
        String userReportMapCurrentString = preferences.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT,null);
        LinkedHashMap<String,UserLoyaltyReport> userReportCurrentMap = gson.fromJson(userReportMapCurrentString,userReportToken);
        if(userReportCurrentMap!=null && !userReportCurrentMap.isEmpty()){
            ArrayList<String> dateKeyList = new ArrayList<>(userReportCurrentMap.keySet());
            String dateKey = dateKeyList.get(dateKeyList.size()-1);
           UserLoyaltyReport userCurrentReport = userReportCurrentMap.get(dateKey);
            if(!reportDate.equals(userCurrentReport.getReportDate())){
                UserLoyaltyReport userReport = new UserLoyaltyReport(reportDate);
                userReport.setTotalImpression(0);
                userReport.setTotalClicked(0);
                userReportCurrentMap.put(reportDate,userReport);
                saveUserReportMap(userReportCurrentMap,gson,preferences,userReportToken);
            }
        }else{
         createNewUserReport(reportDate,gson,userReportToken,preferences);
        }
    }

    private void createNewUserReport(String reportDate, Gson gson, Type reportToken, SharedPreferences preferences){
        UserLoyaltyReport userReport = new UserLoyaltyReport(reportDate);
        userReport.setTotalImpression(0);
        userReport.setTotalClicked(0);
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

    private void stopReportAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent reportPendingIntent = PendingIntent.getBroadcast(
                this,23,new Intent(this,UserReportBroadcastReceiver.class),0
        );
        alarmManager.cancel(reportPendingIntent);
        Log.d("LockupUserReport","Report Alarm Stopped --------");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(fragmentManager.getBackStackEntryCount()==0){
            finish();
            return;
        }

        FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount()-1);
        if(entry.getName().equals("loyaltyProfileFragment")){
            toolbar.setTitle(getString(R.string.loyalty_user_main_title));
        }
        if(entry.getName().equals("loyaltyRedeemFragment")){
            toolbar.setTitle(getString(R.string.loyalty_bonus_redeem_title));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldStartAppLockOn && !isAppLockFirstLoad){
            if(isUserLoggedIn) {
                setReportAlarm();
            }else{
                stopReportAlarm();
            }
            startService(new Intent(getBaseContext(), AppLockingService.class));
        }

        if(shouldCloseAffinity){
            finishAffinity();
        }
        if(!shouldTrackUserPresence){
           unregisterReceiver(loyaltyUserScreenOffReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(shouldTrackUserPresence){
            unregisterReceiver(loyaltyUserScreenOffReceiver);
        }
    }

    static class LoyaltyUserScreenOffReceiver extends BroadcastReceiver {

        WeakReference<LoyaltyUserActivity> activity;
        LoyaltyUserScreenOffReceiver(WeakReference<LoyaltyUserActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishAffinity();
            }
        }
    }
}

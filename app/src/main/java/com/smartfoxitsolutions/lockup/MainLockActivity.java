package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.smartfoxitsolutions.lockup.views.MainPatternView;
import com.smartfoxitsolutions.lockup.views.MainPinView;
import com.smartfoxitsolutions.lockup.views.OnPinLockUnlockListener;

/**
 * Created by RAAJA on 07-12-2016.
 */

public class MainLockActivity extends AppCompatActivity implements OnPinLockUnlockListener{

    private MainPatternView patternLockView;
    private MainPinView pinLockView;
    private boolean shouldTracUserPresence, shouldCloseAffinity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            MobileAds.initialize(this.getApplicationContext());
        }catch (Exception e){
            Log.d("LockUp","Main Lock Exception : " + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        boolean isFingerPrintActive = prefs.getBoolean(LockUpSettingsActivity.FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,false);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if(prefs.getInt(AppLockModel.APP_LOCK_LOCKMODE,0)==AppLockModel.APP_LOCK_MODE_PATTERN){
            patternLockView = new MainPatternView(this,this,isFingerPrintActive);
            patternLockView.setActivityBackground(metrics.heightPixels);
            setContentView(patternLockView);
        }
        if(prefs.getInt(AppLockModel.APP_LOCK_LOCKMODE,0)==AppLockModel.APP_LOCK_MODE_PIN){
            pinLockView = new MainPinView(this,this,isFingerPrintActive);
            pinLockView.setActivityBackground(metrics.heightPixels);
            setContentView(pinLockView);
        }
    }

    @Override
    public void onPinUnlocked() {
        if(patternLockView!=null){
            patternLockView.removeView();
            patternLockView = null;
        }
        if(pinLockView!=null){
            pinLockView.removeView();
            pinLockView = null;
        }
        startActivity(new Intent(getBaseContext(),LockUpMainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        shouldTracUserPresence = true;
    }

    @Override
    public void onPinLocked(String packageName) {
    }

    public void onAdImpressed(String type) {

    }

    public void onAdClicked(String type) {

    }

    public void startResetActivity(){
        startActivity(new Intent(this,ResetPasswordActivity.class));
        shouldTracUserPresence = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shouldTracUserPresence = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTracUserPresence){
            shouldCloseAffinity = true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        super.onBackPressed();
    }

}

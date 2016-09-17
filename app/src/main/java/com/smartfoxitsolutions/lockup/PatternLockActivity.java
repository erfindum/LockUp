package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.renderscript.Sampler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.google.android.gms.ads.NativeExpressAdView;

/**
 * Created by RAAJA on 15-09-2016.
 */
public class PatternLockActivity extends AppCompatActivity implements PatternLockView.OnPatternChangedListener{

    PatternLockView patternView;
    NativeExpressAdView nativeExpressAdView;
    ImageView appIconView;
    private String selectedPatternNode;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled;
    Vibrator patternViewVibrator;
    ValueAnimator patternAnimator;
    private String packageName;
    private int packageColor;
    private long patternPassCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        packageName = getIntent().getStringExtra(AppLockingService.CHECKED_APP_LOCK_PACKAGE_NAME);
        packageColor = getIntent().getIntExtra(AppLockingService.CHECKED_APP_LOCK_COLOR,0);
        if(packageColor!=0){
            Drawable appColor = new ColorDrawable(packageColor);
            getWindow().getDecorView().setBackground(appColor);
            getWindow().getDecorView().setAlpha(0.20f);
        }
        else{
            Drawable appColor = new ColorDrawable(Color.parseColor("#F52874F0"));
            getWindow().getDecorView().setBackground(appColor);
        }
        patternView = (PatternLockView) findViewById(R.id.pattern_lock_activity_pattern_view);
        nativeExpressAdView = (NativeExpressAdView) findViewById(R.id.pattern_lock_activity_ad_view);
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        patternPassCode = prefs.getLong(AppLockModel.USER_SET_LOCK_PASS_CODE,0);
        patternViewVibrator = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);
        setAppIcon(packageName);
        registerListeners();
        setPatternAnimator();
    }

    void setAppIcon(String packageName){
        try{
           Drawable appIcon =  getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(appIcon);
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    void registerListeners(){
        patternView.setOnPatternChangedListener(this);
    }

    void unRegisterListeners(){
        patternView.setOnPatternChangedListener(null);
    }

    void setPatternAnimator(){
        patternAnimator = ValueAnimator.ofFloat(0,1);
        patternAnimator.setDuration(200).setRepeatCount(3);
        patternAnimator.setInterpolator(new OvershootInterpolator());
        patternAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                patternView.postPatternError();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                patternView.resetPatternView();
                resetPatternData();
            }
        });
    }

    @Override
    public void onPatternNodeSelected(int selectedPatternNode) {
            this.selectedPatternNode = this.selectedPatternNode+String.valueOf(selectedPatternNode);
        Log.d("AppLock","Selected Pattern "+ this.selectedPatternNode);
            patternNodeSelectedCount=patternNodeSelectedCount+1;
            if(isVibratorEnabled){
                patternViewVibrator.vibrate(30);
            }
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
            if(patternCompleted && !selectedPatternNode.equals("")){
                Log.d("PatternLock Confirm",selectedPatternNode);
                long selectedPassCode= Long.parseLong(selectedPatternNode)*55439;
                if(patternPassCode == selectedPassCode){
                    Log.d("AppLock",selectedPatternNode + " " + patternPassCode);
                    patternView.resetPatternView();
                    resetPatternData();
                    postPatternCompleted(packageName);
                } else{
                    patternAnimator.start();
                }
            }
    }

    void resetPatternData(){
        selectedPatternNode="";
        patternNodeSelectedCount=0;
    }

    private void postPatternCompleted(String packageUnlockedName){

        AppLockingService.recentlyUnlockedApp = packageUnlockedName;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterListeners();
    }
}

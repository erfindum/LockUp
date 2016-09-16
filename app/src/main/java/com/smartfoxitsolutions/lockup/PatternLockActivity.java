package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
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
 * Created by RAAJA on 16-09-2016.
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        patternView = (PatternLockView) findViewById(R.id.pattern_lock_activity_pattern_view);
        nativeExpressAdView = (NativeExpressAdView) findViewById(R.id.pattern_lock_activity_ad_view);
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        patternViewVibrator = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);
        registerListeners();
        setPatternAnimator();
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
            this.selectedPatternNode = selectedPatternNode+String.valueOf(selectedPatternNode);
            patternNodeSelectedCount=patternNodeSelectedCount+1;
            if(isVibratorEnabled){
                patternViewVibrator.vibrate(30);
            }
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        String pattern="1258";
            if(patternCompleted && !selectedPatternNode.equals("")){
                if(pattern.equals(selectedPatternNode)){
                    Log.d("AppLock",selectedPatternNode + " " + pattern);
                    patternView.resetPatternView();
                    resetPatternData();
                }
                else{
                    patternAnimator.start();
                }
            }
    }

    void resetPatternData(){
        selectedPatternNode="";
        patternNodeSelectedCount=0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterListeners();
    }
}

package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.NativeExpressAdView;

/**
 * Created by RAAJA on 06-10-2016.
 */

public class LockPatternView extends FrameLayout implements PatternLockView.OnPatternChangedListener{
    Context context;
    private OnPinLockUnlockListener patternLockListener;

    PatternLockView patternView;
    NativeExpressAdView nativeExpressAdView;
    AdRequest patternLockAdRequest;
    ImageView appIconView;
    private String selectedPatternNode;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled;
    Vibrator patternViewVibrator;
    ValueAnimator patternAnimator;
    private String packageName;
    private long patternPassCode;
    private RelativeLayout patternViewParent;

    public LockPatternView(Context context, OnPinLockUnlockListener patternLockListener) {
        super(context);
        this.context = context;
        setPinLockUnlockListener(patternLockListener);
        patternLockListener.onPinLocked();
        LayoutInflater.from(context).inflate(R.layout.pattern_lock_activity,this,true);
        patternViewParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_parent_view);
        patternViewVibrator= (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        initializeLockView();
    }



    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.patternLockListener = pinLockListener;
    }

    void setPackageName(String packageName){
        this.packageName = packageName;
        setAppIcon(packageName);
    }

    void setWindowBackground(int colorVibrant, int displayHeight){
        GradientDrawable drawable = new GradientDrawable();
        int[] colors = {Color.parseColor("#263238"),colorVibrant};
        drawable.setColors(colors);
        float radius = Math.round(displayHeight*.95);
        drawable.setGradientRadius(radius);
        drawable.setGradientCenter(0.5f,1f);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setShape(GradientDrawable.RECTANGLE);
        patternViewParent.setBackground(drawable);
    }

    void initializeLockView(){
        patternView = (PatternLockView) findViewById(R.id.pattern_lock_activity_pattern_view);
        nativeExpressAdView = (NativeExpressAdView) findViewById(R.id.pattern_lock_activity_ad_view);
        String adAppId = getResources().getString(R.string.pin_lock_activity_ad_app_id);
        MobileAds.initialize(context,adAppId);
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        patternPassCode = prefs.getLong(AppLockModel.USER_SET_LOCK_PASS_CODE,0);
        patternLockAdRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("A04B6DE7DDFED7231620C0AA9BCF67EC")
                .addTestDevice("A219F9DA86E122F8F4AE0F7EF7FA95E5").build();
        nativeExpressAdView.loadAd(patternLockAdRequest);
        registerListeners();
        setPatternAnimator();
    }

    void setAppIcon(String packageName){
        try{
            Log.e(AppLockingService.TAG,"App Icon CAlled " + packageName);
            Drawable appIcon =  context.getPackageManager().getApplicationIcon(packageName);
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
       // Log.d("AppLock","Selected Pattern "+ this.selectedPatternNode);
        patternNodeSelectedCount=patternNodeSelectedCount+1;
        if(isVibratorEnabled){
            patternViewVibrator.vibrate(30);
        }
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        if(patternCompleted && !selectedPatternNode.equals("")){
           // Log.d("PatternLock Confirm",selectedPatternNode);
            long selectedPassCode= Long.parseLong(selectedPatternNode)*55439;
            if(patternPassCode == selectedPassCode){
            //    Log.d("AppLock",selectedPatternNode + " " + patternPassCode);
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
        patternLockListener.onPinUnlocked(packageUnlockedName);
    }

    void startHome(){
        context.startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction()!=KeyEvent.ACTION_UP && (event.getKeyCode() != KeyEvent.KEYCODE_BACK
                || event.getKeyCode() != KeyEvent.KEYCODE_HOME)) {
            return super.dispatchKeyEvent(event);
        }
        startHome();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    void removeView(){
        unRegisterListeners();
        setPinLockUnlockListener(null);
        context = null;
    }
}

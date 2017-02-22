package com.smartfoxitsolutions.lockup.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by RAAJA on 06-10-2016.
 */

public class LockPatternView extends FrameLayout implements PatternLockView.OnPatternChangedListener
                ,NativeAd.MoPubNativeEventListener{
    Context context;
    private OnPinLockUnlockListener patternLockListener;
    PatternLockView patternView;
    ImageView appIconView;
    private String selectedPatternNode, patternPassCode, salt;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled, shouldHidePatternLine;
    Vibrator patternViewVibrator;
    ValueAnimator patternAnimator;
    private RelativeLayout patternViewParent, patternAdParent;
    private RectF patternRect;
    private NativeAd moPubNativeAd;
    private View nativeAdView;


    public LockPatternView(Context context, OnPinLockUnlockListener patternLockListener) {
        super(context);
        this.context = getContext();
        setPinLockUnlockListener(patternLockListener);
        LayoutInflater.from(context).inflate(R.layout.pattern_lock_activity,this,true);
        patternViewParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_parent_view);
        patternViewVibrator= (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        patternAdParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_ad_parent);
        //initAds();
        initializeLockView();
    }



    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.patternLockListener = pinLockListener;
    }

    public void setPackageName(String packageName){
        patternLockListener.onPinLocked(packageName);
        setAppIcon(packageName);
    }

    public void setWindowBackground(Integer colorVibrant, int displayHeight){
        if(colorVibrant == null){
            colorVibrant = Color.parseColor("#2874F0");
        }
        GradientDrawable drawable = new GradientDrawable();
        int[] colors = {colorVibrant,Color.parseColor("#263238")};
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
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(LockUpSettingsActivity.VIBRATOR_ENABLED_PREFERENCE_KEY,false);
        shouldHidePatternLine = prefs.getBoolean(LockUpSettingsActivity.HIDE_PATTERN_LINE_PREFERENCE_KEY,false);
        if(shouldHidePatternLine){
            patternView.setLinePaintTransparency(0);
        }
        patternPassCode = prefs.getString(AppLockModel.USER_SET_LOCK_PASS_CODE,"noPin");
        salt = prefs.getString(AppLockModel.DEFAULT_APP_BACKGROUND_COLOR_KEY,"noColor");
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

        patternRect = new RectF();
        patternViewParent.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(patternRect.contains(event.getX(),event.getY())){
                    event.setLocation(event.getX()-patternRect.left,event.getY()-patternRect.top);
                    patternView.onTouchEvent(event);
                    return true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    event.setLocation(event.getX()-patternRect.left,event.getY()-patternRect.top);
                    patternView.onTouchEvent(event);
                    return true;
                }
                return true;
            }
        });
    }

    void unRegisterListeners(){
        patternView.setOnPatternChangedListener(null);
        patternViewParent.setOnTouchListener(null);
        patternAnimator.removeAllUpdateListeners();
        if(moPubNativeAd!=null){
            moPubNativeAd.setMoPubNativeEventListener(null);
        }
        appIconView.setImageDrawable(null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if(hasWindowFocus){
            patternRect.set(patternView.getLeft(),patternView.getTop()
                    ,patternView.getRight(),patternView.getBottom());
        }
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

    public void addRenderedAd(View adView, NativeAd nativeAd){
        if(adView !=null && nativeAd != null) {
            nativeAdView = adView;
            moPubNativeAd = nativeAd;
            RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                    , ViewGroup.LayoutParams.WRAP_CONTENT);
            parms.addRule(RelativeLayout.CENTER_IN_PARENT);
            patternAdParent.addView(nativeAdView, parms);
            moPubNativeAd.renderAdView(adView);
            moPubNativeAd.prepare(adView);
            moPubNativeAd.setMoPubNativeEventListener(this);
        }
    }

    @Override
    public void onImpression(View view) {
        if(patternLockListener!=null){
            patternLockListener.onAdImpressed();
        }
    }

    @Override
    public void onClick(View view) {
        if(patternLockListener!=null){
            patternLockListener.onAdClicked();
        }
    }

    @Override
    public void onPatternNodeSelected(int selectedPatternNode) {
        this.selectedPatternNode = this.selectedPatternNode+String.valueOf(selectedPatternNode);
       // Log.d("AppLock","Selected Pattern "+ this.selectedPatternNode);
        patternNodeSelectedCount=patternNodeSelectedCount+1;
        if(isVibratorEnabled){
            patternViewVibrator.vibrate(30);
        }
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        if(patternCompleted && !selectedPatternNode.equals("")){
           // Log.d("PatternLock Confirm",selectedPatternNode);
            if(patternPassCode.equals(validatePassword(selectedPatternNode))){
            //    Log.d("AppLock",selectedPatternNode + " " + patternPassCode);
                patternView.resetPatternView();
                resetPatternData();
                postPatternCompleted();
            } else{
                patternAnimator.start();
            }
        }
    }

    private String validatePassword(String userPass){
        StringBuilder builder = new StringBuilder();
        try {
            byte[] usePassByte = (userPass+salt).getBytes("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = digest.digest(usePassByte);
            for (int i = 0; i < messageDigest.length; ++i) {
                builder.append(Integer.toHexString((messageDigest[i] & 0xFF) | 0x100).substring(1,3));
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    @Override
    public void onErrorStop() {
        if(patternAnimator.isStarted()){
            patternAnimator.end();
            patternAnimator.cancel();
            patternView.resetPatternView();
            resetPatternData();
        }
    }

    void resetPatternData(){
        selectedPatternNode="";
        patternNodeSelectedCount=0;
    }

    private void postPatternCompleted(){
        if(patternLockListener!=null) {
            patternLockListener.onPinUnlocked();
        }
    }

    void startHome(){
        getContext().startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction()!=KeyEvent.ACTION_UP && (event.getKeyCode() != KeyEvent.KEYCODE_BACK)) {
            return super.dispatchKeyEvent(event);
        }
        startHome();
        return true;
    }

    public void removeView(){
        unRegisterListeners();
        patternView.closePatternView();
        if(nativeAdView!=null) {
            patternAdParent.removeView(nativeAdView);
        }
        nativeAdView = null;
        moPubNativeAd = null;
       // mMoPubNative.destroy();
        setPinLockUnlockListener(null);
        context = null;
    }
}

package com.smartfoxitsolutions.lockup.views;

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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

public class LockPatternView extends FrameLayout implements PatternLockView.OnPatternChangedListener{
    Context context;
    private OnPinLockUnlockListener patternLockListener;
    PatternLockView patternView;
    ImageView appIconView;
    private String selectedPatternNode, patternPassCode, salt;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled, shouldHidePatternLine;
    Vibrator patternViewVibrator;
    ValueAnimator patternAnimator;
    private RelativeLayout patternViewParent;

    public LockPatternView(Context context, OnPinLockUnlockListener patternLockListener) {
        super(context);
        this.context = context;
        setPinLockUnlockListener(patternLockListener);
        LayoutInflater.from(context).inflate(R.layout.pattern_lock_activity,this,true);
        patternViewParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_parent_view);
        patternViewVibrator= (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        initAds();
        initializeLockView();
    }



    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.patternLockListener = pinLockListener;
    }

    public void setPackageName(String packageName){
        patternLockListener.onPinLocked(packageName);
        setAppIcon(packageName);
    }

    public void setWindowBackground(int colorVibrant, int displayHeight){
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
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,false);
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

    void initAds(){
        MoPubNative.MoPubNativeNetworkListener moPubNativeListener = new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                Log.d("LockUpMopub","Called onNativeLoad Finger");
                if(context!=null) {
                    View adViewRender = nativeAd.createAdView(context, null);
                    addRenderedAd(adViewRender);
                    nativeAd.renderAdView(adViewRender);
                    nativeAd.prepare(adViewRender);
                    nativeAd.setMoPubNativeEventListener(new NativeAd.MoPubNativeEventListener() {
                        @Override
                        public void onImpression(View view) {
                            Toast.makeText(context,"Impression Will be tracked",Toast.LENGTH_LONG)
                                    .show();
                        }

                        @Override
                        public void onClick(View view) {
                            Toast.makeText(context,"Native ad clicked",Toast.LENGTH_LONG)
                                    .show();
                            postPatternCompleted();
                        }
                    });
                }
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                Log.d("LockUpMopub",errorCode+ " errorcode");
            }

        };

        MoPubNative mMoPubNative = new MoPubNative(context
                ,getResources().getString(R.string.pin_lock_activity_ad_unit_id),moPubNativeListener);

        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.native_ad_sample)
                .mainImageId(R.id.native_ad_main_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .callToActionId(R.id.native_ad_call_to_action)
                .build();

        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        mMoPubNative.registerAdRenderer(adRenderer);
        mMoPubNative.makeRequest();
    }

    void addRenderedAd(View adView){
        int marginTop = Math.round(DimensionConverter.convertDpToPixel(20f,context.getApplicationContext()));
        FrameLayout.LayoutParams parms = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT);
        parms.topMargin = marginTop;
        parms.gravity = Gravity.TOP|Gravity.CENTER;
        this.addView(adView,parms);
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
        patternLockListener.onPinUnlocked();
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

    public void removeView(){
        unRegisterListeners();
        setPinLockUnlockListener(null);
        context = null;
    }
}

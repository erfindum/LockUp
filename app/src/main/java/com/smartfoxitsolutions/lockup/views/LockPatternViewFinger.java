package com.smartfoxitsolutions.lockup.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

/**
 * Created by RAAJA on 27-10-2016.
 */

public class LockPatternViewFinger extends FrameLayout implements PatternLockView.OnPatternChangedListener{
    Context context;
    private OnPinLockUnlockListener patternLockListener;
    private OnFingerScannerCancelListener fingerCanceledListener;

    private PatternLockView patternView;
    private ImageView appIconView;
    private String selectedPatternNode;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled;
    private Vibrator patternViewVibrator;
    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancelSignal;
    private ValueAnimator patternAnimator;
    private String packageName;
    private long patternPassCode;
    private RelativeLayout patternViewParent;
    private AppCompatImageView fingerPrintIcon;
    private TextView fingerPrintInfoText;
    private AppCompatImageButton fingerPrintSwitchButton, pinPatternSwitchButton;
    private boolean isFingerPrintActive;
    private int noOfAttempts,noOfNoisyAttempts;
    private ValueAnimator animatorMain, animatorFingerError;

    public LockPatternViewFinger(Context context, OnPinLockUnlockListener patternLockListener
            ,OnFingerScannerCancelListener fingerCanceledListener, boolean isFingerPrintActive) {
        super(context);
        this.context = context;
        setPinLockUnlockListener(patternLockListener);
        setFingerCanceledListener(fingerCanceledListener);
        patternLockListener.onPinLocked();
        this.isFingerPrintActive = isFingerPrintActive;
        LayoutInflater.from(context).inflate(R.layout.pattern_lock_activity_finger,this,true);
        patternViewParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_finger_parent_view);
        patternViewVibrator= (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        fingerprintManager = FingerprintManagerCompat.from(context);
        cancelSignal = new CancellationSignal();
        initializeLockView();
    }



    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.patternLockListener = pinLockListener;
    }

    void setFingerCanceledListener(OnFingerScannerCancelListener fingerListener){
        this.fingerCanceledListener = fingerListener;
    }

    public void setPackageName(String packageName){
        this.packageName = packageName;
        setAppIcon(packageName);
    }

    public void setWindowBackground(int colorVibrant, int displayHeight){
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
        patternView = (PatternLockView) findViewById(R.id.pattern_lock_activity_finger_pattern_view);
        fingerPrintIcon = (AppCompatImageView) findViewById(R.id.pattern_lock_activity_finger_image_main);
        fingerPrintInfoText = (TextView) findViewById(R.id.pattern_lock_activity_finger_info);
        fingerPrintSwitchButton = (AppCompatImageButton) findViewById(R.id.pattern_lock_activity_finger_switch_finger);
        pinPatternSwitchButton = (AppCompatImageButton) findViewById(R.id.pattern_lock_activity_finger_switch_keyboard);
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_finger_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        patternPassCode = prefs.getLong(AppLockModel.USER_SET_LOCK_PASS_CODE,0);
        registerListeners();
        setPatternAnimator();
        if(isFingerPrintActive){
            pinPatternSwitchButton.setVisibility(VISIBLE);
            fingerPrintSwitchButton.setVisibility(INVISIBLE);
            switchToFingerView();
        }else{
            fingerPrintSwitchButton.setVisibility(INVISIBLE);
            pinPatternSwitchButton.setVisibility(INVISIBLE);
            switchToPatternView();
        }
        initAds();
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
        fingerPrintSwitchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToFingerView();
                fingerPrintSwitchButton.setVisibility(INVISIBLE);
                pinPatternSwitchButton.setVisibility(VISIBLE);
            }
        });

        pinPatternSwitchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToPatternView();
                fingerPrintSwitchButton.setVisibility(VISIBLE);
                pinPatternSwitchButton.setVisibility(INVISIBLE);
            }
        });
    }

    void unRegisterListeners(){
        patternView.setOnPatternChangedListener(null);
        pinPatternSwitchButton.setOnClickListener(null);
        fingerPrintSwitchButton.setOnClickListener(null);
    }

    private void switchToPatternView(){
        fingerPrintIcon.setVisibility(INVISIBLE);
        fingerPrintInfoText.setVisibility(INVISIBLE);
        patternView.setVisibility(VISIBLE);
        patternView.setEnabled(true);
        fingerPrintIcon.setEnabled(false);
        fingerPrintInfoText.setEnabled(false);
        cancelSignal.cancel();
    }

    private void switchToFingerView(){
        patternView.setVisibility(INVISIBLE);
        fingerPrintIcon.setVisibility(VISIBLE);
        fingerPrintInfoText.setVisibility(VISIBLE);
        fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
        fingerPrintIcon.setEnabled(true);
        fingerPrintInfoText.setEnabled(true);
        patternView.setEnabled(false);
        if(cancelSignal.isCanceled()){
            fingerCanceledListener.onFingerScannerCanceled();
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

    void initAds(){
        MoPubNative.MoPubNativeNetworkListener moPubNativeListener = new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                Log.d("LockUpMopub","Called onNativeLoad Finger");
                View adViewRender = nativeAd.createAdView(context,null);
                addRenderedAd(adViewRender);
                nativeAd.renderAdView(adViewRender);
                nativeAd.prepare(adViewRender);
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
                .iconImageId(R.id.native_ad_icon_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .build();

        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        mMoPubNative.registerAdRenderer(adRenderer);
        mMoPubNative.makeRequest();
    }

    void addRenderedAd(View adView){
        int height  = Math.round(DimensionConverter.convertDpToPixel(270f,context.getApplicationContext()));
        int width = Math.round(DimensionConverter.convertDpToPixel(210f,context.getApplicationContext()));
        RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(width,height);
        parms.addRule(RelativeLayout.ABOVE,R.id.pattern_lock_activity_finger_app_icon_view);
        parms.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
        patternViewParent.addView(adView,parms);
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

    @TargetApi(23)
    void analyzeFingerPrint(){
        final Resources res = getResources();
        if((context.checkSelfPermission("android.permission.USE_FINGERPRINT") ==PackageManager.PERMISSION_GRANTED)){
            if(fingerprintManager.isHardwareDetected()){
                if(fingerprintManager.hasEnrolledFingerprints()){
                    fingerprintManager.authenticate(null, 0, cancelSignal, new FingerprintManagerCompat.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errMsgId, CharSequence errString) {
                            super.onAuthenticationError(errMsgId, errString);
                            if(!cancelSignal.isCanceled()) {
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                                    errorSwitchPatternView();
                                    animateFingerPrintToPattern(res.getString(R.string.finger_print_lock_main_failed_locked_pattern));

                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE) {
                                    errorSwitchPatternView();
                                    animateFingerPrintToPattern(res.getString(R.string.finger_print_lock_main_error_hardware_unavailable_pattern));

                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS) {
                                    fingerPrintInfoText.setText(res.getString(R.string.finger_print_lock_main_error_unable_process));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT) {
                                    fingerPrintInfoText.setText(res.getString(R.string.finger_print_lock_main_error_timeout));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                                    errorSwitchPatternView();
                                    animateFingerPrintToPattern(res.getString(R.string.finger_print_lock_main_error_cancelled_pattern));

                                }
                            }
                        }

                        @Override
                        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                            super.onAuthenticationHelp(helpMsgId, helpString);
                            if(helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_INSUFFICIENT){
                                if(noOfNoisyAttempts<2){
                                    animateMainInfo(res.getString(R.string.finger_print_lock_main_help_noisy_general));
                                    ++noOfNoisyAttempts;
                                }
                                if(noOfNoisyAttempts >= 2){
                                    animateMainInfo(res.getString(R.string.finger_print_lock_main_help_dirty));
                                    noOfNoisyAttempts = 0;
                                }
                            }
                            if(helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_IMAGER_DIRTY){
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_help_dirty));
                            }
                            if(helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_PARTIAL){
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_help_partial));
                            }
                            if(helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST){
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_help_fast));
                            }

                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            postPatternCompleted(packageName);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            if(noOfAttempts<2){
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_failed_general));
                                ++noOfAttempts;
                            }
                            if(noOfAttempts >= 2){
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_failed_attempt_two));
                            }
                        }
                    },null);
                }
                else{
                    errorSwitchPatternView();
                }

            }else{
                errorSwitchPatternView();
            }
        }
    }

    void errorSwitchPatternView(){
        fingerPrintSwitchButton.setVisibility(INVISIBLE);
        pinPatternSwitchButton.setVisibility(INVISIBLE);
        switchToPatternView();
    }

    void animateFingerPrintToPattern(final String infoText){
        animatorFingerError = ValueAnimator.ofFloat(0,1f);
        animatorFingerError.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fingerPrintInfoText.setVisibility(INVISIBLE);
                patternView.setVisibility(VISIBLE);
                fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                patternView.setVisibility(INVISIBLE);
                fingerPrintInfoText.setVisibility(VISIBLE);
                fingerPrintInfoText.setText(infoText);
                if(animatorMain!=null && animatorMain.isRunning()){
                    animatorMain.removeAllListeners();
                    animatorMain.end();
                    animatorMain.cancel();
                }
            }
        });
        animatorFingerError.setInterpolator(new LinearInterpolator());
        animatorFingerError.setDuration(2500);
        animatorFingerError.start();
    }

    void animateMainInfo(final String infoText){
        animatorMain = ValueAnimator.ofFloat(0,1f);
        animatorMain.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                fingerPrintInfoText.setText(infoText);
            }
        });
        animatorMain.setInterpolator(new LinearInterpolator());
        animatorMain.setDuration(2500);
        animatorMain.start();
    }

    public void updateCancelSignal(){
        if(isFingerPrintActive){
            cancelSignal = null;
            cancelSignal = new CancellationSignal();
            analyzeFingerPrint();
        }
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

    public void removeView(){
        unRegisterListeners();
        setPinLockUnlockListener(null);
        setFingerCanceledListener(null);
        if(cancelSignal!=null && !cancelSignal.isCanceled()){
            cancelSignal.cancel();
        }
        context = null;
    }
}
package com.smartfoxitsolutions.lockup.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.MainLockActivity;
import com.smartfoxitsolutions.lockup.R;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by RAAJA on 07-12-2016.
 */

public class MainPatternView extends FrameLayout implements PatternLockView.OnPatternChangedListener {
    MainLockActivity activity;
    private OnPinLockUnlockListener patternLockListener;

    private PatternLockView patternView;
    private String selectedPatternNode,patternPassCode, salt;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled, shouldHidePatternLine;
    private Vibrator patternViewVibrator;
    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancelSignal;
    private ValueAnimator patternAnimator;
    private AppCompatImageView fingerPrintIcon;
    private TextView fingerPrintInfoText;
    private AppCompatImageButton fingerPrintSwitchButton, pinPatternSwitchButton, forgotPasswordMenu;
    private Button forgotPasswordButton;
    private boolean isFingerPrintActive, isForgotPasswordVisible;
    private int noOfAttempts,noOfNoisyAttempts;
    private ValueAnimator animatorMain, animatorFingerError;

    public MainPatternView(MainLockActivity activity, OnPinLockUnlockListener patternLockListener
                            , boolean isFingerPrintActive) {
        super(activity);
        this.activity = activity;
        setPinLockUnlockListener(patternLockListener);
        this.isFingerPrintActive = isFingerPrintActive;
        LayoutInflater.from(activity).inflate(R.layout.main_pattern_lock,this,true);
        patternViewVibrator= (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        fingerprintManager = FingerprintManagerCompat.from(activity);
        cancelSignal = new CancellationSignal();
        initializeLockView();
    }

    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.patternLockListener = pinLockListener;
    }

    public void setActivityBackground(int heightPixels){
        GradientDrawable drawable = new GradientDrawable();
        int[] colors = {Color.parseColor("#448AFF"), Color.parseColor("#1565C0")};
        drawable.setColors(colors);
        float radius = Math.round(heightPixels*.95);
        drawable.setGradientRadius(radius);
        drawable.setGradientCenter(0.5f,1f);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setShape(GradientDrawable.RECTANGLE);
        setBackground(drawable);
    }

    void initializeLockView(){
        patternView = (PatternLockView) findViewById(R.id.main_pattern_lock_pattern_view);
        fingerPrintIcon = (AppCompatImageView) findViewById(R.id.main_pattern_lock_finger_image);
        fingerPrintInfoText = (TextView) findViewById(R.id.main_pattern_lock_finger_info_text);
        fingerPrintSwitchButton = (AppCompatImageButton) findViewById(R.id.main_pattern_lock_finger_icon);
        pinPatternSwitchButton = (AppCompatImageButton) findViewById(R.id.main_pattern_lock_keyboard_icon);
        forgotPasswordMenu = (AppCompatImageButton) findViewById(R.id.main_pattern_lock_menu);
        forgotPasswordButton = (Button) findViewById(R.id.main_pattern_lock_forgot_button);
        selectedPatternNode = "";
        SharedPreferences prefs = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        shouldHidePatternLine = prefs.getBoolean(LockUpSettingsActivity.HIDE_PATTERN_LINE_PREFERENCE_KEY,false);
        if(shouldHidePatternLine){
            patternView.setLinePaintTransparency(0);
        }
        patternPassCode = prefs.getString(AppLockModel.USER_SET_LOCK_PASS_CODE,"noPin");
        salt = prefs.getString(AppLockModel.DEFAULT_APP_BACKGROUND_COLOR_KEY,"noColor");

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

        forgotPasswordMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isForgotPasswordVisible) {
                    isForgotPasswordVisible = true;
                    ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(forgotPasswordButton,
                            PropertyValuesHolder.ofFloat("scaleX",0.0f,1.0f),
                            PropertyValuesHolder.ofFloat("scaleY",0.0f,1.0f),
                            PropertyValuesHolder.ofFloat("alpha",0.0f,1.0f));
                    animator.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            forgotPasswordButton.setVisibility(VISIBLE);
                        }
                    });
                    animator.start();
                }
            }
        });

        forgotPasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isForgotPasswordVisible) {
                    activity.startResetActivity();
                    forgotPasswordButton.setVisibility(INVISIBLE);
                    isForgotPasswordVisible=false;
                }
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
            updateCancelSignal();
        }else {
            analyzeFingerPrint();
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

    @Override
    public void onPatternNodeSelected(int selectedPatternNode) {
        this.selectedPatternNode = this.selectedPatternNode+String.valueOf(selectedPatternNode);
       // Log.d("PatternLock","Selected Pattern "+ this.selectedPatternNode);
        patternNodeSelectedCount=patternNodeSelectedCount+1;
        if(isVibratorEnabled){
            patternViewVibrator.vibrate(30);
        }
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        if(patternCompleted && !selectedPatternNode.equals("")){
            //Log.d("PatternLock","Pattern confirm " +selectedPatternNode);
            if(patternPassCode.equals(validatePassword(selectedPatternNode))){
               // Log.d("PatternLock",selectedPatternNode + " " + patternPassCode);
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

    @TargetApi(23)
    void analyzeFingerPrint(){
        final Resources res = getResources();
        if((activity.checkSelfPermission("android.permission.USE_FINGERPRINT") == PackageManager.PERMISSION_GRANTED)){
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
                            postPatternCompleted();
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

    private void postPatternCompleted(){
        patternLockListener.onPinUnlocked();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isForgotPasswordVisible){
            isForgotPasswordVisible = false;
            forgotPasswordButton.setVisibility(INVISIBLE);
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void removeView(){
        unRegisterListeners();
        setPinLockUnlockListener(null);
        if(cancelSignal!=null && !cancelSignal.isCanceled()){
            cancelSignal.cancel();
        }
        activity = null;
    }
}

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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * Created by RAAJA on 26-10-2016.
 */

public class LockPinViewFinger extends FrameLayout implements View.OnClickListener{
    Context context;

    private ImageView appIconView;
    private AppCompatButton button_digit_one,button_digit_two,button_digit_three,button_digit_four,button_digit_five;
    private AppCompatButton button_digit_six,button_digit_seven,button_digit_eight,button_digit_nine,button_digit_zero,clear_pin_button;
    private ImageView img_trigger_one,img_trigger_two,img_trigger_three,img_trigger_four;
    private RelativeLayout pinLayout,triggerLayout;
    private Typeface digitTypFace;
    private Vibrator pinDigitVibrator;
    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancelSignal;
    private RelativeLayout pinViewParent;
    private AppCompatImageView fingerPrintIcon;
    private TextView fingerPrintInfoText;
    private AppCompatImageButton fingerPrintSwitchButton, pinPatternSwitchButton;
    private boolean isFingerPrintActive;
    int noOfAttempts,noOfNoisyAttempts;
    private ValueAnimator animatorFingerError,animatorMain;

    private String selectedPin;
    private int pinDigitCount;
    private boolean isVibratorEnabled, isErrorConfirmed;

    ValueAnimator digitOneAnimator,digitTwoAnimator, digitThreeAnimator, digitFourAnimator,digitFiveAnimator, digitSixAnimator
            ,digitSevenAnimator,digitEightAnimator,digitNineAnimator,digitZeroAnimator;
    ValueAnimator triggerAnimator;

    private String packageName;
    long pinPassCode;

    private OnPinLockUnlockListener pinLockListener;
    private OnFingerScannerCancelListener fingerCanceledListener;

    public LockPinViewFinger(Context context, OnPinLockUnlockListener pinLockListener
            ,OnFingerScannerCancelListener fingerScannerCancelListener,boolean isFingerPrintActive) {
        super(context);
        this.context = context;
        this.isFingerPrintActive = isFingerPrintActive;
        setPinLockUnlockListener(pinLockListener);
        setFingerCanceledListener(fingerScannerCancelListener);
        pinLockListener.onPinLocked();
        LayoutInflater.from(context).inflate(R.layout.pin_lock_activity_finger,this,true);
        pinViewParent = (RelativeLayout) findViewById(R.id.pin_lock_activity_finger_parent);
        pinDigitVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        fingerprintManager = FingerprintManagerCompat.from(context);
        cancelSignal = new CancellationSignal();
        initAds();
        initializeLockView();
    }

    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.pinLockListener = pinLockListener;
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
        int[] colors = {colorVibrant,Color.parseColor("#263238")};
        drawable.setColors(colors);
        float radius = Math.round(displayHeight*.95);
        drawable.setGradientRadius(radius);
        drawable.setGradientCenter(0.5f,1f);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setShape(GradientDrawable.RECTANGLE);
        pinViewParent.setBackground(drawable);
    }

    void initializeLockView(){
        pinLayout = (RelativeLayout) findViewById(R.id.pin_lock_activity_finger_digit_group);
        triggerLayout = (RelativeLayout) findViewById(R.id.pin_lock_activity_finger_trigger_group);
        fingerPrintIcon = (AppCompatImageView) findViewById(R.id.pin_lock_activity_finger_image_main);
        fingerPrintInfoText = (TextView) findViewById(R.id.pin_lock_activity_finger_info);
        fingerPrintSwitchButton = (AppCompatImageButton) findViewById(R.id.pin_lock_activity_finger_switch_finger);
        pinPatternSwitchButton = (AppCompatImageButton) findViewById(R.id.pin_lock_activity_finger_switch_keyboard);
        SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        selectedPin = "";
        pinPassCode = prefs.getLong(AppLockModel.USER_SET_LOCK_PASS_CODE,0);
        String adAppId = getResources().getString(R.string.pin_lock_activity_ad_app_id);
        digitTypFace = Typeface.createFromAsset(context.getAssets(),"fonts/arquitectabook.ttf");
        appIconView = (ImageView) findViewById(R.id.pin_lock_activity_finger_app_icon_view);
        inflatePinViews();
    }

    void setAppIcon(String packageName){
        try{
            Drawable appIcon =  context.getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(appIcon);
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
            Log.e(AppLockingService.TAG,packageName + " Not Found");
        }
    }

    void inflatePinViews(){
        button_digit_one = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_one);
        button_digit_two = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_two);
        button_digit_three = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_three);
        button_digit_four = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_four);
        button_digit_five = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_five);
        button_digit_six = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_six);
        button_digit_seven= (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_seven);
        button_digit_eight = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_eight);
        button_digit_nine = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_nine);
        button_digit_zero = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_zero);

        img_trigger_one = (ImageView) findViewById(R.id.pin_lock_activity_finger_trigger_one);
        img_trigger_two = (ImageView) findViewById(R.id.pin_lock_activity_finger_trigger_two);
        img_trigger_three = (ImageView) findViewById(R.id.pin_lock_activity_finger_trigger_three);
        img_trigger_four = (ImageView) findViewById(R.id.pin_lock_activity_finger_trigger_four);

        clear_pin_button = (AppCompatButton) findViewById(R.id.pin_lock_activity_finger_digit_clear);

        button_digit_one.setTypeface(digitTypFace);
        button_digit_two.setTypeface(digitTypFace);
        button_digit_three.setTypeface(digitTypFace);
        button_digit_four.setTypeface(digitTypFace);
        button_digit_five.setTypeface(digitTypFace);
        button_digit_six.setTypeface(digitTypFace);
        button_digit_seven.setTypeface(digitTypFace);
        button_digit_eight.setTypeface(digitTypFace);
        button_digit_nine.setTypeface(digitTypFace);
        button_digit_zero.setTypeface(digitTypFace);

        registerListeners();
        setPinAnimation();
        if(isFingerPrintActive){
            pinPatternSwitchButton.setVisibility(VISIBLE);
            fingerPrintSwitchButton.setVisibility(INVISIBLE);
            switchToFingerView();
        }else{
            fingerPrintSwitchButton.setVisibility(INVISIBLE);
            pinPatternSwitchButton.setVisibility(INVISIBLE);
            switchToPinView();
        }
    }

    void registerListeners(){
        button_digit_one.setOnClickListener(this);
        button_digit_two.setOnClickListener(this);
        button_digit_three.setOnClickListener(this);
        button_digit_four.setOnClickListener(this);
        button_digit_five.setOnClickListener(this);
        button_digit_six.setOnClickListener(this);
        button_digit_seven.setOnClickListener(this);
        button_digit_eight.setOnClickListener(this);
        button_digit_nine.setOnClickListener(this);
        button_digit_zero.setOnClickListener(this);
        clear_pin_button.setOnClickListener(this);
        fingerPrintSwitchButton.setOnClickListener(this);
        pinPatternSwitchButton.setOnClickListener(this);
    }

    void unregisterListeners(){
        button_digit_one.setOnClickListener(null);
        button_digit_two.setOnClickListener(null);
        button_digit_three.setOnClickListener(null);
        button_digit_four.setOnClickListener(null);
        button_digit_five.setOnClickListener(null);
        button_digit_six.setOnClickListener(null);
        button_digit_seven.setOnClickListener(null);
        button_digit_eight.setOnClickListener(null);
        button_digit_nine.setOnClickListener(null);
        button_digit_zero.setOnClickListener(null);
        clear_pin_button.setOnClickListener(null);
        fingerPrintSwitchButton.setOnClickListener(null);
        pinPatternSwitchButton.setOnClickListener(null);
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.pin_lock_activity_finger_digit_one:
                pinClicked("1");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_two:
                pinClicked("2");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_three:
                pinClicked("3");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_four:
                pinClicked("4");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_five:
                pinClicked("5");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_six:
                pinClicked("6");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_seven:
                pinClicked("7");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_eight:
                pinClicked("8");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_nine:
                pinClicked("9");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_zero:
                pinClicked("0");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_digit_clear:
                if(selectedPin!=null){
                    clearPin();
                }
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_finger_switch_finger:
                switchToFingerView();
                fingerPrintSwitchButton.setVisibility(INVISIBLE);
                pinPatternSwitchButton.setVisibility(VISIBLE);
                break;
            case R.id.pin_lock_activity_finger_switch_keyboard:
                switchToPinView();
                fingerPrintSwitchButton.setVisibility(VISIBLE);
                pinPatternSwitchButton.setVisibility(INVISIBLE);
                break;
        }
    }

    private void switchToPinView(){
        fingerPrintIcon.setVisibility(INVISIBLE);
        fingerPrintInfoText.setVisibility(INVISIBLE);
        pinLayout.setVisibility(VISIBLE);
        triggerLayout.setVisibility(VISIBLE);
        pinLayout.setEnabled(true);
        triggerLayout.setEnabled(true);
        fingerPrintIcon.setEnabled(false);
        fingerPrintInfoText.setEnabled(false);
        cancelSignal.cancel();
    }

    private void switchToFingerView(){
        pinLayout.setVisibility(INVISIBLE);
        triggerLayout.setVisibility(INVISIBLE);
        fingerPrintIcon.setVisibility(VISIBLE);
        fingerPrintInfoText.setVisibility(VISIBLE);
        fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
        fingerPrintIcon.setEnabled(true);
        fingerPrintInfoText.setEnabled(true);
        pinLayout.setEnabled(false);
        triggerLayout.setEnabled(false);
        if(cancelSignal.isCanceled()){
            fingerCanceledListener.onFingerScannerCanceled();
        }
    }

    void vibrateDevice(){
        if(isVibratorEnabled){
            pinDigitVibrator.vibrate(30);
        }
    }

    void setPinAnimation(){

        //Digit One animation

        digitOneAnimator = ValueAnimator.ofFloat(0,1f);
        digitOneAnimator.setInterpolator(new OvershootInterpolator());
        digitOneAnimator.setDuration(300);
        digitOneAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_one.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_one_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_one.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_one_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitOneAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_one.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_one_selected);
                button_digit_one.setScaleX(val);
                button_digit_one.setScaleY(val);
            }
        });

        //Digit Two Animation

        digitTwoAnimator = ValueAnimator.ofFloat(0,1f);
        digitTwoAnimator.setInterpolator(new OvershootInterpolator());
        digitTwoAnimator.setDuration(300);
        digitTwoAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_two.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_two_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_two.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_two_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitTwoAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_two.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_two_selected);
                button_digit_two.setScaleX(val);
                button_digit_two.setScaleY(val);
            }
        });

        // Digit Three Animation

        digitThreeAnimator = ValueAnimator.ofFloat(0,1f);
        digitThreeAnimator.setInterpolator(new OvershootInterpolator());
        digitThreeAnimator.setDuration(300);
        digitThreeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_three.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_three_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_three.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_three_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitThreeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_three.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_three_selected);
                button_digit_three.setScaleX(val);
                button_digit_three.setScaleY(val);
            }
        });

        // Digit Four Animation

        digitFourAnimator = ValueAnimator.ofFloat(0,1f);
        digitFourAnimator.setInterpolator(new OvershootInterpolator());
        digitFourAnimator.setDuration(300);
        digitFourAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_four.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_four_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_four.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_four_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitFourAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_four.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_four_selected);
                button_digit_four.setScaleX(val);
                button_digit_four.setScaleY(val);
            }
        });

        // Digit Five animation

        digitFiveAnimator = ValueAnimator.ofFloat(0,1f);
        digitFiveAnimator.setInterpolator(new OvershootInterpolator());
        digitFiveAnimator.setDuration(300);
        digitFiveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_five.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_five_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_five.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_five_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitFiveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_five.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_five_selected);
                button_digit_five.setScaleX(val);
                button_digit_five.setScaleY(val);
            }
        });

        // Digit Six Animation

        digitSixAnimator = ValueAnimator.ofFloat(0,1f);
        digitSixAnimator.setInterpolator(new OvershootInterpolator());
        digitSixAnimator.setDuration(300);
        digitSixAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_six.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_six_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_six.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_six_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitSixAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_six.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_six_selected);
                button_digit_six.setScaleX(val);
                button_digit_six.setScaleY(val);
            }
        });

        // Digit Seven Animation

        digitSevenAnimator = ValueAnimator.ofFloat(0,1f);
        digitSevenAnimator.setInterpolator(new OvershootInterpolator());
        digitSevenAnimator.setDuration(300);
        digitSevenAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_seven.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_seven_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_seven.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_seven_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitSevenAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_seven.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_seven_selected);
                button_digit_seven.setScaleX(val);
                button_digit_seven.setScaleY(val);
            }
        });

        // Digit Eight Animation

        digitEightAnimator = ValueAnimator.ofFloat(0,1f);
        digitEightAnimator.setInterpolator(new OvershootInterpolator());
        digitEightAnimator.setDuration(300);
        digitEightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_eight.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_eight_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_eight.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_eight_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitEightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_eight.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_eight_selected);
                button_digit_eight.setScaleX(val);
                button_digit_eight.setScaleY(val);
            }
        });

        // Digit nine Animation

        digitNineAnimator = ValueAnimator.ofFloat(0,1f);
        digitNineAnimator.setInterpolator(new OvershootInterpolator());
        digitNineAnimator.setDuration(300);
        digitNineAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_nine.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_nine_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_nine.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_nine_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitNineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_nine.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_nine_selected);
                button_digit_nine.setScaleX(val);
                button_digit_nine.setScaleY(val);
            }
        });

        // Digit zero Animation

        digitZeroAnimator = ValueAnimator.ofFloat(0,1f);
        digitZeroAnimator.setInterpolator(new OvershootInterpolator());
        digitZeroAnimator.setDuration(300);
        digitZeroAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                button_digit_zero.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_zero_normal);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_zero.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_zero_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitZeroAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_zero.setBackgroundResource(R.drawable.img_pin_lock_activity_digit_zero_selected);
                button_digit_zero.setScaleX(val);
                button_digit_zero.setScaleY(val);
            }
        });


        triggerAnimator = ValueAnimator.ofFloat(0,1f);
        triggerAnimator.setDuration(300).setInterpolator(new OvershootInterpolator());
        triggerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                for(int i =1;i<=4;i++){
                    getTrigger(i).setBackgroundResource(R.drawable.img_pin_trigger_error);
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                resetPinView();
                isErrorConfirmed = false;
            }
        });

        triggerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                for(int i =1;i<=4;i++){
                    getTrigger(i).setScaleY(val);
                    getTrigger(i).setScaleX(val);
                }
            }
        });
    }


    ValueAnimator getDigitAnimators(String digit){
        if(Integer.parseInt(digit)>9 || Integer.parseInt(digit)<0){
            throw new IllegalArgumentException("Digit cannot be less than 0 or more than 9");
        }
        switch(digit){
            case "1":
                return digitOneAnimator;
            case "2":
                return digitTwoAnimator;
            case "3":
                return digitThreeAnimator;
            case "4":
                return digitFourAnimator;
            case "5":
                return digitFiveAnimator;
            case "6":
                return digitSixAnimator;
            case "7":
                return digitSevenAnimator;
            case "8":
                return digitEightAnimator;
            case "9":
                return digitNineAnimator;
            case "0":
                return digitZeroAnimator;
        }
        return null;

    }

    ImageView getTrigger(int trigger){
        if(trigger>4 || trigger<1){
            throw new IllegalArgumentException("Trigger no cannot be less than 0 or more than 4");
        }
        switch (trigger){
            case 1:
                return img_trigger_one;
            case 2:
                return img_trigger_two;
            case 3:
                return img_trigger_three;
            case 4:
                return img_trigger_four;
        }
        return null;
    }

    void pinClicked(String digit){
        Log.d("AppLock", "PinClicked .......");

        if(!isErrorConfirmed) {
            if (pinDigitCount < 3) {
                pinDigitCount += 1;
                selectedPin += digit;
                getTrigger(pinDigitCount).setBackgroundResource(R.drawable.img_pin_trigger_selected);
                getDigitAnimators(digit).start();
            } else if (pinDigitCount == 3) {
                selectedPin += digit;
                pinDigitCount += 1;
                getTrigger(pinDigitCount).setBackgroundResource(R.drawable.img_pin_trigger_selected);
                getDigitAnimators(digit).start();
                long confirmedPin = Long.parseLong(selectedPin)*55439;
                if (!selectedPin.equals("") && pinPassCode == confirmedPin) {
                    Log.d("AppLock", "Passcode is " + selectedPin + "  " + pinPassCode);
                    resetPinView();
                    postPinCompleted(packageName);
                } else if (pinPassCode != confirmedPin) {
                    triggerAnimator.start();
                    if(isVibratorEnabled){
                        pinDigitVibrator.vibrate(30);
                    }
                    isErrorConfirmed = true;

                }
            }
        }
    }

    void resetPinView(){
        for(int i =1;i<=4;i++){
            getTrigger(i).setBackgroundResource(R.drawable.img_pin_trigger_normal);
        }
        pinDigitCount=0;
        selectedPin="";

    }


    void clearPin(){
        if(!selectedPin.isEmpty() && selectedPin.length()>=0){
            getTrigger(selectedPin.length()).setBackgroundResource(R.drawable.img_pin_trigger_normal);
            selectedPin = selectedPin.substring(0,selectedPin.length()-1);
            pinDigitCount-=1;
            Log.d("PatternLock","Cleared : " +selectedPin);
        }
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
                            postPinCompleted(packageName);
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
                                    errorSwitchPinView();
                                    animateFingerPrintToPin(res.getString(R.string.finger_print_lock_main_failed_locked_pin));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE) {
                                    errorSwitchPinView();
                                    animateFingerPrintToPin(res.getString(R.string.finger_print_lock_main_error_hardware_unavailable_pin));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS) {
                                    animateMainInfo(res.getString(R.string.finger_print_lock_main_error_unable_process));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT) {
                                    animateMainInfo(res.getString(R.string.finger_print_lock_main_error_timeout));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                                    errorSwitchPinView();
                                   animateFingerPrintToPin(res.getString(R.string.finger_print_lock_main_error_cancelled_pin));
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
                            postPinCompleted(packageName);
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
                    errorSwitchPinView();
                }

            }else{
                errorSwitchPinView();
            }
        }
    }

    void errorSwitchPinView(){
        fingerPrintSwitchButton.setVisibility(INVISIBLE);
        pinPatternSwitchButton.setVisibility(INVISIBLE);
        switchToPinView();
    }

    void animateFingerPrintToPin(final String infoText){
        animatorFingerError = ValueAnimator.ofFloat(0,1f);
                animatorFingerError.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        fingerPrintInfoText.setVisibility(INVISIBLE);
                        triggerLayout.setVisibility(VISIBLE);
                        pinLayout.setVisibility(VISIBLE);
                        fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        triggerLayout.setVisibility(INVISIBLE);
                        pinLayout.setVisibility(INVISIBLE);
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

    private void postPinCompleted(String packageUnlockedName){
        pinLockListener.onPinUnlocked(packageUnlockedName);
    }

    void startHome(){
        context.startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("AppLock","Called onMeasure");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d("AppLock","Called OnLayout");
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

    public void removeView(){
        unregisterListeners();
        setPinLockUnlockListener(null);
        if(cancelSignal!=null && !cancelSignal.isCanceled()){
            cancelSignal.cancel();
        }
        context = null;
    }
}

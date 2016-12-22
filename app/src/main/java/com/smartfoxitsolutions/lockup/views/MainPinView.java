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
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

public class MainPinView extends FrameLayout implements View.OnClickListener{
    MainLockActivity activity;
    private AppCompatButton button_digit_one,button_digit_two,button_digit_three,button_digit_four,button_digit_five;
    private AppCompatButton button_digit_six,button_digit_seven,button_digit_eight,button_digit_nine,button_digit_zero,clear_pin_button;
    private ImageView img_trigger_one,img_trigger_two,img_trigger_three,img_trigger_four;
    private RelativeLayout pinLayout,triggerLayout;
    private Typeface digitTypFace;
    private Vibrator pinDigitVibrator;
    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancelSignal;
    private AppCompatImageView fingerPrintIcon;
    private TextView fingerPrintInfoText;
    private AppCompatImageButton fingerPrintSwitchButton, pinPatternSwitchButton, forgotPasswordMenu;
    private Button forgotPasswordButton;
    private boolean isFingerPrintActive, isForgotPasswordVisible;
    int noOfAttempts,noOfNoisyAttempts;
    private ValueAnimator animatorFingerError,animatorMain;

    private String selectedPin,pinPassCode,salt;
    private int pinDigitCount;
    private boolean isVibratorEnabled, isErrorConfirmed,shouldHidePinTouch;

    ValueAnimator digitOneAnimator,digitTwoAnimator, digitThreeAnimator, digitFourAnimator,digitFiveAnimator, digitSixAnimator
            ,digitSevenAnimator,digitEightAnimator,digitNineAnimator,digitZeroAnimator;
    ValueAnimator triggerAnimator;

    private OnPinLockUnlockListener pinLockListener;
    public MainPinView(MainLockActivity activity,OnPinLockUnlockListener pinLockListener
                        ,boolean isFingerPrintActive) {
        super(activity);
        this.activity = activity;
        this.isFingerPrintActive = isFingerPrintActive;
        setPinLockUnlockListener(pinLockListener);
        LayoutInflater.from(activity).inflate(R.layout.main_pin_lock,this,true);
        pinDigitVibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        fingerprintManager = FingerprintManagerCompat.from(activity);
        cancelSignal = new CancellationSignal();
        initializeLockView();
    }

    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener){
        this.pinLockListener = pinLockListener;
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
        pinLayout = (RelativeLayout) findViewById(R.id.main_pin_lock_digit_group);
        triggerLayout = (RelativeLayout) findViewById(R.id.main_pin_lock_trigger_group);
        fingerPrintIcon = (AppCompatImageView) findViewById(R.id.main_pin_lock_finger_image);
        fingerPrintInfoText = (TextView) findViewById(R.id.main_pin_lock_finger_info_text);
        fingerPrintSwitchButton = (AppCompatImageButton) findViewById(R.id.main_pin_lock_finger_icon);
        pinPatternSwitchButton = (AppCompatImageButton) findViewById(R.id.main_pin_lock_keyboard_icon);
        SharedPreferences prefs = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(LockUpSettingsActivity.VIBRATOR_ENABLED_PREFERENCE_KEY,false);
        shouldHidePinTouch = prefs.getBoolean(LockUpSettingsActivity.HIDE_PIN_TOUCH_PREFERENCE_KEY,false);
        forgotPasswordMenu = (AppCompatImageButton) findViewById(R.id.main_pin_lock_menu);
        forgotPasswordButton = (Button) findViewById(R.id.main_pin_lock_forgot_button);
        selectedPin = "";
        pinPassCode = prefs.getString(AppLockModel.USER_SET_LOCK_PASS_CODE,"noPin");
        salt = prefs.getString(AppLockModel.DEFAULT_APP_BACKGROUND_COLOR_KEY,"noColor");
        digitTypFace = Typeface.createFromAsset(activity.getAssets(),"fonts/arquitectabook.ttf");
        inflatePinViews();
    }

    void inflatePinViews(){
        button_digit_one = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_one);
        button_digit_two = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_two);
        button_digit_three = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_three);
        button_digit_four = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_four);
        button_digit_five = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_five);
        button_digit_six = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_six);
        button_digit_seven= (AppCompatButton) findViewById(R.id.main_pin_lock_digit_seven);
        button_digit_eight = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_eight);
        button_digit_nine = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_nine);
        button_digit_zero = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_zero);

        img_trigger_one = (ImageView) findViewById(R.id.main_pin_lock_trigger_one);
        img_trigger_two = (ImageView) findViewById(R.id.main_pin_lock_trigger_two);
        img_trigger_three = (ImageView) findViewById(R.id.main_pin_lock_trigger_three);
        img_trigger_four = (ImageView) findViewById(R.id.main_pin_lock_trigger_four);

        clear_pin_button = (AppCompatButton) findViewById(R.id.main_pin_lock_digit_clear);

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
        forgotPasswordMenu.setOnClickListener(this);
        forgotPasswordButton.setOnClickListener(this);
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
        forgotPasswordMenu.setOnClickListener(null);
        forgotPasswordButton.setOnClickListener(null);
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.main_pin_lock_digit_one:
                pinClicked("1");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_two:
                pinClicked("2");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_three:
                pinClicked("3");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_four:
                pinClicked("4");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_five:
                pinClicked("5");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_six:
                pinClicked("6");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_seven:
                pinClicked("7");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_eight:
                pinClicked("8");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_nine:
                pinClicked("9");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_zero:
                pinClicked("0");
                vibrateDevice();
                break;
            case R.id.main_pin_lock_digit_clear:
                if(selectedPin!=null){
                    clearPin();
                }
                vibrateDevice();
                break;
            case R.id.main_pin_lock_finger_icon:
                switchToFingerView();
                fingerPrintSwitchButton.setVisibility(INVISIBLE);
                pinPatternSwitchButton.setVisibility(VISIBLE);
                break;
            case R.id.main_pin_lock_keyboard_icon:
                switchToPinView();
                fingerPrintSwitchButton.setVisibility(VISIBLE);
                pinPatternSwitchButton.setVisibility(INVISIBLE);
                break;

            case R.id.main_pin_lock_menu:
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
                break;

            case R.id.main_pin_lock_forgot_button:
                if (isForgotPasswordVisible) {
                    activity.startResetActivity();
                    forgotPasswordButton.setVisibility(INVISIBLE);
                    isForgotPasswordVisible=false;
                }
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
            updateCancelSignal();
        }else {
            analyzeFingerPrint();
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
                button_digit_one.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_one.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitOneAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_one.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_two.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_two.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitTwoAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_two.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_three.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_three.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitThreeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_three.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_four.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_four.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitFourAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_four.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_five.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_five.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitFiveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_five.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_six.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_six.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitSixAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_six.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_seven.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_seven.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitSevenAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_seven.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_eight.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_eight.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitEightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_eight.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_nine.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_nine.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitNineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_nine.setBackgroundResource(R.drawable.img_pin_selected);
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
                button_digit_zero.setBackgroundResource(R.drawable.img_pin_normal_no_back);
                Log.d("AppLock","Animation End");
            }
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                button_digit_zero.setBackgroundResource(R.drawable.img_pin_selected);
                Log.d("AppLock","Animation End");
            }
        });
        digitZeroAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                button_digit_zero.setBackgroundResource(R.drawable.img_pin_selected);
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
                if(!shouldHidePinTouch) {
                    getDigitAnimators(digit).start();
                }
            } else if (pinDigitCount == 3) {
                selectedPin += digit;
                pinDigitCount += 1;
                getTrigger(pinDigitCount).setBackgroundResource(R.drawable.img_pin_trigger_selected);
                if(!shouldHidePinTouch) {
                    getDigitAnimators(digit).start();
                }
                if (!selectedPin.equals("") && pinPassCode.equals(validatePassword(selectedPin))) {
                    Log.d("AppLock", "Passcode is " + selectedPin + "  " );
                    resetPinView();
                    postPinCompleted();
                } else  {
                    triggerAnimator.start();
                    if(isVibratorEnabled){
                        pinDigitVibrator.vibrate(30);
                    }
                    isErrorConfirmed = true;

                }
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
                            postPinCompleted();
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

    private void postPinCompleted(){
        pinLockListener.onPinUnlocked();
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
        unregisterListeners();
        setPinLockUnlockListener(null);
        if(cancelSignal!=null && !cancelSignal.isCanceled()){
            cancelSignal.cancel();
        }
        activity = null;
    }
}

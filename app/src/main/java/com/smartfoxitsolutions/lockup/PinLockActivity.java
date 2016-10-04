package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.NativeExpressAdView;


/**
 * Created by RAAJA on 13-09-2016.
 */
public class PinLockActivity extends AppCompatActivity implements View.OnClickListener{
    NativeExpressAdView pinLockAdView;
    ImageView appIconView;
    AdRequest pinLockAdRequest;
    AppCompatButton button_digit_one,button_digit_two,button_digit_three,button_digit_four,button_digit_five;
    AppCompatButton button_digit_six,button_digit_seven,button_digit_eight,button_digit_nine,button_digit_zero,clear_pin_button;
    ImageView img_trigger_one,img_trigger_two,img_trigger_three,img_trigger_four;
    RelativeLayout pinLayout,triggerLayout;
    Typeface digitTypFace;
    Vibrator pinDigitVibrator;

    private String selectedPin;
    private int pinDigitCount;
    private boolean isVibratorEnabled, isErrorConfirmed;

    ValueAnimator digitOneAnimator,digitTwoAnimator, digitThreeAnimator, digitFourAnimator,digitFiveAnimator, digitSixAnimator
            ,digitSevenAnimator,digitEightAnimator,digitNineAnimator,digitZeroAnimator;
    ValueAnimator triggerAnimator;

    private String packageName;
    private int packageColor;
    long pinPassCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_lock_activity);
        packageName = getIntent().getStringExtra(AppLockingService.CHECKED_APP_LOCK_PACKAGE_NAME);
        packageColor = getIntent().getIntExtra(AppLockingService.CHECKED_APP_LOCK_COLOR,0);
        if(packageColor!=0){
            Drawable appColor = new ColorDrawable(packageColor);
            getWindow().getDecorView().setBackground(appColor);
            getWindow().getDecorView().setAlpha(0.95f);
        }
        else{
            Drawable appColor = new ColorDrawable(Color.parseColor("#F52874F0"));
            getWindow().getDecorView().setBackground(appColor);
        }
        pinLayout = (RelativeLayout) findViewById(R.id.pin_lock_activity_digit_group);
        triggerLayout = (RelativeLayout) findViewById(R.id.pin_lock_activity_trigger_group);
        pinDigitVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        SharedPreferences prefs = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(AppLockModel.VIBRATOR_ENABLED_PREF_KEY,true);
        selectedPin = "";
        pinPassCode = prefs.getLong(AppLockModel.USER_SET_LOCK_PASS_CODE,0);
        String adAppId = getResources().getString(R.string.pin_lock_activity_ad_app_id);
        digitTypFace = Typeface.createFromAsset(getAssets(),"fonts/arquitectabook.ttf");
        appIconView = (ImageView) findViewById(R.id.pin_lock_activity_app_icon_view);
       MobileAds.initialize(getApplicationContext(),adAppId);
        pinLockAdView = (NativeExpressAdView) findViewById(R.id.pin_lock_activity_ad_view);
        inflatePinViews();

    }

    void setAppIcon(String packageName){
        try{
            Drawable appIcon =  getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(appIcon);
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    void inflatePinViews(){
        button_digit_one = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_one);
        button_digit_two = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_two);
        button_digit_three = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_three);
        button_digit_four = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_four);
        button_digit_five = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_five);
        button_digit_six = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_six);
        button_digit_seven= (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_seven);
        button_digit_eight = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_eight);
        button_digit_nine = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_nine);
        button_digit_zero = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_zero);

        img_trigger_one = (ImageView) findViewById(R.id.pin_lock_activity_trigger_one);
        img_trigger_two = (ImageView) findViewById(R.id.pin_lock_activity_trigger_two);
        img_trigger_three = (ImageView) findViewById(R.id.pin_lock_activity_trigger_three);
        img_trigger_four = (ImageView) findViewById(R.id.pin_lock_activity_trigger_four);

        clear_pin_button = (AppCompatButton) findViewById(R.id.pin_lock_activity_digit_clear);

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
        setAppIcon(packageName);
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
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.pin_lock_activity_digit_one:
                pinClicked("1");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_two:
                pinClicked("2");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_three:
                pinClicked("3");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_four:
                pinClicked("4");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_five:
                pinClicked("5");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_six:
                pinClicked("6");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_seven:
                pinClicked("7");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_eight:
                pinClicked("8");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_nine:
                pinClicked("9");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_zero:
                pinClicked("0");
                vibrateDevice();
                break;
            case R.id.pin_lock_activity_digit_clear:
                if(selectedPin!=null){
                    clearPin();
                }
                vibrateDevice();
                break;
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


    /**
     * Returns a Button reference from the digit passed
     * @param digit Digit for which the button reference is to be returned
     * @return Button reference
     */

    AppCompatButton getDigitButton(String digit){
        if(Integer.parseInt(digit)>9 || Integer.parseInt(digit)<0){
            throw new IllegalArgumentException("Digit cannot be less than 0 or more than 9");
        }
        switch(digit){
            case "1":
                return button_digit_one;
            case "2":
                return button_digit_two;
            case "3":
                return button_digit_three;
            case "4":
                return button_digit_four;
            case "5":
                return button_digit_five;
            case "6":
                return button_digit_six;
            case "7":
                return button_digit_seven;
            case "8":
                return button_digit_eight;
            case "9":
                return button_digit_nine;
            case "0":
                return button_digit_zero;
        }
        return null;
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
                    postPatternCompleted(packageName);
                    resetPinView();
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
            getTrigger(selectedPin.length()).setBackgroundResource(R.drawable.img_pin_normal);
            selectedPin = selectedPin.substring(0,selectedPin.length()-1);
            pinDigitCount-=1;
            Log.d("PatternLock","Cleared : " +selectedPin);
        }
    }

    private void postPatternCompleted(String packageUnlockedName){

        AppLockingService.recentlyUnlockedApp = packageUnlockedName;
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pinLockAdRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("A04B6DE7DDFED7231620C0AA9BCF67EC")
                .addTestDevice("A219F9DA86E122F8F4AE0F7EF7FA95E5").build();
        pinLockAdView.loadAd(pinLockAdRequest);

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setPinAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterListeners();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AppLock","Called OnDestroy");
    }

}
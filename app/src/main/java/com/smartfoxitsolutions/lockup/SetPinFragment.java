package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by RAAJA on 7-09-2016.
 */
public class SetPinFragment extends Fragment implements View.OnClickListener {

    static final int PIN_SET_FIRST_ATTEMPT =1; // Flag for first pin set
    static final int PIN_SET_CONFIRMED =2; // Flag for pin confirm

    static final int RESET_PIN_VIEW_PARTIAL =3;//Flag to reset View Partially without removing previous password
    static final int RESET_PIN_VIEW_COMPLETE =4; // Flag to reset view to its original state

    TextView changeLock,pinInfo,pinHead;
    Button button_digit_one,button_digit_two,button_digit_three,button_digit_four,button_digit_five;
    Button button_digit_six,button_digit_seven,button_digit_eight,button_digit_nine,button_digit_zero,clear_pin_button;
    ImageView img_trigger_one,img_trigger_two,img_trigger_three,img_trigger_four;
    RelativeLayout pinLayout;
    Typeface digitTypFace;

    private String selectedPin,pinSetFirstAttempt, pinConfirmed;
    private int pinDigitCount, pinSetCount=PIN_SET_FIRST_ATTEMPT;
    private boolean isPinSetStarted,isPinSetCompleted,isResetPin,isPinError; // Set to true when user starts pressing the Pin
    ObjectAnimator pinErrorAnimator,pinCompleteAnimator;

    SetPinPatternActivity pinPatternActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.set_pin_fragment,container,false);
        changeLock = (TextView) parent.findViewById(R.id.set_pin_fragment_change_lock_text);
        pinInfo = (TextView) parent.findViewById(R.id.set_pin_fragment_info_text);
        pinHead = (TextView) parent.findViewById(R.id.set_pin_fragment_title_text);
        pinLayout = (RelativeLayout) parent.findViewById(R.id.set_pin_fragment_digit_group);
        inflatePinViews(parent);
        setErrorAnimation();
        registerListeners();
        resetPinView(RESET_PIN_VIEW_COMPLETE);
        return parent;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pinPatternActivity = (SetPinPatternActivity) getActivity();
        setDigitTypeface();
    }

    void setDigitTypeface(){
        digitTypFace = Typeface.createFromAsset(pinPatternActivity.getAssets(),"fonts/arquitectabook.ttf");
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

    }

    /**
     * Inflates the Pin views and triggers.
     * @param parent Parent Viewgroup where the pin views are present.
     */

    void inflatePinViews(View parent){
        button_digit_one = (Button) parent.findViewById(R.id.set_pin_fragment_digit_one);
        button_digit_two = (Button) parent.findViewById(R.id.set_pin_fragment_digit_two);
        button_digit_three = (Button) parent.findViewById(R.id.set_pin_fragment_digit_three);
        button_digit_four = (Button) parent.findViewById(R.id.set_pin_fragment_digit_four);
        button_digit_five = (Button) parent.findViewById(R.id.set_pin_fragment_digit_five);
        button_digit_six = (Button) parent.findViewById(R.id.set_pin_fragment_digit_six);
        button_digit_seven= (Button) parent.findViewById(R.id.set_pin_fragment_digit_seven);
        button_digit_eight = (Button) parent.findViewById(R.id.set_pin_fragment_digit_eight);
        button_digit_nine = (Button) parent.findViewById(R.id.set_pin_fragment_digit_nine);
        button_digit_zero = (Button) parent.findViewById(R.id.set_pin_fragment_digit_zero);



        img_trigger_one = (ImageView) parent.findViewById(R.id.set_pin_fragment_trigger_one);
        img_trigger_two = (ImageView) parent.findViewById(R.id.set_pin_fragment_trigger_two);
        img_trigger_three = (ImageView) parent.findViewById(R.id.set_pin_fragment_trigger_three);
        img_trigger_four = (ImageView) parent.findViewById(R.id.set_pin_fragment_trigger_four);

        clear_pin_button = (Button) parent.findViewById(R.id.set_pin_fragment_digit_clear);
    }

    /**
     * Register onClick Listeners for the Pin Digits
     */

    void registerListeners(){
        changeLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isResetPin){
                    pinHead.setText(R.string.set_pin_fragment_title_text);
                    changeLock.setText(R.string.set_pin_fragment_change_lock_text);
                    resetPinView(RESET_PIN_VIEW_COMPLETE);
                }else{
                    pinPatternActivity.addNewFragment(SetPinPatternActivity.SET_PATTERN_FRAGMENT_TAG);
                }
            }
        });

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

    /**
     * Unregister onClick Listeners for the Pin Digits
     */

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

    /**
     * Sets the Error animation for pin digits
     */

    void setErrorAnimation(){
        pinErrorAnimator = ObjectAnimator.ofInt(pinInfo,"padding",0,0);
        pinErrorAnimator.setDuration(200).setInterpolator(new LinearInterpolator());
        pinErrorAnimator.setRepeatCount(3);
        pinErrorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                pinInfo.setText(R.string.set_pin_fragment_not_equal_text);
                pinInfo.setTextColor(Color.parseColor("#ef5350")); // For minimum API 15
                Button button;
                String digitString;
                for(int i=0;i<selectedPin.length();i++){
                    digitString = String.valueOf(selectedPin.charAt(i));
                    button = getDigitButton(digitString);
                    button.setBackgroundResource(R.drawable.img_pin_error);
                }
                for(int i =1;i<=4;i++){
                    getTrigger(i).setBackgroundResource(R.drawable.img_pin_error);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                pinInfo.setText(R.string.set_pin_fragment_info_text);
                pinInfo.setTextColor(Color.WHITE);
                resetPinView(RESET_PIN_VIEW_PARTIAL);
                isPinError = false;
            }
        });

        pinCompleteAnimator = ObjectAnimator.ofInt(pinInfo,"padding",0,0);
        pinCompleteAnimator.setDuration(100).setInterpolator(new LinearInterpolator());
        pinCompleteAnimator.setRepeatCount(1);
        pinCompleteAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(isPinSetCompleted){
                    resetPinView(RESET_PIN_VIEW_COMPLETE);
                }else{
                    resetPinView(RESET_PIN_VIEW_PARTIAL);
                    isResetPin =true;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.set_pin_fragment_digit_one:
                    pinClicked("1");
                break;
            case R.id.set_pin_fragment_digit_two:
                    pinClicked("2");
                break;
            case R.id.set_pin_fragment_digit_three:
                    pinClicked("3");
                break;
            case R.id.set_pin_fragment_digit_four:
                    pinClicked("4");
                break;
            case R.id.set_pin_fragment_digit_five:
                     pinClicked("5");
                break;
            case R.id.set_pin_fragment_digit_six:
                     pinClicked("6");
                break;
            case R.id.set_pin_fragment_digit_seven:
                    pinClicked("7");
                break;
            case R.id.set_pin_fragment_digit_eight:
                    pinClicked("8");
                break;
            case R.id.set_pin_fragment_digit_nine:
                     pinClicked("9");
                break;
            case R.id.set_pin_fragment_digit_zero:
                    pinClicked("0");
                break;
            case R.id.set_pin_fragment_digit_clear:
                    if(selectedPin!=null){
                        clearPin();
                    }

                break;
        }
    }

    /**
     * Handles pin clicks as the users presses them. This is the main part where the pressed digits are
     * compared to set the Pin
     * @param digit Digit String to be passed when the users presses a particular digit
     */

    void pinClicked(String digit){
       if(!isPinError) {
           if (!isPinSetStarted) {
               isPinSetStarted = true;
               pinDigitCount += 1;
               selectedPin += digit;
               getTrigger(pinDigitCount).setBackgroundResource(R.drawable.img_pin_selected);
           } else if (pinDigitCount < 3) {
               pinDigitCount += 1;
               selectedPin += digit;
               getTrigger(pinDigitCount).setBackgroundResource(R.drawable.img_pin_selected);
           } else if (pinDigitCount == 3) {
               selectedPin += digit;
               pinDigitCount += 1;
               getTrigger(pinDigitCount).setBackgroundResource(R.drawable.img_pin_selected);
               if (pinSetCount == PIN_SET_FIRST_ATTEMPT) {
                   pinSetCount = PIN_SET_CONFIRMED;
                   pinSetFirstAttempt = selectedPin;
                   pinHead.setText(R.string.set_pin_fragment_confirm_text);
                   changeLock.setText(R.string.set_pattern_fragment_reset_text);
                   isPinSetCompleted = false;
                   pinCompleteAnimator.start();
               } else if (pinSetFirstAttempt.equals(selectedPin)) {
                   pinConfirmed = selectedPin;
                   //pinHead.setText(R.string.set_pin_fragment_title_text);
                   //changeLock.setText(R.string.set_pin_fragment_change_lock_text);
                   isPinSetCompleted = true;
                   persistUserPin(pinConfirmed);
                   pinPatternActivity.startLockUpMainActivity();
                   pinCompleteAnimator.start();

               } else if (!pinSetFirstAttempt.equals(selectedPin)) {
                   isPinError = true;
                   pinErrorAnimator.start();
               }
           }
       }
    }

    /**
     * Saves the hashed Pin to preference
     * @param pin pin confirmed by user
     */

    void persistUserPin(String pin) {
        String salt = String.valueOf(System.currentTimeMillis());
        try {
            byte[] usePassByte = (pin+salt).getBytes("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = digest.digest(usePassByte);
            StringBuilder sb1 = new StringBuilder();
            for (int i = 0; i < messageDigest.length; ++i) {
                sb1.append(Integer.toHexString((messageDigest[i] & 0xFF) | 0x100).substring(1,3));
            }
            SharedPreferences prefs = pinPatternActivity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(AppLockModel.USER_SET_LOCK_PASS_CODE, sb1.toString());
            edit.putString(AppLockModel.DEFAULT_APP_BACKGROUND_COLOR_KEY,salt);
            edit.putInt(AppLockModel.APP_LOCK_LOCKMODE, AppLockModel.APP_LOCK_MODE_PIN);
            edit.apply();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

        /**
     *  Resets the Pin View partially or completely from the flag passed
     * @param reset Flag for resetting the view. Either RESET_PIN_VIEW_PARTIAL or RESET_PIN_VIEW_COMPLETE
     */

    void resetPinView(int reset){
        if(reset!=SetPinFragment.RESET_PIN_VIEW_COMPLETE &
                   reset!= SetPinFragment.RESET_PIN_VIEW_PARTIAL){
            throw new IllegalArgumentException("Flags can either be "+SetPinFragment.RESET_PIN_VIEW_PARTIAL
                                                +" or "+SetPinFragment.RESET_PIN_VIEW_COMPLETE);
        }
        Button button;
        String digitString;
        if(selectedPin!=null) {
            for (int i = 0; i < selectedPin.length(); i++) {
                digitString = String.valueOf(selectedPin.charAt(i));
                button = getDigitButton(digitString);
                button.setBackgroundResource(R.drawable.selector_pin_fragment_digit_button);
            }
        }
        for(int i =1;i<=4;i++){
            getTrigger(i).setBackgroundResource(R.drawable.img_pin_normal);
        }
        if(reset==RESET_PIN_VIEW_PARTIAL){
            isPinSetStarted=false;
            pinDigitCount=0;
            selectedPin="";
        }
        if (reset==RESET_PIN_VIEW_COMPLETE){
            isPinSetStarted=false;
            isPinSetCompleted=false;
            isResetPin=false;
            pinDigitCount=0;
            selectedPin="";
            pinSetFirstAttempt="";
            pinConfirmed="";
            pinSetCount=PIN_SET_FIRST_ATTEMPT;

        }
    }

    /**
     * Returns a Button reference from the digit passed
     * @param digit Digit for which the button reference is to be returned
     * @return Button reference
     */

    Button getDigitButton(String digit){
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

    void clearPin(){
        if(!selectedPin.isEmpty() && selectedPin.length()>=0){
            getTrigger(selectedPin.length()).setBackgroundResource(R.drawable.img_pin_normal);
            selectedPin = selectedPin.substring(0,selectedPin.length()-1);
            pinDigitCount-=1;
            Log.d("PatternLock","Cleared : " +selectedPin);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pinPatternActivity = null;
        unregisterListeners();
    }
}

package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.views.PatternLockView;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/**
 * Created by RAAJA on 7-09-2016.
 *
 * Fragment class for setting the pattern for the app lock.
 */
public class SetPatternFragment extends Fragment implements PatternLockView.OnPatternChangedListener{
    static final int PATTERN_SET_FIRST_ATTEMPT = 1;
    static final int PATTERN_CONFIRMED_SET =2;
    static final int RESET_PATTERN_VIEW = 3;
    static final int COMPLETE_RESET_PATTERN_VIEW=4;

    private PatternLockView patternLockView;
    TextView patternInfo,changeLock, patternHead;
    private boolean isLessThanFourDots,isPatternNotEqual,isRestPatternEnabled;
    private int patternSetCount = 1, patternDotSelectedCount;
    private String selectedPatternDot,patternSelectedFirstAttempt,patternConfirmed;
    SetPinPatternActivity pinPatternActivity;
    ObjectAnimator patternAnimator;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pinPatternActivity = (SetPinPatternActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.set_pattern_fragment,container,false);
        patternLockView = (PatternLockView) parent.findViewById(R.id.set_pattern_fragment_pattern_view);
        patternInfo = (TextView) parent.findViewById(R.id.set_pattern_fragment_info_text);
        patternHead = (TextView) parent.findViewById(R.id.set_pattern_fragment_title_text);
        changeLock = (TextView) parent.findViewById(R.id.set_pattern_fragment_change_lock_text);
        registerViewListeners();
        setPatternErrorAnimation();
        resetPatternView(COMPLETE_RESET_PATTERN_VIEW);
        return parent;
    }

    /**
     * Sets Views Listeners
     */

    void registerViewListeners(){
        patternLockView.setOnPatternChangedListener(this);
        changeLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRestPatternEnabled){
                    resetPatternView(COMPLETE_RESET_PATTERN_VIEW);
                }else {
                    pinPatternActivity.addNewFragment(SetPinPatternActivity.SET_PIN_FRAGMENT_TAG);
                }
            }
        });
    }

    void unregisterViewListeners(){
        patternLockView.setOnPatternChangedListener(null);
        changeLock.setOnClickListener(null);
    }

    /**
     * Sets the Animation for Pattern Error
     */
    void setPatternErrorAnimation(){
        patternAnimator = ObjectAnimator.ofInt(patternInfo,"padding",0,0);
        patternAnimator.setDuration(200).setRepeatCount(3);
        patternAnimator.setInterpolator(new LinearInterpolator());
        patternAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                patternLockView.postPatternError();
                if(isLessThanFourDots){
                    patternInfo.setText(R.string.set_pattern_fragment_info_text);
                    isLessThanFourDots = false;
                }else if(isPatternNotEqual){
                        patternInfo.setText(R.string.set_pattern_fragment_not_equal_text);
                    isPatternNotEqual = false;
                }
                patternInfo.setTextColor(Color.parseColor("#ef5350")); //For minimum API 15
                resetPatternView(RESET_PATTERN_VIEW);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                patternInfo.setText(R.string.set_pattern_fragment_info_text);
                patternInfo.setTextColor(Color.WHITE);
                patternLockView.resetPatternView();
            }
        });

    }

    @Override
    public void onPatternNodeSelected(int selectedPatternNode) {
        selectedPatternDot = selectedPatternDot+String.valueOf(selectedPatternNode);
        patternDotSelectedCount+=1;
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        if(patternCompleted && patternDotSelectedCount>=4){
            if(patternSetCount == PATTERN_SET_FIRST_ATTEMPT){
                patternSelectedFirstAttempt = selectedPatternDot;
                patternHead.setText(R.string.set_pattern_fragment_confirm_text);
                changeLock.setText(R.string.set_pattern_fragment_reset_text);
                isRestPatternEnabled=true;
                patternSetCount+=1;
                resetPatternView(RESET_PATTERN_VIEW);
                patternLockView.resetPatternView();
            }else if (patternSetCount == PATTERN_CONFIRMED_SET && patternSelectedFirstAttempt.equals(selectedPatternDot)){
                patternConfirmed = selectedPatternDot;
                setPatternPassCode(patternConfirmed);
                patternLockView.resetPatternView();
                resetPatternView(COMPLETE_RESET_PATTERN_VIEW);
            }else if(!patternSelectedFirstAttempt.equals(selectedPatternDot)){
                isPatternNotEqual=true;
                patternAnimator.start();
            }
        }else if(patternDotSelectedCount < 4){
            isLessThanFourDots = true;
            patternAnimator.start();
        }
    }

    @Override
    public void onErrorStop() {

    }

    /**
     * Resets set pattern lock screen to original state
     * @param reset On of the constants used to reset partially or completely
     */

    void resetPatternView(int reset){
        if(reset==RESET_PATTERN_VIEW){
            selectedPatternDot="";
            patternDotSelectedCount=0;
        }
        if (reset==COMPLETE_RESET_PATTERN_VIEW){
            selectedPatternDot="";
            patternDotSelectedCount=0;
            patternSetCount=1;
            isRestPatternEnabled=false;
            patternSelectedFirstAttempt="";
            patternConfirmed="";
            patternHead.setText(R.string.set_pattern_fragment_title_text);
            changeLock.setText(R.string.set_pattern_fragment_change_lock_text);
        }
    }

    void setPatternPassCode(String passCode){
        if(passCode!=null && !passCode.isEmpty()){
            persistUserPin(passCode);
            pinPatternActivity.startLockUpMainActivity();
        }else{
            resetPatternView(COMPLETE_RESET_PATTERN_VIEW);
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
            edit.putInt(AppLockModel.APP_LOCK_LOCKMODE, AppLockModel.APP_LOCK_MODE_PATTERN);
            edit.apply();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        patternLockView.closePatternView();
        super.onDestroy();
        unregisterViewListeners();
        pinPatternActivity = null;
    }


}

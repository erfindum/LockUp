package com.smartfoxitsolutions.lockup.loyaltybonus;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.smartfoxitsolutions.lockup.MainLockActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.OnUserSignUpListener;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 13-01-2017.
 */

public class LoyaltyBonusMain extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener{

    private static final String LOYALTY_BONUS_SIGNUP_TAG = "loyalty_bonus_signup";
    static final int USER_LOGGED_OUT = 25;


    private FragmentManager fragmentManager;
    private OnUserSignUpListener userSignUpListener;
    boolean shouldTrackUserPresence, shouldCloseAffinity;
    boolean isRecoverySent, isSignInDisplayedOnce;
    private LoyaltyMainScreenOffReceiver loyaltyMainScreenOffReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loyalty_bonus_main_activity);
        fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        shouldTrackUserPresence = true;
    }

    public void setOnUserSignUpListener(OnUserSignUpListener listener){
        this.userSignUpListener = listener;
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences preferences = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
        int startType = getIntent().getIntExtra("userLoggedOut",0);
        if(startType == USER_LOGGED_OUT && !isSignInDisplayedOnce){
            isSignInDisplayedOnce =true;
            startSignIn();
        }else {
            if (isLoggedIn) {
                shouldTrackUserPresence = false;
                startActivity(new Intent(this, LoyaltyUserActivity.class));
                finish();
            } else if (!isRecoverySent) {
                FragmentTransaction fragTransaction = fragmentManager.beginTransaction();
                fragTransaction.add(R.id.loyalty_bonus_main_activity_container, new LoyaltyBonusSignUpFragment(), LOYALTY_BONUS_SIGNUP_TAG);
                fragTransaction.addToBackStack(LOYALTY_BONUS_SIGNUP_TAG);
                fragTransaction.commit();
            }
        }
        loyaltyMainScreenOffReceiver = new LoyaltyMainScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(loyaltyMainScreenOffReceiver,filter);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity=true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    void startSignIn(){
        FragmentTransaction fragTransaction = fragmentManager.beginTransaction();
        fragTransaction.add(R.id.loyalty_bonus_main_activity_container,new LoyaltyBonusLoginFragment(),"loyaltyBonusSignin");
        fragTransaction.addToBackStack("loyaltyBonusSignin");
        fragTransaction.commit();
    }

    void startForgotPassword(){
        FragmentTransaction fragTransaction = fragmentManager.beginTransaction();
        fragTransaction.add(R.id.loyalty_bonus_main_activity_container,new LoyaltyBonusRecoverFragment(),"forgotUserPassword");
        fragTransaction.addToBackStack("forgotUserPassword");
        fragTransaction.commit();
    }

    public void signUpSuccess(){
        shouldTrackUserPresence = false;
        startActivity(new Intent(this,LoyaltyUserActivity.class));
        finish();
    }

    public void passwordResetSuccess(){
        fragmentManager.popBackStack();
    }

    void startLoyaltyUserMain(){
        shouldTrackUserPresence = false;
        startActivity(new Intent(this,LoyaltyUserActivity.class));
        finish();
    }

    void setPanInputMode(){
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                        |WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    void setResizeInputMode(){
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void setDate(int year, int monthOfYear, int dayOfMonth){
        if(userSignUpListener !=null) {
            userSignUpListener.onUserDatePicked(year, monthOfYear, dayOfMonth);
        }
    }

    public void errorDialogCancelled(){
        if(userSignUpListener !=null){
            userSignUpListener.onErrorDialogCancel();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            finishAffinity();
        }
        if(!shouldTrackUserPresence){
            unregisterReceiver(loyaltyMainScreenOffReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("LoyaltyBonus",fragmentManager.getBackStackEntryCount() + " Frag Entries");
        if(fragmentManager.getBackStackEntryCount()==0){
            finish();
            return;
        }
       FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount()-1);
        if(entry.getName().equals("loyaltyBonusSignin")){
            if(isRecoverySent){
                startActivity(new Intent(getBaseContext(),MainLockActivity.class));
                finishAffinity();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setOnUserSignUpListener(null);
        if(shouldTrackUserPresence){
            unregisterReceiver(loyaltyMainScreenOffReceiver);
        }
    }

    @Override
    public void onBackStackChanged() {
        int count = fragmentManager.getBackStackEntryCount()-1;
        if(count>-1) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1);
            if (entry.getName().equals("loyaltyBonusSignin")) {
                setPanInputMode();
            } else {
                setResizeInputMode();
            }
        }
    }

    static class LoyaltyMainScreenOffReceiver extends BroadcastReceiver {

        WeakReference<LoyaltyBonusMain> activity;
        LoyaltyMainScreenOffReceiver(WeakReference<LoyaltyBonusMain> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishAffinity();
            }
        }
    }
}

package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.dialogs.FingerPrintActivateDialog;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

/**
 * Created by RAAJA on 18-08-2016.
 */
public class LockUpSettingsActivity extends AppCompatActivity {


    private TextView changePinPatternText, activateAppLockText;
    private int appLockMode;
    private SwitchCompat fingerPrintSwitch, activateAppLockSwitch, vibrateSwitch, pinTouchSwitch, patternLineSwitch
                            ,lockScreenOnSwitch;
    private SharedPreferences prefs;
    private boolean isFingerPrintActive, shouldAppLockStart,isVibratorEnabled,shouldHidePinTouch, shouldHidePatternLine
                    , shouldLockScreenOn, isAppLockFirstLoad;
    private RelativeLayout activateAppLockItem, vibrateItem, changePinPatternItem,fingerPrintItem, pinTouchItem
                    , patternLineItem, screenOnLockItem;
    private FingerPrintActivateDialog fingerprintActivateDialog;
    private ValueAnimator textAnimator;

    public static final String FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY = "fingerPrintLockSelected";
    public static final String APP_LOCKING_SERVICE_START_PREFERENCE_KEY = "appLockStopSettings";
    public static final String VIBRATOR_ENABLED_PREFERENCE_KEY = "vibratorEnabled";
    public static final String HIDE_PIN_TOUCH_PREFERENCE_KEY = "hidePinTouch";
    public static final String HIDE_PATTERN_LINE_PREFERENCE_KEY = "hidePatternLine";
    public static final String LOCK_SCREEN_ON_PREFERENCE_KEY = "lockScreenOn";
    public static final String RECOVERY_EMAIL_PREFERENCE_KEY = "recoverEmail";

    private static final String FINGERPRINT_ACTIVATE_DIALOG_TAG = "fingerprint_activate_dialog";
    private static final int SET_PIN_PATTERN_REQUEST_CODE = 44;
    private FingerprintManagerCompat fingerprintManager;
    private boolean shouldTrackUserPresence, shouldCloseAffinity;
    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        activateAppLockText = (TextView) findViewById(R.id.settings_activity_activate_lock_summery);
        changePinPatternText = (TextView) findViewById(R.id.settings_activity_change_pin_summery);

        activateAppLockSwitch = (SwitchCompat) findViewById(R.id.settings_activity_activate_lock_switch);
        vibrateSwitch = (SwitchCompat) findViewById(R.id.settings_activity_vibrate_switch);
        fingerPrintSwitch = (SwitchCompat) findViewById(R.id.settings_activity_fingerprint_switch);
        pinTouchSwitch = (SwitchCompat) findViewById(R.id.settings_activity_pin_touch_switch);
        patternLineSwitch = (SwitchCompat) findViewById(R.id.settings_activity_pattern_line_switch);
        lockScreenOnSwitch = (SwitchCompat) findViewById(R.id.settings_activity_relock_switch);

        activateAppLockItem = (RelativeLayout) findViewById(R.id.settings_activity_activate_lock_group);
        vibrateItem = (RelativeLayout) findViewById(R.id.settings_activity_vibrate_group);
        changePinPatternItem = (RelativeLayout) findViewById(R.id.settings_activity_change_pin_group);
        fingerPrintItem = (RelativeLayout) findViewById(R.id.settings_activity_fingerprint_group);
        pinTouchItem = (RelativeLayout) findViewById(R.id.settings_activity_pin_touch_group);
        patternLineItem = (RelativeLayout) findViewById(R.id.settings_activity_pattern_line_group);
        screenOnLockItem = (RelativeLayout) findViewById(R.id.settings_activity_relock_group);

        toolbar = (Toolbar) findViewById(R.id.settings_activity_tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.settings_activity_toolbar_title));
        prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        fingerprintManager = FingerprintManagerCompat.from(getBaseContext());
        setUpPreferences();
        setupViews();
        setListeners();
        shouldTrackUserPresence = true;
    }

    void setUpPreferences(){
        shouldAppLockStart = prefs.getBoolean(APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = prefs.getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,false);
        isVibratorEnabled = prefs.getBoolean(VIBRATOR_ENABLED_PREFERENCE_KEY,false);
        appLockMode = prefs.getInt(AppLockModel.APP_LOCK_LOCKMODE,0);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            isFingerPrintActive = prefs.getBoolean(FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY, false);
        }
        shouldHidePinTouch = prefs.getBoolean(HIDE_PIN_TOUCH_PREFERENCE_KEY,false);
        shouldHidePatternLine = prefs.getBoolean(HIDE_PATTERN_LINE_PREFERENCE_KEY,false);
        shouldLockScreenOn = prefs.getBoolean(LOCK_SCREEN_ON_PREFERENCE_KEY,false);
    }
    private void setupViews(){

        //AppLock On/Off
        if(shouldAppLockStart){
            activateAppLockText.setText(getString(R.string.settings_activity_activate_locker_enabled));
            if(!isAppLockFirstLoad) {
                activateAppLockSwitch.setChecked(true);
            }
        }else{
            activateAppLockText.setText(getString(R.string.settings_activity_activate_locker_disabled));
            if(!isAppLockFirstLoad) {
                activateAppLockSwitch.setChecked(false);
            }
        }

        //Vibration
        if(isVibratorEnabled){
            vibrateSwitch.setChecked(true);
        }else{
            vibrateSwitch.setChecked(false);
        }

        //Change Pin/Pattern
        if(appLockMode==AppLockModel.APP_LOCK_MODE_PIN){
            changePinPatternText.setText(getString(R.string.settings_activity_change_pin_summery));
        }
        if(appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN){
            changePinPatternText.setText(getString(R.string.settings_activity_change_pattern_summery));
        }

        //FingerPrint Lock
        if(isFingerPrintActive){
            fingerPrintSwitch.setChecked(true);
        }else{
            fingerPrintSwitch.setChecked(false);
        }

        //Hide Pin Touch
        if(shouldHidePinTouch){
            pinTouchSwitch.setChecked(true);
        }else{
            pinTouchSwitch.setChecked(false);
        }

        //Hide Pattern Touch
        if(shouldHidePatternLine){
            patternLineSwitch.setChecked(true);
        }else{
            patternLineSwitch.setChecked(false);
        }

        //ScreenOn Lock
        if(shouldLockScreenOn){
            lockScreenOnSwitch.setChecked(true);
        }else{
            lockScreenOnSwitch.setChecked(false);
        }

    }

    private void setListeners(){
        //AppLock On/Off
        activateAppLockItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = prefs.edit();
                if(!isAppLockFirstLoad) {
                    if (shouldAppLockStart) {
                        startAlphaAnimation(activateAppLockText, getString(R.string.settings_activity_activate_locker_disabled));
                        activateAppLockSwitch.setChecked(false);
                        shouldAppLockStart = false;
                        edit.putBoolean(APP_LOCKING_SERVICE_START_PREFERENCE_KEY, false);
                    } else {
                        startAlphaAnimation(activateAppLockText, getString(R.string.settings_activity_activate_locker_enabled));
                        activateAppLockSwitch.setChecked(true);
                        shouldAppLockStart = true;
                        edit.putBoolean(APP_LOCKING_SERVICE_START_PREFERENCE_KEY, true);
                    }
                    edit.apply();
                }
            }
        });

        //Vibration
        vibrateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = prefs.edit();
                if(isVibratorEnabled){
                    vibrateSwitch.setChecked(false);
                    isVibratorEnabled = false;
                    edit.putBoolean(VIBRATOR_ENABLED_PREFERENCE_KEY,false);
                }else{
                    vibrateSwitch.setChecked(true);
                    isVibratorEnabled = true;
                    edit.putBoolean(VIBRATOR_ENABLED_PREFERENCE_KEY,true);
                }
                edit.apply();
            }
        });

        //Change Pin/Pattern
        changePinPatternItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getBaseContext(),SetPinPatternActivity.class)
                        .putExtra(SetPinPatternActivity.INTENT_PIN_PATTERN_START_TYPE_KEY,SetPinPatternActivity.INTENT_SETTINGS)
                   ,SET_PIN_PATTERN_REQUEST_CODE);
                shouldTrackUserPresence = false;
            }
        });

        //FingerPrint Lock
        fingerPrintItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
                    if((checkSelfPermission("android.permission.USE_FINGERPRINT")== PackageManager.PERMISSION_GRANTED)) {
                        if(fingerprintManager.isHardwareDetected()) {
                            if(fingerprintManager.hasEnrolledFingerprints()){
                                SharedPreferences.Editor edit = prefs.edit();
                                if(isFingerPrintActive){
                                    edit.putBoolean(FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,false);
                                    isFingerPrintActive = false;
                                    fingerPrintSwitch.setChecked(false);
                                }else{
                                    edit.putBoolean(FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,true);
                                    isFingerPrintActive = true;
                                    fingerPrintSwitch.setChecked(true);
                                }
                                edit.apply();

                            }else{
                                fingerprintActivateDialog = new FingerPrintActivateDialog();
                                Bundle bundle = new Bundle();
                                bundle.putString(FingerPrintActivateDialog.FINGERPRINT_INFO_MESSAGE,
                                        getString(R.string.settings_dialog_finger_no_fingerprint));
                                fingerprintActivateDialog.setArguments(bundle);
                                FragmentManager fragmentManager = getSupportFragmentManager();
                                FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
                                fragmentTransaction.addToBackStack(FINGERPRINT_ACTIVATE_DIALOG_TAG);
                                fingerprintActivateDialog.show(fragmentTransaction,FINGERPRINT_ACTIVATE_DIALOG_TAG);
                            }
                        }else{
                            fingerprintActivateDialog = new FingerPrintActivateDialog();
                            Bundle bundle = new Bundle();
                            bundle.putString(FingerPrintActivateDialog.FINGERPRINT_INFO_MESSAGE,
                                    getString(R.string.settings_dialog_finger_no_sensor));
                            fingerprintActivateDialog.setArguments(bundle);
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
                            fragmentTransaction.addToBackStack(FINGERPRINT_ACTIVATE_DIALOG_TAG);
                            fingerprintActivateDialog.show(fragmentTransaction,FINGERPRINT_ACTIVATE_DIALOG_TAG);
                        }
                    }
                }else{
                    fingerprintActivateDialog = new FingerPrintActivateDialog();
                    Bundle bundle = new Bundle();
                    bundle.putString(FingerPrintActivateDialog.FINGERPRINT_INFO_MESSAGE,
                            getString(R.string.settings_dialog_finger_not_supported));
                    fingerprintActivateDialog.setArguments(bundle);
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack(FINGERPRINT_ACTIVATE_DIALOG_TAG);
                    fingerprintActivateDialog.show(fragmentTransaction,FINGERPRINT_ACTIVATE_DIALOG_TAG);
                }
            }
        });

        //Hide Pin Touch
        pinTouchItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = prefs.edit();
                if(shouldHidePinTouch){
                    edit.putBoolean(HIDE_PIN_TOUCH_PREFERENCE_KEY,false);
                    shouldHidePinTouch = false;
                    pinTouchSwitch.setChecked(false);
                }else{
                    edit.putBoolean(HIDE_PIN_TOUCH_PREFERENCE_KEY,true);
                    shouldHidePinTouch = true;
                    pinTouchSwitch.setChecked(true);
                }
                edit.apply();
            }
        });

        //Hide Pattern Touch
        patternLineItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = prefs.edit();
                if(shouldHidePatternLine){
                    edit.putBoolean(HIDE_PATTERN_LINE_PREFERENCE_KEY,false);
                    shouldHidePatternLine = false;
                    patternLineSwitch.setChecked(false);
                }else{
                    edit.putBoolean(HIDE_PATTERN_LINE_PREFERENCE_KEY,true);
                    shouldHidePatternLine = true;
                    patternLineSwitch.setChecked(true);
                }
                edit.apply();
            }
        });

        //ScreenOn Lock
        screenOnLockItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = prefs.edit();
                if(shouldLockScreenOn){
                    edit.putBoolean(LOCK_SCREEN_ON_PREFERENCE_KEY,false);
                    shouldLockScreenOn = false;
                    lockScreenOnSwitch.setChecked(false);
                }else{
                    edit.putBoolean(LOCK_SCREEN_ON_PREFERENCE_KEY,true);
                    shouldLockScreenOn = true;
                    lockScreenOnSwitch.setChecked(true);
                }
                edit.apply();
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shouldTrackUserPresence = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity = true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SET_PIN_PATTERN_REQUEST_CODE){
            appLockMode = prefs.getInt(AppLockModel.APP_LOCK_LOCKMODE,0);
            if(appLockMode==AppLockModel.APP_LOCK_MODE_PIN){
                changePinPatternText.setText(getString(R.string.settings_activity_change_pin_summery));
            }
            if(appLockMode == AppLockModel.APP_LOCK_MODE_PATTERN){
                changePinPatternText.setText(getString(R.string.settings_activity_change_pattern_summery));
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        textAnimator = ValueAnimator.ofFloat(0.0f,1.0f);
        textAnimator.setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator());
    }

    void startAlphaAnimation(final TextView textView, final String text){
        textAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                textView.setText(text);
            }
        });
        textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                textView.setAlpha(value);
            }
        });
        textAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(shouldAppLockStart && !isAppLockFirstLoad) {
            startService(new Intent(getBaseContext(),AppLockingService.class));
            LockUpMainActivity.hasAppLockStarted = true;
        }
        if(!shouldAppLockStart){
            sendBroadcast(new Intent(AppLockingService.STOP_APP_LOCK_SERVICE));
            LockUpMainActivity.hasAppLockStarted = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            finishAffinity();
        }
    }
}

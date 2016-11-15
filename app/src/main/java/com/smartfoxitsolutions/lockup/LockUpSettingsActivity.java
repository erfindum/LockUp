package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.services.AppLockingService;

/**
 * Created by RAAJA on 18-08-2016.
 */
public class LockUpSettingsActivity extends AppCompatActivity {


    private TextView changePinPattern;
    private SwitchCompat fingerPrintSwitch;
    private SharedPreferences prefs;
    private boolean isfingerPrintActive, shouldAppLockStart;

    public static String FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY = "fingerPrintLockSelected";
    public static String APP_LOCKING_SERVICE_START_PREFERENCE_KEY = "appLockStopSettingsKey";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        changePinPattern = (TextView) findViewById(R.id.settings_change_pin_pattern);
        fingerPrintSwitch = (SwitchCompat) findViewById(R.id.settings_switch_fingerprint_switch);
        prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            isfingerPrintActive = prefs.getBoolean(FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY, false);
        }
        shouldAppLockStart = prefs.getBoolean(APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        setupViews();
    }

    private void setupViews(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
             if((checkSelfPermission("android.permission.USE_FINGERPRINT")== PackageManager.PERMISSION_GRANTED)) {
                 FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(getBaseContext());
                 if(fingerprintManager.isHardwareDetected()) {
                     setFingerPrintSwitch();
                 }else{
                     fingerPrintSwitch.setEnabled(false);
                 }
             }
        }else{
            fingerPrintSwitch.setEnabled(false);
        }
        setChangePinPattern();
    }

    private void setFingerPrintSwitch(){
        if(isfingerPrintActive){
            fingerPrintSwitch.setChecked(true);
        }else{
            fingerPrintSwitch.setChecked(false);
        }
        fingerPrintSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor edit = prefs.edit();
                if(!isChecked){
                    edit.putBoolean(FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,false);
                    fingerPrintSwitch.setChecked(false);
                }else{
                    edit.putBoolean(FINGER_PRINT_LOCK_SELECTION_PREFERENCE_KEY,true);
                    fingerPrintSwitch.setChecked(true);
                }
                edit.apply();
            }
        });

    }

    private void setChangePinPattern(){
        changePinPattern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),SetPinPatternActivity.class)
                .putExtra(SetPinPatternActivity.INTENT_PIN_PATTERN_START_TYPE_KEY,SetPinPatternActivity.INTENT_SETTINGS));
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
        if(shouldAppLockStart) {
            startService(new Intent(getBaseContext(),AppLockingService.class));
        }
    }
}

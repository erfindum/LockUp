package com.smartfoxitsolutions.lockup;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/**
 * Created by RAAJA  on 07-09-2016.
 */
public class SetPinPatternActivity extends AppCompatActivity {

    static final String SET_PATTERN_FRAGMENT_TAG = "setPatternFragment";
    static final String SET_PIN_FRAGMENT_TAG ="setPinFragment";
    static final String INTENT_PIN_PATTERN_START_TYPE_KEY ="intentStartType";

    static final int LOCKUP_MAIN_ACTIVITY = 3;
    static final int INTENT_APP_LOADER =5;
    static final int INTENT_SETTINGS =6;
    static final int INTENT_RESET_PASSWORD =7;

    private int startType;
    private boolean shouldCloseAffinity, shouldTrackUserPresence;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_pin_pattern_activity);
        startType = getIntent().getIntExtra(INTENT_PIN_PATTERN_START_TYPE_KEY,0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        addNewFragment(SET_PATTERN_FRAGMENT_TAG);
    }

    void addNewFragment(String tag){
        FragmentManager fragManager = getFragmentManager();
        Fragment addedFragment = fragManager.findFragmentByTag(tag);
        switch (tag){
            case SET_PATTERN_FRAGMENT_TAG:
                if(addedFragment==null){
                FragmentTransaction fragTransactPattern = fragManager.beginTransaction();
                fragTransactPattern.add(R.id.set_pin_pattern_container,new SetPatternFragment(),SET_PATTERN_FRAGMENT_TAG);
                fragTransactPattern.addToBackStack(SET_PATTERN_FRAGMENT_TAG);
                fragTransactPattern.commit();
                }else {
                    fragManager.popBackStack(SET_PATTERN_FRAGMENT_TAG,0);
                }
                break;
            case SET_PIN_FRAGMENT_TAG:
                if(addedFragment==null){
                    FragmentTransaction fragTransactPattern = fragManager.beginTransaction();
                    fragTransactPattern.add(R.id.set_pin_pattern_container,new SetPinFragment(),SET_PIN_FRAGMENT_TAG);
                    fragTransactPattern.addToBackStack(SET_PIN_FRAGMENT_TAG);
                    fragTransactPattern.commit();
                }
                break;
        }

}

    void startLockUpMainActivity(){
        if(startType == INTENT_APP_LOADER) {
            startActivity(new Intent(this,SetEmailActivity.class));
            shouldTrackUserPresence = false;
        }
        if(startType == INTENT_SETTINGS){
            finish();
        }
        if(startType == INTENT_RESET_PASSWORD){
            startActivity(new Intent(getBaseContext(),LockUpMainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finishAffinity();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shouldTrackUserPresence = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("PatternLock","Back Pressed Activity");
        String frag = getFragmentManager().getBackStackEntryAt(getFragmentManager().getBackStackEntryCount()-1).getName();
        if (frag.equals(SET_PIN_FRAGMENT_TAG)){
            getFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();

    }

}

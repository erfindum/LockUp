package com.smartfoxitsolutions.lockup;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.smartfoxitsolutions.lockup.services.AppLockingService;


/**
 * Created by RAAJA  on 07-09-2016.
 */
public class SetPinPatternActivity extends AppCompatActivity {

    static final String SET_PATTERN_FRAGMENT_TAG = "setPatternFragment";
    static final String SET_PIN_FRAGMENT_TAG ="setPinFragment";
    static final String INTENT_PIN_PATTERN_START_TYPE_KEY ="intentAppLoader";

    static final int LOCKUP_MAIN_ACTIVITY = 3;
    static final int INTENT_APP_LOADER =5;
    static final int INTENT_SETTINGS =6;
    private int startType;
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
                }/*else{
                    fragManager.popBackStack(SET_PIN_FRAGMENT_TAG,0);
                }*/
                break;
        }

}

    void startLockUpMainActivity(){
        if(startType == INTENT_APP_LOADER) {
            startActivityForResult(new Intent(this, LockUpMainActivity.class), LOCKUP_MAIN_ACTIVITY);
        }
        if(startType == INTENT_SETTINGS){
            startService(new Intent(getBaseContext(),AppLockingService.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOCKUP_MAIN_ACTIVITY){
            finish();
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

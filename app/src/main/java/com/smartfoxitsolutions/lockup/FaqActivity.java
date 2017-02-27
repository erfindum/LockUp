package com.smartfoxitsolutions.lockup;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 23-02-2017.
 */

public class FaqActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private boolean shouldTrackUserPresence, shouldCloseAffinity;
    private FaqScreenOffReceiver faqScreenOffReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.faq_activity);
        shouldTrackUserPresence =true;
        toolbar = (Toolbar) findViewById(R.id.faq_activity_tool_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FragmentManager fragmentManager = getFragmentManager();
        if(fragmentManager.findFragmentByTag("faq_fragment")== null){
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.faq_activity_fragment_container,new FaqFragment(),"faq_fragment");
            transaction.addToBackStack("faq_fragment");
            transaction.commit();
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.main_screen_activity_faq_button_text));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        faqScreenOffReceiver = new FaqActivity.FaqScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(faqScreenOffReceiver,filter);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (shouldTrackUserPresence) {
            shouldCloseAffinity = true;
        } else {
            shouldCloseAffinity = false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(getFragmentManager().getBackStackEntryCount()==0){
            finish();
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(faqScreenOffReceiver);
    }

    static class FaqScreenOffReceiver extends BroadcastReceiver {

        WeakReference<FaqActivity> activity;
        FaqScreenOffReceiver(WeakReference<FaqActivity> activity){
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

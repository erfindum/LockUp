package com.smartfoxitsolutions.lockup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.TreeMap;

/**
 * Created by RAAJA on 18-10-2016.
 */

public class NotificationLockActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView notificationRecycler;
    private NotificationLockRecyclerAdapter notificationAdapter;
    private AppLockModel appLockModel;
    private AppCompatImageView imageView;
    private TextView infoText;

    private boolean shouldTrackUserPresence, shouldCloseAffinity;
    private NotificationScreenOffReceiver notificationScreenOffReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.notification_lock_activity);
        shouldTrackUserPresence = true;
        appLockModel = new AppLockModel(this.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE));
        notificationRecycler = (RecyclerView) findViewById(R.id.notification_lock_activity_recycler);
        imageView = (AppCompatImageView) findViewById(R.id.notification_lock_activity_empty_image);
        infoText = (TextView) findViewById(R.id.notification_lock_activity_empty_text);
        toolbar = (Toolbar) findViewById(R.id.notification_lock_activity_tool_bar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.notification_lock_activity_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        displayRecyclerView();
    }

    private void displayRecyclerView(){
        String[] packageNames = getIntent().getStringArrayExtra(AppLockModel.NOTIFICATION_ACTIVITY_CHECKED_APPS_PACKAGE_KEY);
        String[] appNames = getIntent().getStringArrayExtra(AppLockModel.NOTIFICATION_ACTIVITY_CHECKED_APPS_NAME_KEY);
        TreeMap<String,String> checkedAppsMap = new TreeMap<>();
        if(packageNames!=null && packageNames.length !=0) {
                for (int i = 0; i < packageNames.length; i++) {
                    checkedAppsMap.put(packageNames[i], appNames[i]);
                }

            notificationAdapter = new NotificationLockRecyclerAdapter(this, appLockModel);
            notificationAdapter.setCheckedAppsMap(checkedAppsMap);
            notificationRecycler.setAdapter(notificationAdapter);
            notificationRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        }
        else {
            imageView.setVisibility(View.VISIBLE);
            infoText.setVisibility(View.VISIBLE);
            notificationRecycler.setVisibility(View.INVISIBLE);
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
        notificationScreenOffReceiver = new NotificationScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(notificationScreenOffReceiver,filter);
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
        }
        else{
            shouldCloseAffinity = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(notificationAdapter != null){
            notificationAdapter.updateAppModel();
            notificationAdapter.closeAppLockRecyclerAdapter();
        }
        if(shouldCloseAffinity){
            finishAffinity();
        }
        if(!shouldTrackUserPresence){
            unregisterReceiver(notificationScreenOffReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(shouldTrackUserPresence){
            unregisterReceiver(notificationScreenOffReceiver);
        }
    }

    static class NotificationScreenOffReceiver extends BroadcastReceiver {

        WeakReference<NotificationLockActivity> activity;
        NotificationScreenOffReceiver(WeakReference<NotificationLockActivity> activity){
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

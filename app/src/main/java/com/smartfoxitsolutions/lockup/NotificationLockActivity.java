package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.util.TreeMap;

/**
 * Created by RAAJA on 18-10-2016.
 */

public class NotificationLockActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView notificationRecycler;
    NotificationLockRecyclerAdapter notificationAdapter;
    AppLockModel appLockModel;
    AppCompatImageView imageView;
    TextView infoText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.notification_lock_activity);
        appLockModel = new AppLockModel(this.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE));
        notificationRecycler = (RecyclerView) findViewById(R.id.notification_lock_activity_recycler);
        imageView = (AppCompatImageView) findViewById(R.id.notification_lock_activity_empty_image);
        infoText = (TextView) findViewById(R.id.notification_lock_activity_empty_text);
        toolbar = (Toolbar) findViewById(R.id.notification_lock_activity_tool_bar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.notification_lock_activity_title);
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
    protected void onDestroy() {
        super.onDestroy();
        if (notificationAdapter!=null){
            notificationAdapter.closeAppLockRecyclerAdapter();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(notificationAdapter != null){
            notificationAdapter.updateAppModel();
        }
    }
}

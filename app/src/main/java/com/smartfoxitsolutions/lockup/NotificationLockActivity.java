package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

/**
 * Created by RAAJA on 18-10-2016.
 */

public class NotificationLockActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView notificationRecycler;
    NotificationLockRecyclerAdapter notificationAdapter;
    AppLockModel appLockModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.notification_lock_activity);
        appLockModel = new AppLockModel(this.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE));
        notificationRecycler = (RecyclerView) findViewById(R.id.notification_lock_activity_recycler);
        toolbar = (Toolbar) findViewById(R.id.notification_lock_activity_tool_bar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.notification_lock_activity_title);
        }

        displayRecyclerView();
    }

    private void displayRecyclerView(){
        notificationAdapter = new NotificationLockRecyclerAdapter(this,appLockModel);
        notificationRecycler.setAdapter(notificationAdapter);
        notificationRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.VERTICAL,false));
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

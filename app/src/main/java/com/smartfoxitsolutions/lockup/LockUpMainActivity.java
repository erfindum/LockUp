package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.view.View;

/**
 * Created by RAAJA on 16-09-2016.
 */
public class LockUpMainActivity extends AppCompatActivity {

    AppCompatImageButton appLockActivityButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_up_main_activity);
        appLockActivityButton= (AppCompatImageButton) findViewById(R.id.main_screen_activity_app_lock_image);

        setImageButtonListeners();
    }

    void setImageButtonListeners(){
        appLockActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startActivity(new Intent(getBaseContext(),AppLockActivity.class));

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(AppLoaderActivity.isLockUpFirstLoad()){
            finish();
        }
    }
}

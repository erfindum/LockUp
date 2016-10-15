package com.smartfoxitsolutions.lockup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.NativeExpressAdView;
import com.smartfoxitsolutions.lockup.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveService;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultActivity;

import java.io.File;

/**
 * Created by RAAJA on 16-09-2016.
 */
public class LockUpMainActivity extends AppCompatActivity {

    AppCompatImageButton appLockActivityButton, vaultActivityButton
                            ,adEarningButton,settingsButton,faqButton;
    NativeExpressAdView adView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_up_main_activity);
        appLockActivityButton= (AppCompatImageButton) findViewById(R.id.main_screen_activity_app_lock_image);
        vaultActivityButton = (AppCompatImageButton) findViewById(R.id.main_screen_activity_vault_image);
        adEarningButton = (AppCompatImageButton) findViewById(R.id.main_screen_activity_user_profile_image);
        faqButton = (AppCompatImageButton) findViewById(R.id.main_screen_activity_faq_image);
        settingsButton= (AppCompatImageButton) findViewById(R.id.main_screen_activity_settings_image);
        adView = (NativeExpressAdView) findViewById(R.id.main_screen_activity_adView);
        setImageButtonListeners();
    }

    void setImageButtonListeners(){
        appLockActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startActivity(new Intent(getBaseContext(),AppLockActivity.class));

            }
        });

        vaultActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!MediaMoveService.SERVICE_STARTED) {
                    startActivity(new Intent(getBaseContext(), MediaVaultActivity.class));
                }else{
                    startActivity(new Intent(getBaseContext(),MediaMoveActivity.class));
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}

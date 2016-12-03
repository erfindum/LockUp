package com.smartfoxitsolutions.lockup;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.View;

import com.smartfoxitsolutions.lockup.dialogs.GrantUsageAccessDialog;
import com.smartfoxitsolutions.lockup.dialogs.OverlayPermissionDialog;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.mediavault.services.MediaMoveService;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultAlbumActivity;
import com.smartfoxitsolutions.lockup.mediavault.services.ShareMoveService;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

/**
 * Created by RAAJA on 16-09-2016.
 */
public class LockUpMainActivity extends AppCompatActivity {

    public static final int USAGE_ACCESS_PERMISSION_REQUEST=3;
    public static final int OVERLAY_PERMISSION_REQUEST = 5;
    private static final String USAGE_ACCESS_DIALOG_TAG = "usageAccessPermissionDialog";
    private static final String OVERLAY_ACCESS_DIALOG_TAG = "overlay_permission_dialog";

    private AppCompatImageButton appLockActivityButton, vaultActivityButton
                            ,adEarningButton,settingsButton,faqButton;

    private DialogFragment overlayPermissionDialog,usageDialog;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_up_main_activity);
        appLockActivityButton= (AppCompatImageButton) findViewById(R.id.main_screen_activity_app_lock_image);
        vaultActivityButton = (AppCompatImageButton) findViewById(R.id.main_screen_activity_vault_image);
        adEarningButton = (AppCompatImageButton) findViewById(R.id.main_screen_activity_user_profile_image);
        faqButton = (AppCompatImageButton) findViewById(R.id.main_screen_activity_faq_image);
        settingsButton= (AppCompatImageButton) findViewById(R.id.main_screen_activity_settings_image);
        setImageButtonListeners();
    }

    void setImageButtonListeners(){
        appLockActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    checkAndSetUsagePermissions();
                }else{
                    startActivity(new Intent(getBaseContext(),AppLockActivity.class));
                }
            }
        });

        vaultActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!MediaMoveService.SERVICE_STARTED && !ShareMoveService.SERVICE_STARTED) {
                    startActivity(new Intent(getBaseContext(), MediaVaultAlbumActivity.class));
                }else{
                    startActivity(new Intent(getBaseContext(),MediaMoveActivity.class));
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),LockUpSettingsActivity.class));
            }
        });
    }

    @TargetApi(21)
    void checkAndSetUsagePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
            if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                        checkAndSetOverlayPermission();
                }else{
                    startActivity(new Intent(getBaseContext(),AppLockActivity.class));
                }
                Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS
                        , Process.myUid(), getPackageName())
                        == AppOpsManager.MODE_ALLOWED));
            } else {
                startUsagePermissionDialog();
                Log.d(AppLockingService.TAG,"No Usage");
            }
        }
    }

    @TargetApi(23)
    void checkAndSetOverlayPermission(){
        AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
        if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Process.myUid(), getPackageName())
                == AppOpsManager.MODE_ALLOWED) {
            Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW
                    , Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED));
            startActivity(new Intent(getBaseContext(),AppLockActivity.class));
        }
        else
        {
            startOverlayPermissionDialog();
            Log.d(AppLockingService.TAG,"No Overlay");
        }
    }

    void startUsagePermissionDialog(){
        usageDialog = new GrantUsageAccessDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(USAGE_ACCESS_DIALOG_TAG);
        usageDialog.show(fragmentTransaction,USAGE_ACCESS_DIALOG_TAG);
    }

    void startOverlayPermissionDialog(){
        overlayPermissionDialog = new OverlayPermissionDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(OVERLAY_ACCESS_DIALOG_TAG);
        overlayPermissionDialog.show(fragmentTransaction,OVERLAY_ACCESS_DIALOG_TAG);
    }

    @TargetApi(21)
    public void startUsageAccessSettingActivity(){
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),USAGE_ACCESS_PERMISSION_REQUEST);
    }

    @TargetApi(23)
    public void requestOverlayPermission(){
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),OVERLAY_PERMISSION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == USAGE_ACCESS_PERMISSION_REQUEST){
            usageDialog.dismiss();
            checkAndSetUsagePermissions();
        }else
        if(requestCode == OVERLAY_PERMISSION_REQUEST){
            overlayPermissionDialog.dismiss();
            checkAndSetOverlayPermission();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

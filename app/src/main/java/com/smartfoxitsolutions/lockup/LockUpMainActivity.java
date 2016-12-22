package com.smartfoxitsolutions.lockup;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.dialogs.GrantUsageAccessDialog;
import com.smartfoxitsolutions.lockup.dialogs.OverlayPermissionDialog;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultAlbumActivity;
import com.smartfoxitsolutions.lockup.mediavault.services.MediaMoveService;
import com.smartfoxitsolutions.lockup.mediavault.services.ShareMoveService;
import com.smartfoxitsolutions.lockup.services.AppLockingService;

import java.util.ArrayList;

/**
 * Created by RAAJA on 16-09-2016.
 */
public class LockUpMainActivity extends AppCompatActivity {

    public static final int USAGE_ACCESS_PERMISSION_REQUEST=3;
    public static final int OVERLAY_PERMISSION_REQUEST = 5;
    private static final String USAGE_ACCESS_DIALOG_TAG = "usageAccessPermissionDialog";
    private static final String OVERLAY_ACCESS_DIALOG_TAG = "overlay_permission_dialog";

    public static int installedAppsCount, lockedAppsCount;
    public static boolean hasAppLockStarted;

    private AppCompatImageButton appLockActivityButton, vaultActivityButton
                            ,adEarningButton,settingsButton,faqButton;

    private DialogFragment overlayPermissionDialog,usageDialog;
    private boolean shouldTrackUserPresence, shouldCloseAffinity, shouldStartAppLock, isAppLockFirstLoad;
    private TextView installedAppNo, lockedAppNo, installedAppText, lockedAppText, appLockStatusInfo;
    private ImageButton lockButton;
    private int lockOriginalSize,lockPressedSize;
    private ImageView appLockStatusImage;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_up_main_activity);
        shouldTrackUserPresence = true;
        vaultActivityButton = (AppCompatImageButton) findViewById(R.id.lockup_main_activity_vault_image);
        settingsButton= (AppCompatImageButton) findViewById(R.id.lockup_main_activity_settings_image);
        installedAppText = (TextView) findViewById(R.id.lockup_main_activity_installed_text);
        installedAppNo = (TextView) findViewById(R.id.lockup_main_activity_installed_no);
        lockedAppText = (TextView) findViewById(R.id.lockup_main_activity_locked_text);
        lockedAppNo = (TextView) findViewById(R.id.lockup_main_activity_locked_no);
        lockButton = (ImageButton) findViewById(R.id.lockup_main_activity_lock_button);
        appLockStatusInfo = (TextView) findViewById(R.id.lockup_main_activity_applock_info);
        appLockStatusImage = (ImageView) findViewById(R.id.lockup_main_activity_applock_info_img);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/arquitectabook.ttf");
        SharedPreferences prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        shouldStartAppLock = prefs.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = prefs.getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,false);
        AppLockModel appLockModel = new AppLockModel(prefs);
        installedAppsCount = appLockModel.getInstalledAppsPackage().size();
        lockedAppsCount = appLockModel.getCheckedAppsPackage().size();
        hasAppLockStarted = prefs.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        setTypeFace(typeface);
        setLockButtonSize();
        setImageButtonListeners();
        setBackground();
    }

    void setTypeFace(Typeface typeFace){
        installedAppNo.setTypeface(typeFace);
        lockedAppNo.setTypeface(typeFace);
        setInfoText();
    }

    void setInfoText(){
        String installedAppsCountText = String.valueOf(installedAppsCount);
        String lockedAppsCountText = String.valueOf(lockedAppsCount);
        if(installedAppsCountText.equals("0")){
            installedAppNo.setText("0");
        }else{
            String[] installedSplit = installedAppsCountText.split("");
            String installCountSpaced = TextUtils.join(" ",installedSplit);
            installedAppNo.setText(installCountSpaced);
        }

        if(lockedAppsCountText.equals("0")){
            lockedAppNo.setText("0");
        }else{
            String[] lockedAppSplit = lockedAppsCountText.split("");
            String lockedAppCountSpaced = TextUtils.join(" ",lockedAppSplit);
            lockedAppNo.setText(lockedAppCountSpaced);
        }

        if(hasAppLockStarted){
            appLockStatusImage.setImageResource(R.drawable.img_main_screen_lock_on);
            appLockStatusInfo.setText(getString(R.string.main_screen_activity_app_lock_info_On));
        }else{
            appLockStatusImage.setImageResource(R.drawable.img_main_screen_lock_off);
            appLockStatusInfo.setText(getString(R.string.main_screen_activity_app_lock_info_Off));
        }
    }

    void setLockButtonSize(){
        lockOriginalSize = Math.round(DimensionConverter.convertDpToPixel(160,this));
        lockPressedSize = Math.round(DimensionConverter.convertDpToPixel(150,this));
    }

    void setImageButtonListeners(){
        lockButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) lockButton.getLayoutParams();
                if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
                    parms.width = lockPressedSize;
                    parms.height = lockPressedSize;
                    lockButton.setLayoutParams(parms);
                    return true;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP){
                    parms.width = lockOriginalSize;
                    parms.height = lockOriginalSize;
                    lockButton.setLayoutParams(parms);
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                        checkAndSetUsagePermissions();
                    }else{
                        startActivity(new Intent(getBaseContext(),AppLockActivity.class));
                        shouldTrackUserPresence = false;
                    }
                    return true;
                }
                return false;
            }
        });

        vaultActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!MediaMoveService.SERVICE_STARTED && !ShareMoveService.SERVICE_STARTED) {
                    startActivity(new Intent(getBaseContext(), MediaVaultAlbumActivity.class));
                    shouldTrackUserPresence = false;
                }else{
                    startActivity(new Intent(getBaseContext(),MediaMoveActivity.class));
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),LockUpSettingsActivity.class));
                shouldTrackUserPresence = false;
            }
        });
    }

    void setBackground(){
        Drawable drawable = ContextCompat.getDrawable(getBaseContext(),R.drawable.main_screen_background);
        getWindow().getDecorView().setBackground(drawable);
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
                    shouldTrackUserPresence =false;
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
            shouldTrackUserPresence = false;
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
        shouldTrackUserPresence = false;
    }

    @TargetApi(23)
    public void requestOverlayPermission(){
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),OVERLAY_PERMISSION_REQUEST);
        shouldTrackUserPresence = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == USAGE_ACCESS_PERMISSION_REQUEST){
            checkAndSetUsagePermissions();
            shouldTrackUserPresence = true;
        }else
        if(requestCode == OVERLAY_PERMISSION_REQUEST){
            checkAndSetOverlayPermission();
            shouldTrackUserPresence = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shouldTrackUserPresence = true;
        setInfoText();
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
    public void onBackPressed() {
        super.onBackPressed();
        startAppLock();
        finishAffinity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            startAppLock();
            finishAffinity();
        }
    }

    void startAppLock(){
        if(shouldStartAppLock && !isAppLockFirstLoad){
            if(!AppLockingService.isAppLockRunning) {
                startService(new Intent(getBaseContext(), AppLockingService.class));
            }
        }
    }
}

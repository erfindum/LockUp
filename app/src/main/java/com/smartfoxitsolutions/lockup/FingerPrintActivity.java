package com.smartfoxitsolutions.lockup;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.smartfoxitsolutions.lockup.services.AppLockingService;

/**
 * Created by RAAJA on 01-11-2016.
 */

public class FingerPrintActivity extends AppCompatActivity {
    private static AppLockingService lockingService;

    public static void updateService(AppLockingService service){
        lockingService = service;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(lockingService!=null && AppLockingService.isAppLockRunning) {
            lockingService.updateCancelSignal();
        }
        lockingService = null;
        finish();
    }
}

package com.smartfoxitsolutions.lockup;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by RAAJA on 20-12-2016.
 */

public class LockUpApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    /* if(LeakCanary.isInAnalyzerProcess(this)){
            return;
        }
        LeakCanary.install(this); */
    }
}

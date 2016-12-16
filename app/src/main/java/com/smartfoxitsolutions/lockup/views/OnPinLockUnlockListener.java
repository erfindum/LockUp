package com.smartfoxitsolutions.lockup.views;

/**
 * Created by RAAJA on 06-10-2016.
 */

public interface OnPinLockUnlockListener {
    void onPinUnlocked();

    void onPinLocked(String packageName);
}

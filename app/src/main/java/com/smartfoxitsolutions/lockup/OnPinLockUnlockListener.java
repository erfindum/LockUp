package com.smartfoxitsolutions.lockup;

/**
 * Created by RAAJA on 06-10-2016.
 */

public interface OnPinLockUnlockListener {
    void onPinUnlocked(String packageName);

    void onPinLocked();
}

package com.smartfoxitsolutions.lockup;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.smartfoxitsolutions.lockup.mediavault.MediaVaultActivity;

/**
 * Created by RAAJA on 24-09-2016.
 */

public class OverlayPermissionDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext())
                .setMessage(R.string.media_vault_permission_request_message)
                .setPositiveButton(R.string.media_vault_permission_grant, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppLockActivity appLockActivity = (AppLockActivity) getActivity();
                        appLockActivity.requestOverlayPermission();
                    }
                })
                .setNegativeButton(R.string.media_vault_permission_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MediaVaultActivity mediaVaultActivity = (MediaVaultActivity) getActivity();
                        dialog.dismiss();
                    }
                });
        return dialogBuilder.create();
    }
}

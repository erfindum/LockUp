package com.smartfoxitsolutions.lockup;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class GrantUsageAccessDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setMessage(R.string.appLock_activity_usage_dialog_message)
                .setPositiveButton(R.string.appLock_activity_usage_dialog_permit_text
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                AppLockActivity appLockActivity = (AppLockActivity) getActivity();
                                appLockActivity.startUsageAccessSettingActivity();
                            }
                        })
                .setNegativeButton(R.string.appLock_activity_usage_dialog_cancel_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        });
        return dialogBuilder.create();
    }
}

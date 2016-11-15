package com.smartfoxitsolutions.lockup.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockActivity;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 27-10-2016.
 */

public class StartAppLockDialog extends DialogFragment {
    TextView positive, negative;
    private int position;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.should_start_app_lock_dialog,container,false);
        positive = (TextView) parent.findViewById(R.id.should_start_app_lock_positive_button);
        negative = (TextView) parent.findViewById(R.id.should_start_app_lock_negative_button);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return parent;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final AppLockActivity activity = (AppLockActivity) getActivity();
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppLockActivity.shouldStartAppLock = true;
                SharedPreferences.Editor edit = getActivity().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME
                        , Context.MODE_PRIVATE).edit();
                edit.putBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,true);
                edit.apply();
                dismiss();
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}

package com.smartfoxitsolutions.lockup.dialogs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockActivity;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 24-10-2016.
 */

public class NotificationPermissionDialog extends DialogFragment {

    AppCompatImageView dialogIcon;
    TextView infoText,infoTextSub, positiveButton, negativeButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.lockup_default_dialog,container,false);
        dialogIcon = (AppCompatImageView) parent.findViewById(R.id.lockup_default_dialog_image);
        infoText = (TextView) parent.findViewById(R.id.lockup_default_dialog_info_text);
        infoTextSub = (TextView) parent.findViewById(R.id.lockup_default_dialog_info_text_sub);
        positiveButton = (TextView) parent.findViewById(R.id.lockup_default_dialog_positive_button);
        negativeButton = (TextView) parent.findViewById(R.id.lockup_default_dialog_negative_button);
        dialogIcon.setImageResource(R.drawable.ic_lock_notif_permission_icon);
        infoText.setText(R.string.appLock_activity_notif_dialog_message);
        infoTextSub.setText(R.string.appLock_activity_notif_dialog_message_two);
        positiveButton.setText(R.string.appLock_activity_usage_dialog_permit_text);
        negativeButton.setText(R.string.appLock_activity_usage_dialog_cancel_text);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final AppLockActivity activity = (AppLockActivity) getActivity();
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.requestNotificationPermission();
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}



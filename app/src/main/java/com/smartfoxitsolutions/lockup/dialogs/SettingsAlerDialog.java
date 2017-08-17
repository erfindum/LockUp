package com.smartfoxitsolutions.lockup.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 20-02-2017.
 */

public class SettingsAlerDialog extends DialogFragment {

    TextView infoText,infoTextSub;
    Button negativeButton,positiveButton;
    public static final String SETTINGS_ALERT_MESSAGE = "settingsAlertMessage";
    LockUpSettingsActivity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.redeem_info_dialog,container,false);
        infoText = (TextView) parent.findViewById(R.id.redeem_dialog_header);
        infoTextSub = (TextView) parent.findViewById(R.id.redeem_dialog_message);
        negativeButton = (Button) parent.findViewById(R.id.redeem_dialog_negative_button);
        positiveButton = (Button) parent.findViewById(R.id.redeem_dialog_positive_button);
        infoText.setText(R.string.settings_prevent_uninstall_alert_title);
        //String infoString = Html.fromHtml(getArguments().getString(SETTINGS_ALERT_MESSAGE)).toString();
        infoTextSub.setText(R.string.settings_prevent_uninstall_alert_message);
        positiveButton.setText(R.string.settings_prevent_uninstall_positive);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LockUpSettingsActivity) getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        final View dialogView = getDialog().getWindow().getDecorView();

        ObjectAnimator displayAnimator = ObjectAnimator.ofPropertyValuesHolder(
                dialogView,
                PropertyValuesHolder.ofFloat("scaleX", 0.0f, 1.1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.0f, 1.1f),
                PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f));
        displayAnimator.setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator());
        displayAnimator.start();

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator dismissAnimator = ObjectAnimator.ofPropertyValuesHolder(
                        dialogView,
                        PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.0f));
                dismissAnimator.setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator());
                dismissAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator dismissAnimator = ObjectAnimator.ofPropertyValuesHolder(
                        dialogView,
                        PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.0f));
                dismissAnimator.setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator());
                dismissAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        activity.openDeviceAdminPermission();
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activity = null;
    }
}

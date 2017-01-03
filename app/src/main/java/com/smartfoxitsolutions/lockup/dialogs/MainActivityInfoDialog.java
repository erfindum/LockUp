package com.smartfoxitsolutions.lockup.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.LockUpMainActivity;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 30-12-2016.
 */

public class MainActivityInfoDialog extends DialogFragment {

    private ImageView dialogIcon;
    private TextView infoText, positiveButton;
    public static boolean shouldDisplayDialog = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.lockup_main_info_dialog,container,false);
        dialogIcon = (ImageView) parent.findViewById(R.id.lockup_main_dialog_icon);
        infoText = (TextView) parent.findViewById(R.id.lockup_main_dialog_message);
        positiveButton = (TextView) parent.findViewById(R.id.lockup_main_dialog_positive_button);
        dialogIcon.setImageResource(R.drawable.ic_main_screen_lock);
        infoText.setText(R.string.main_screen_activity_lock_info_message);
        positiveButton.setText(R.string.settings_dialog_finger_negative_text);
        shouldDisplayDialog = false;
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        return parent;
    }

    @Override
    public void onStart() {
        super.onStart();

        final View dialogView = getDialog().getWindow().getDecorView();
        final LockUpMainActivity activity = (LockUpMainActivity) getActivity();
        ObjectAnimator displayAnimator = ObjectAnimator.ofPropertyValuesHolder(
                dialogView,
                PropertyValuesHolder.ofFloat("scaleX",0.0f,1.0f),
                PropertyValuesHolder.ofFloat("scaleY",0.0f,1.0f),
                PropertyValuesHolder.ofFloat("alpha",0.0f,1.0f));
        displayAnimator.setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator());
        displayAnimator.start();

        final ObjectAnimator lockIconResizeAnimator = ObjectAnimator.ofPropertyValuesHolder(
                dialogIcon,PropertyValuesHolder.ofFloat("scaleX",0.9f,1.0f),
                PropertyValuesHolder.ofFloat("scaleY",0.9f,1.0f)
        );
        lockIconResizeAnimator.setDuration(400);

        final ObjectAnimator lockIconAnimator = ObjectAnimator.ofPropertyValuesHolder(
                dialogIcon,PropertyValuesHolder.ofFloat("scaleX",1.0f,0.9f),
                PropertyValuesHolder.ofFloat("scaleY",1.0f,0.9f)
        );
        lockIconAnimator.setDuration(400);
        lockIconAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                lockIconResizeAnimator.start();
            }
        });
        lockIconResizeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                lockIconAnimator.start();
            }
        });

        lockIconAnimator.start();

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator dismissAnimator = ObjectAnimator.ofPropertyValuesHolder(
                        dialogView,
                        PropertyValuesHolder.ofFloat("alpha",1.0f,0.0f));
                dismissAnimator.setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator());
                dismissAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        activity.closeAppLockInfoDialog();
                        shouldDisplayDialog = true;
                        lockIconAnimator.cancel();
                        lockIconResizeAnimator.cancel();
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });
    }
}

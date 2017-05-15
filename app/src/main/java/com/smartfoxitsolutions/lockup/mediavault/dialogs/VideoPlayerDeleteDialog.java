package com.smartfoxitsolutions.lockup.mediavault.dialogs;

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
import android.widget.Button;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.VaultVideoPlayerActivity;

/**
 * Created by RAAJA on 03-12-2016.
 */

public class VideoPlayerDeleteDialog extends DialogFragment {

    AppCompatImageView dialogIcon;
    TextView infoText,infoTextSub;
    Button positiveButton, negativeButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.lockup_default_dialog,container,false);
        dialogIcon = (AppCompatImageView) parent.findViewById(R.id.lockup_default_dialog_image);
        infoText = (TextView) parent.findViewById(R.id.lockup_default_dialog_info_text);
        infoTextSub = (TextView) parent.findViewById(R.id.lockup_default_dialog_info_text_sub);
        positiveButton = (Button) parent.findViewById(R.id.lockup_default_dialog_positive_button);
        negativeButton = (Button) parent.findViewById(R.id.lockup_default_dialog_negative_button);
        dialogIcon.setImageResource(R.drawable.ic_app_lock_activity_alert);
        infoText.setText(R.string.vault_move_dialog_delete_title_text);
        infoTextSub.setText(R.string.vault_move_dialog_delete_sub_text);
        positiveButton.setText(R.string.vault_move_dialog_delete_positive);
        negativeButton.setText(R.string.vault_move_dialog_move_negative);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        return parent;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View dialogView = getDialog().getWindow().getDecorView();
        final VaultVideoPlayerActivity activity = (VaultVideoPlayerActivity) getActivity();

        ObjectAnimator displayAnimator = ObjectAnimator.ofPropertyValuesHolder(
                dialogView,
                PropertyValuesHolder.ofFloat("scaleX",0.0f,1.1f),
                PropertyValuesHolder.ofFloat("scaleY",0.0f,1.1f),
                PropertyValuesHolder.ofFloat("alpha",0.0f,1.0f));
        displayAnimator.setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator());
        displayAnimator.start();

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
                        activity.deleteVideo();
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
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
                        activity.deleteVideoCancelled();
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });
    }
}

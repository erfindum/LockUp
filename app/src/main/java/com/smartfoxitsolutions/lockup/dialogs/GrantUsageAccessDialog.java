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
import android.widget.Button;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.LockUpMainActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyUserActivity;

/**
 * Created by RAAJA on 11-09-2016.
 */
public class GrantUsageAccessDialog extends DialogFragment {

    private AppCompatImageView dialogIcon;
    private TextView infoText,infoTextSub;
    private Button positiveButton, negativeButton;
    private String startType;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.lockup_permission_dialog,container,false);
        dialogIcon = (AppCompatImageView) parent.findViewById(R.id.lockup_permission_dialog_image);
        infoText = (TextView) parent.findViewById(R.id.lockup_permission_dialog_info_text);
        infoTextSub = (TextView) parent.findViewById(R.id.lockup_permission_dialog_info_text_sub);
        positiveButton = (Button) parent.findViewById(R.id.lockup_permission_dialog_positive_button);
        negativeButton = (Button) parent.findViewById(R.id.lockup_permission_dialog_negative_button);
        dialogIcon.setImageResource(R.drawable.ic_lock_usage_permission_icon);
        if(getArguments()!=null){
            startType = getArguments().getString("grandUsageStartType");
        }else{
            startType = "appLockStart";
        }
        infoText.setText(R.string.appLock_activity_usage_dialog_message);
        infoTextSub.setVisibility(View.GONE);
        positiveButton.setText(R.string.appLock_activity_usage_dialog_permit_text);
        negativeButton.setText(R.string.appLock_activity_usage_dialog_cancel_text);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        return parent;
    }

    @Override
    public void onStart() {
        super.onStart();

        final View dialogView = getDialog().getWindow().getDecorView();
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
                        startUsageAccessSettingActivity();
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
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });
    }

    void startUsageAccessSettingActivity(){
        if(startType.equals("appLockStart")) {
            final LockUpMainActivity activity = (LockUpMainActivity) getActivity();
            activity.startUsageAccessSettingActivity();
        }else
        if(startType.equals("loyaltyBonusStart")){
            final LoyaltyUserActivity activity = (LoyaltyUserActivity) getActivity();
            activity.startUsageAccessSettingActivity();
        }
    }
}

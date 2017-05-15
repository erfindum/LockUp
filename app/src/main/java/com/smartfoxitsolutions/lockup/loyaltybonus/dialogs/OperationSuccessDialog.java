package com.smartfoxitsolutions.lockup.loyaltybonus.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusMain;

/**
 * Created by RAAJA on 22-01-2017.
 */

public class OperationSuccessDialog extends DialogFragment {

    TextView infoText,infoTextSub;
    Button negativeButton;
    ProgressBar progressBar;
    LoyaltyBonusMain activity;

    public static final String NETWORK_INFO_MESSAGE = "networkInfoMessage";
    public static final String NETWORK_INFO_BUTTON = "networkInfoButton";

    public static final String OPERATION_TYPE_KEY = "operationType";
    public static final int OPERATION_TYPE_SINGUP_SUCCESS = 34;
    public static final int OPERATION_TYPE_RESET_SUCCESS = 35;

    int operationType;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.network_dialog,container,false);
        infoText = (TextView) parent.findViewById(R.id.network_dialog_header);
        infoTextSub = (TextView) parent.findViewById(R.id.network_dialog_message);
        negativeButton = (Button) parent.findViewById(R.id.network_dialog_negative_button);
        progressBar = (ProgressBar) parent.findViewById(R.id.network_dialog_progress);
        progressBar.setVisibility(View.GONE);
        infoText.setText(getString(R.string.reset_pin_pattern_network_success_header));
        operationType = getArguments().getInt(OPERATION_TYPE_KEY);
        infoTextSub.setText(getArguments().getString(NETWORK_INFO_MESSAGE));
        negativeButton.setText(getArguments().getString(NETWORK_INFO_BUTTON));
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyBonusMain) getActivity();
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
                        if(operationType == OPERATION_TYPE_SINGUP_SUCCESS){
                            activity.signUpSuccess();
                        }
                        if(operationType == OPERATION_TYPE_RESET_SUCCESS){
                            activity.passwordResetSuccess();
                        }
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

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 15-12-2016.
 */

public class NetworkProcessDialog extends DialogFragment {

    TextView infoText,infoTextSub;
    ProgressBar progressBar;
    Button negativeButton;

    public static final String NETWORK_INFO_HEADER = "networkInfoHeader";
    public static final String NETWORK_INFO_MESSAGE = "networkInfoMessage";
    public static final String NETWORK_INFO_BUTTON = "networkInfoButton";

    public static final String NETWORK_DIALOG_TYPE = "networkDialogType";
    public static final int NETWORK_DIALOG_TYPE_LOADING = 34;
    public static final int NETWORK_DIALOG_TYPE_COMPLETE = 35;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.network_dialog,container,false);
        infoText = (TextView) parent.findViewById(R.id.network_dialog_header);
        infoTextSub = (TextView) parent.findViewById(R.id.network_dialog_message);
        negativeButton = (Button) parent.findViewById(R.id.network_dialog_negative_button);
        progressBar = (ProgressBar) parent.findViewById(R.id.network_dialog_progress);
        View separator = parent.findViewById(R.id.network_dialog_divider);
        if(getArguments().getInt(NETWORK_DIALOG_TYPE)==NETWORK_DIALOG_TYPE_COMPLETE) {
            progressBar.setVisibility(View.GONE);
            infoText.setText(getArguments().getString(NETWORK_INFO_HEADER));
            infoTextSub.setText(getArguments().getString(NETWORK_INFO_MESSAGE));
            negativeButton.setText(getArguments().getString(NETWORK_INFO_BUTTON));
        }
        if(getArguments().getInt(NETWORK_DIALOG_TYPE) == NETWORK_DIALOG_TYPE_LOADING){
            infoText.setText(getArguments().getString(NETWORK_INFO_HEADER));
            infoTextSub.setText(getArguments().getString(NETWORK_INFO_MESSAGE));
            negativeButton.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
        }
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
}

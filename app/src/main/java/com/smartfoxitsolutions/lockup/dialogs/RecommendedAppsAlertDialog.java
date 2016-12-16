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
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockActivity;
import com.smartfoxitsolutions.lockup.AppLockRecyclerViewItem;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 25-10-2016.
 */

public class RecommendedAppsAlertDialog extends DialogFragment {

    AppCompatImageView dialogIcon;
    TextView infoText,infoTextSub, positiveButton, negativeButton;
    private int position;
    private AppLockRecyclerViewItem itemView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.lockup_default_dialog,container,false);
        dialogIcon = (AppCompatImageView) parent.findViewById(R.id.lockup_default_dialog_image);
        infoText = (TextView) parent.findViewById(R.id.lockup_default_dialog_info_text);
        infoTextSub = (TextView) parent.findViewById(R.id.lockup_default_dialog_info_text_sub);
        positiveButton = (TextView) parent.findViewById(R.id.lockup_default_dialog_positive_button);
        negativeButton = (TextView) parent.findViewById(R.id.lockup_default_dialog_negative_button);
        dialogIcon.setImageResource(R.drawable.ic_app_lock_activity_alert);
        infoText.setText(R.string.recommended_unlock_alert_text);
        infoTextSub.setVisibility(View.GONE);
        positiveButton.setText(R.string.recommended_unlock_alert_dialog_positive_text);
        negativeButton.setText(R.string.recommended_unlock_alert_dialog_negative_text);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        return parent;
    }

    private int getPosition(){
       return this .position;
    }

    public void setPosition(int position){
        this.position = position;
    }

    private AppLockRecyclerViewItem getItemView(){
        return this .itemView;
    }

    public void setItemView(AppLockRecyclerViewItem itemView){
        this.itemView = itemView;
    }

    @Override
    public void onStart() {
        super.onStart();
        final View dialogView = getDialog().getWindow().getDecorView();
        final AppLockActivity activity = (AppLockActivity) getActivity();

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
                        activity.unlockRecommendedApp(itemView,getPosition());
                        itemView = null;
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
                        itemView = null;
                        dismiss();
                    }
                });
                dismissAnimator.start();
            }
        });
    }
}

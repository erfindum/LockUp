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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final AppLockActivity activity = (AppLockActivity) getActivity();
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.unlockRecommendedApp(itemView,getPosition());
                dismiss();
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemView = null;
                dismiss();
            }
        });
    }
}

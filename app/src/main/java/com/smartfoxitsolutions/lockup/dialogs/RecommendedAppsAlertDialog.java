package com.smartfoxitsolutions.lockup.dialogs;

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
import com.smartfoxitsolutions.lockup.AppLockRecyclerViewItem;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 25-10-2016.
 */

public class RecommendedAppsAlertDialog extends DialogFragment {

    TextView positive, negative;
    private int position;
    private AppLockRecyclerViewItem itemView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.recommended_apps_alert_dialog,container,false);
        positive = (TextView) parent.findViewById(R.id.recommended_alert_positive_button);
        negative = (TextView) parent.findViewById(R.id.recommended_alert_negative_button);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.unlockRecommendedApp(itemView,getPosition());
                dismiss();
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemView = null;
                dismiss();
            }
        });
    }
}

package com.smartfoxitsolutions.lockup;



import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.mediavault.MediaVaultActivity;

/**
 * Created by RAAJA on 24-09-2016.
 */

public class OverlayPermissionDialog extends DialogFragment {
    TextView positive, negative;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.overlay_permission_dialog,container,false);
        positive = (TextView) parent.findViewById(R.id.overlay_permission_positive_button);
        negative = (TextView) parent.findViewById(R.id.overlay_permission_negative_button);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final AppLockActivity activity = (AppLockActivity) getActivity();
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.requestOverlayPermission();
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}

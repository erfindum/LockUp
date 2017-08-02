package com.smartfoxitsolutions.lockup.earnmore;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 04-07-2017.
 */

public class OffersFragment extends Fragment {

    RelativeLayout offerToroLayout;
    EarnMoreActivity earnActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         super.onCreateView(inflater, container, savedInstanceState);

        View parent  = inflater.inflate(R.layout.earn_offerwall_fragment,container,false);
        offerToroLayout = (RelativeLayout) parent.findViewById(R.id.layout_offertoro_offerwall);
        return parent;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
       setEarnActivity((EarnMoreActivity) getActivity());
    }

    private void setEarnActivity(EarnMoreActivity activityEarn){
        earnActivity = activityEarn;
    }

    private EarnMoreActivity getEarnActivity(){
        return earnActivity;
    }

    @Override
    public void onStart() {
        super.onStart();
        offerToroLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getEarnActivity().openOTOfferWall();
            }
        });
    }
}

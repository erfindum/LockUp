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

public class SurveyFragment extends Fragment implements View.OnClickListener {

    RelativeLayout pollfishLayout, offerToroLayout;
    EarnMoreActivity earnMoreActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View parent = inflater.inflate(R.layout.earn_survey_fragment,container,false);
        pollfishLayout = (RelativeLayout) parent.findViewById(R.id.layout_pollfish);
        offerToroLayout = (RelativeLayout) parent.findViewById(R.id.layout_offertoro);
        return parent;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEarnMoreActivity((EarnMoreActivity) getActivity());
    }

    private void setEarnMoreActivity(EarnMoreActivity activity){
        earnMoreActivity = activity;
    }

    private EarnMoreActivity getEarnMoreActivity(){
        return earnMoreActivity;
    }

    @Override
    public void onStart() {
        super.onStart();
        pollfishLayout.setOnClickListener(this);
        offerToroLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.layout_pollfish){
            getEarnMoreActivity().openPollfishPanel();
            return;
        }
        if(v.getId() == R.id.layout_offertoro){
            getEarnMoreActivity().openOTSurveyWall();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        pollfishLayout.setOnClickListener(null);
        offerToroLayout.setOnClickListener(null);
    }
}

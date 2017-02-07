package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 30-01-2017.
 */

public class LoyaltyUserRedeemFragment extends Fragment {

    private AppCompatImageView redeemTypeImage;
    private RelativeLayout redeemPointsOne, redeemPointsTwo,redeemPointsThree,redeemPointsFour,redeemPointsFive,redeemPointsSix
                ,redeemPointsSeven;
    private Button redeemButton;
    private int selectedDenomination;
    private RelativeLayout currentSelection;
    private LoyaltyUserActivity activity;
    private String type;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_redeem_points,container,false);
        redeemTypeImage = (AppCompatImageView) parent.findViewById(R.id.loyalty_bonus_redeem_points_type);

        redeemPointsOne = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_one_group);
        redeemPointsTwo = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_two_group);
        redeemPointsThree = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_three_group);
        redeemPointsFour = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_four_group);
        redeemPointsFive = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_five_group);
        redeemPointsSix = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_six_group);
        redeemPointsSeven = (RelativeLayout) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_seven_group);

        redeemButton = (Button) parent.findViewById(R.id.loyalty_bonus_redeem_points_button);
        Bundle args = getArguments();
        if(args!=null){
            type = args.getString("redeemType");
        }
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyUserActivity) getActivity();
        setListeners();
    }

    void setListeners(){
        redeemPointsOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 1;
                currentSelection = redeemPointsOne;
                redeemPointsOne.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemPointsTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 2;
                currentSelection = redeemPointsTwo;
                redeemPointsTwo.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemPointsThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 3;
                currentSelection = redeemPointsThree;
                redeemPointsThree.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemPointsFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 4;
                currentSelection = redeemPointsFour;
                redeemPointsFour.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemPointsFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 5;
                currentSelection = redeemPointsFive;
                redeemPointsFive.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemPointsSix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 6;
                currentSelection = redeemPointsSix;
                redeemPointsSix.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemPointsSeven.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentSelection!=null){
                    currentSelection.setBackground(null);
                }
                selectedDenomination = 7;
                currentSelection = redeemPointsSeven;
                redeemPointsSeven.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedDenomination>0) {
                    activity.startRedeemFinal(type, selectedDenomination);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if(type.equals("paypal")){
            redeemTypeImage.setImageResource(R.drawable.ic_paypal_logo);
        }
        if(type.equals("paytm")){
            redeemTypeImage.setImageResource(R.drawable.ic_paytm_logo);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}

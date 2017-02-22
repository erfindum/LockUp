package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.RedeemErrorDialog;

/**
 * Created by RAAJA on 30-01-2017.
 */

public class LoyaltyUserRedeemFragment extends Fragment {

    private AppCompatImageView redeemTypeImage;
    private RelativeLayout redeemPointsOne, redeemPointsTwo,redeemPointsThree,redeemPointsFour,redeemPointsFive,redeemPointsSix
                ,redeemPointsSeven;
    private TextView redeemOneCredit,redeemTwoCredit,redeemThreeCredit,redeemFourCredit,redeemFiveCredit,redeemSixCredit,
            redeemSevenCredit, userTotalPoints;
    private Button redeemButton;
    private int selectedDenomination;
    private RelativeLayout currentSelection;
    private LoyaltyUserActivity activity;
    private String type;
    private DialogFragment errorFragment;

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

        redeemOneCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_one_credit);
        redeemTwoCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_two_credit);
        redeemThreeCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_three_credit);
        redeemFourCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_four_credit);
        redeemFiveCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_five_credit);
        redeemSixCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_six_credit);
        redeemSevenCredit = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_seven_credit);

        userTotalPoints = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_points_redeem_info);

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
                selectedDenomination = 0;
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
                selectedDenomination = 1;
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
                selectedDenomination = 2;
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
                selectedDenomination = 3;
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
                selectedDenomination = 4;
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
                selectedDenomination = 5;
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
                selectedDenomination = 6;
                currentSelection = redeemPointsSeven;
                redeemPointsSeven.setBackgroundResource(R.drawable.img_redeem_point_group);
            }
        });

        redeemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedDenomination>-1) {
                    activity.startRedeemFinal(type, selectedDenomination);

                   /* double earnedLoyaltyPoints  = Double.parseDouble(userTotalPoints.getText().toString());
                    if(validateRedeemablePoints(earnedLoyaltyPoints,selectedDenomination)) {
                        activity.startRedeemFinal(type, selectedDenomination);
                    }else{
                        if(errorFragment !=null){
                            errorFragment.dismiss();
                        }
                        errorFragment = new RedeemErrorDialog();
                        Bundle argBundle = new Bundle();
                        argBundle.putString(RedeemErrorDialog.REDEEM_ERROR_MESSAGE,
                                getString(R.string.loyalty_bonus_redeem_low_point_error));
                        errorFragment.setArguments(argBundle);
                        errorFragment.show(activity.getSupportFragmentManager(),"redeem_points_error");
                    }*/
                }
            }
        });
    }

    boolean validateRedeemablePoints(double earnedPoints, int selectedDenomination){
        switch (selectedDenomination){
            case 0:
                return earnedPoints>200;
            case 1:
                return earnedPoints>300;
            case 2:
                return earnedPoints>500;
            case 3:
                return earnedPoints>1000;
            case 4:
                return earnedPoints>5000;
            case 5:
                return earnedPoints>10000;
            case 6:
                return earnedPoints>20000;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        setDenomination();
        userTotalPoints.setText(activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,Context.MODE_PRIVATE)
                        .getString(LoyaltyBonusModel.USER_LOYALTY_BONUS,"00.00"));
    }

    void setDenomination(){
        if(type.equals("paypal")){
            redeemTypeImage.setImageResource(R.drawable.ic_paypal_logo);
            int[] credit = activity.getResources().getIntArray(R.array.loyalty_bonus_redeem_paypal_credits);
            redeemOneCredit.setText(getCreditText(credit[0]));
            redeemTwoCredit.setText(getCreditText(credit[1]));
            redeemThreeCredit.setText(getCreditText(credit[2]));
            redeemFourCredit.setText(getCreditText(credit[3]));
            redeemFiveCredit.setText(getCreditText(credit[4]));
            redeemSixCredit.setText(getCreditText(credit[5]));
            redeemSevenCredit.setText(getCreditText(credit[6]));
        }
        if(type.equals("paytm")){
            redeemTypeImage.setImageResource(R.drawable.ic_paytm_logo);
            int[] credit = activity.getResources().getIntArray(R.array.loyalty_bonus_redeem_paytm_credits);
            redeemOneCredit.setText(getCreditText(credit[0]));
            redeemTwoCredit.setText(getCreditText(credit[1]));
            redeemThreeCredit.setText(getCreditText(credit[2]));
            redeemFourCredit.setText(getCreditText(credit[3]));
            redeemFiveCredit.setText(getCreditText(credit[4]));
            redeemSixCredit.setText(getCreditText(credit[5]));
            redeemSevenCredit.setText(getCreditText(credit[6]));
        }

        selectedDenomination = 0;
        currentSelection = redeemPointsOne;
        redeemPointsOne.setBackgroundResource(R.drawable.img_redeem_point_group);
    }

    String getCreditText(int credit){
        return "$"+credit;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}

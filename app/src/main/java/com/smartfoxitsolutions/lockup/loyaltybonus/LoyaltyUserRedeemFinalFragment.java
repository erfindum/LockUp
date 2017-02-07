package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 30-01-2017.
 */

public class LoyaltyUserRedeemFinalFragment extends Fragment {

    private AppCompatImageView redeemTypeImage;
    private TextView denomination, denominationPoints, enterIdInfo;
    private EditText enterIdEdit;
    private Button redeemPointsButton;
    private String redeemType;
    private int selection;
    private LoyaltyUserActivity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_redeem_final,container,false);

        denomination = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_final_redeem_credit);
        denominationPoints = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_final_redeem_info);
        enterIdInfo = (TextView) parent.findViewById(R.id.loyalty_bonus_redeem_final_head);

        enterIdEdit = (EditText) parent.findViewById(R.id.loyalty_bonus_redeem_final_id_edit);

        redeemPointsButton = (Button) parent.findViewById(R.id.loyalty_bonus_redeem_final_button);
        redeemTypeImage = (AppCompatImageView) parent.findViewById(R.id.loyalty_bonus_redeem_final_type);

        Bundle args = getArguments();
        if(args!=null){
            redeemType = args.getString("redeemFinalType");
            selection = args.getInt("redeemFinalSelection");
        }

        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyUserActivity) getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(redeemType.equals("paypal")){
            enterIdInfo.setText(getString(R.string.loyalty_redeem_final_paypal_info));
            enterIdEdit.setHint(getString(R.string.loyalty_redeem_final_paypal_hint));
            redeemTypeImage.setImageResource(R.drawable.ic_paypal_logo);
        }
        if(redeemType.equals("paytm")){
            enterIdInfo.setText(getString(R.string.loyalty_redeem_final_paytm_info));
            enterIdEdit.setHint(getString(R.string.loyalty_redeem_final_paytm_hint));
            redeemTypeImage.setImageResource(R.drawable.ic_paytm_logo);
        }
        setDenomination(selection);
    }

    private void setDenomination(int selection){
        switch (selection){
            case 1:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_one_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_one_points));
                return;
            case 2:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_two_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_two_points));
                return;
            case 3:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_three_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_three_points));
                return;
            case 4:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_four_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_four_points));
                return;
            case 5:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_five_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_five_points));
                return;
            case 6:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_six_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_six_points));
                return;
            case 7:
                denomination.setText(getString(R.string.loyalty_bonus_redeem_seven_credit));
                denominationPoints.setText(getString(R.string.loyalty_bonus_redeem_seven_points));

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}

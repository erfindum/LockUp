package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatImageView;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.OnRequestRedeemListener;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.RedeemAlertDialog;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.RedeemErrorDialog;

import java.util.Calendar;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by RAAJA on 30-01-2017.
 */

public class LoyaltyUserRedeemFinalFragment extends Fragment implements OnRequestRedeemListener {

    private AppCompatImageView redeemTypeImage;
    private TextView denomination, denominationPoints, enterIdInfo;
    private EditText enterIdEdit;
    private Button redeemPointsButton;
    private String redeemType, redeemTypeString;
    private ProgressBar redeemFinalProgress;
    private int selection;
    private LoyaltyUserActivity activity;
    private DialogFragment redeemFinalDialog, redeemAlertDialog;
    private int[] credits;
    private String[] points;

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
        redeemFinalProgress = (ProgressBar) parent.findViewById(R.id.loyalty_bonus_redeem_final_progress);

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
        activity.setOnRequestRedeemListener(this);
        setListeners();
    }

    private void setListeners(){
        enterIdEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    redeemPointsButton.setEnabled(false);
                    redeemFinalProgress.setVisibility(View.VISIBLE);
                    validateId();
                }
                return false;
            }
        });
       redeemPointsButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               redeemPointsButton.setEnabled(false);
               redeemFinalProgress.setVisibility(View.VISIBLE);
               validateId();
           }
       });
    }

    private void validateId(){
        Pattern emailMatcher = Patterns.EMAIL_ADDRESS;
        if(enterIdEdit.getText().toString().equals("")){
            displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_ERROR,getString(R.string.loyalty_redeem_final_empty_ID));
            return;
        }
        if(redeemType.equals("paypal") && !emailMatcher.matcher(enterIdEdit.getText()).matches()){
            displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_ERROR,getString(R.string.loyalty_redeem_paypal_id_error));
            return;
        }
        if(redeemFinalDialog!=null){
            redeemFinalDialog.dismiss();
        }
        redeemAlertDialog = new RedeemAlertDialog();
        Bundle arguments = new Bundle();
        String creditEncoded = TextUtils.htmlEncode(String.valueOf(credits[selection]));
        String redeemTypeEncoded = TextUtils.htmlEncode(redeemTypeString);
        String idEncoded = TextUtils.htmlEncode(enterIdEdit.getText().toString());
        String redeemMessage = String.format(getString(R.string.loyalty_redeem_dialog_info_message),creditEncoded
                    ,redeemTypeEncoded,idEncoded);
        arguments.putString(RedeemAlertDialog.REDEEM_ERROR_MESSAGE,redeemMessage);
        redeemAlertDialog.setArguments(arguments);
        redeemAlertDialog.show(activity.getSupportFragmentManager(),"reddem_alert_dialog");
    }

    private void displayRedeemInfoDialog(int type, String mssg){
        if(redeemFinalDialog!=null){
            redeemFinalDialog.dismiss();
        }
        redeemFinalDialog = new RedeemErrorDialog();
        Bundle arguments = new Bundle();
        arguments.putString(RedeemErrorDialog.REDEEM_ERROR_MESSAGE,mssg);
        arguments.putInt(RedeemErrorDialog.REDEEM_DIALOG_TYPE,type);
        redeemFinalDialog.setArguments(arguments);
        redeemFinalDialog.show(activity.getSupportFragmentManager(),"redeem_info_dialog");
        redeemPointsButton.setEnabled(true);
        redeemFinalProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(redeemType.equals("paypal")){
            enterIdInfo.setText(getString(R.string.loyalty_redeem_final_paypal_info));
            enterIdEdit.setHint(getString(R.string.loyalty_redeem_final_paypal_hint));
            redeemTypeImage.setImageResource(R.drawable.ic_paypal_logo);
            credits = getResources().getIntArray(R.array.loyalty_bonus_redeem_paypal_credits);
            points = getResources().getStringArray(R.array.loyalty_bonus_server_paypal_points);
            setDenomination(selection);
            denomination.setText(getCreditText(credits[selection]));
            redeemTypeString = "Paypal";
        }
        if(redeemType.equals("paytm")){
            enterIdInfo.setText(getString(R.string.loyalty_redeem_final_paytm_info));
            enterIdEdit.setHint(getString(R.string.loyalty_redeem_final_paytm_hint));
            enterIdEdit.setMaxLines(1);
            enterIdEdit.setInputType(InputType.TYPE_CLASS_PHONE);
            enterIdEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            redeemTypeImage.setImageResource(R.drawable.ic_paytm_logo);
            credits = getResources().getIntArray(R.array.loyalty_bonus_redeem_paytm_credits);
            points = getResources().getStringArray(R.array.loyalty_bonus_server_paytm_points);
            setDenomination(selection);
            denomination.setText(getCreditText(credits[selection]));
            redeemTypeString="Paytm";
        }
    }

    String getCreditText(int credit){
        return "$"+credit;
    }

    private void setDenomination(int selection){
        switch (selection){
            case 0:
                denominationPoints.setText(points[0]);
                return;
            case 1:
                denominationPoints.setText(points[1]);
                return;
            case 2:
                denominationPoints.setText(points[2]);
                return;
            case 3:
                denominationPoints.setText(points[3]);
                return;
            case 4:
                denominationPoints.setText(points[4]);
                return;
            case 5:
                denominationPoints.setText(points[5]);
        }
    }

    @Override
    public void requestRedeem() {
        SharedPreferences appLockPreference = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        final SharedPreferences loyaltyPreference = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME
                                                ,Context.MODE_PRIVATE);
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo==null || !networkInfo.isConnected()){
            displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_ERROR
                ,getString(R.string.loyalty_bonus_signup_no_connection));
            return;
        }
        Calendar calendar = Calendar.getInstance();
        final String userEmail = appLockPreference.getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"noEmail");
        String dateString = calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"+calendar.get(Calendar.DAY_OF_MONTH);
        LoyaltyBonusRequest redeemRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
        Call<LoyaltyUserRedeemResponse> redeemCall = redeemRequest.requestBonusRedeem("PointReddem",getRedeemPoints(selection)
        ,userEmail
        ,loyaltyPreference.getString(LoyaltyBonusModel.LOYALTY_SEND_REQUEST,"noCode"),redeemTypeString
        ,String.valueOf(credits[selection]),dateString,enterIdEdit.getText().toString());
        redeemCall.enqueue(new Callback<LoyaltyUserRedeemResponse>() {
            @Override
            public void onResponse(Call<LoyaltyUserRedeemResponse> call, Response<LoyaltyUserRedeemResponse> response) {
                if(response.isSuccessful()){
                    LoyaltyUserRedeemResponse redeemResponse = response.body();
                    if(redeemResponse.code.equals("200")){
                        loyaltyPreference.edit().putString(LoyaltyBonusModel.USER_LOYALTY_BONUS,redeemResponse.BalacePoint)
                                .apply();
                        if(activity==null){
                            return;
                        }
                        String successString = String.format(getString(R.string.loyalty_redeem_success_message),
                                String.valueOf(credits[selection]));
                        displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_SUCCESS
                                ,successString);
                        redeemFinalProgress.setVisibility(View.INVISIBLE);
                    }
                    if(redeemResponse.code.equals("100")){
                        if(activity==null){
                            return;
                        }
                        displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_FAILED
                                ,redeemResponse.msg);
                    }
                }else{
                    if(activity==null){
                        return;
                    }
                    displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_ERROR
                            ,getString(R.string.loyalty_bonus_signup_unknown_error));
                }
            }

            @Override
            public void onFailure(Call<LoyaltyUserRedeemResponse> call, Throwable t) {
                if(activity==null){
                    return;
                }
                displayRedeemInfoDialog(RedeemErrorDialog.REDEEM_TYPE_ERROR
                        ,getString(R.string.loyalty_bonus_signup_unknown_error));
            }
        });

    }

    private String getRedeemPoints(int selection){
        switch (selection){
            case 0:
                return points[0];
            case 1:
                return points[1];
            case 2:
                return points[2];
            case 3:
                return points[3];
            case 4:
                return points[4];
            case 5:
                return points[5];
        }
        return null;
    }

    @Override
    public void requestCancelled() {
        redeemPointsButton.setEnabled(true);
        redeemFinalProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity.setOnRequestRedeemListener(null);
        activity = null;
    }
}

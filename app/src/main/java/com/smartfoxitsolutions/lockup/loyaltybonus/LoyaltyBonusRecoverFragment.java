package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.dialogs.NetworkProcessDialog;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.OperationSuccessDialog;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by RAAJA on 19-01-2017.
 */

public class LoyaltyBonusRecoverFragment extends Fragment {

    private AppCompatEditText sendRecoveryEdit, resetPasswordEdit, passwordEdit, confirmPasswordEdit;
    private TextView sendRecoveryButton, resetPasswordButton;
    private Toolbar toolbar;
    private LoyaltyBonusMain activity;
    private DialogFragment networkProcessDialog,operationSuccessDialog;
    private ConnectivityManager connectivityManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_password_recovery,container,false);
        sendRecoveryButton = (TextView) parent.findViewById(R.id.loyalty_bonus_password_recovery_send_button);
        resetPasswordButton = (TextView) parent.findViewById(R.id.loyalty_bonus_password_recovery_reset_button);

        sendRecoveryEdit = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_password_recovery_send_email);
        resetPasswordEdit = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_password_recovery_reset_edit);
        passwordEdit = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_password_recovery_one);
        confirmPasswordEdit = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_password_recovery_two);
        toolbar = (Toolbar) parent.findViewById(R.id.loyalty_bonus_password_recovery_tool_bar);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyBonusMain) getActivity();
        connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        activity.setSupportActionBar(toolbar);
        ActionBar actionToolbar = activity.getSupportActionBar();
        if(actionToolbar!=null){
            actionToolbar.setDisplayHomeAsUpEnabled(true);
            actionToolbar.setTitle(getString(R.string.loyalty_password_recovery_toolbar_title));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onBackPressed();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String emailString = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE)
                .getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"No Registered Email");
        sendRecoveryEdit.setText(emailString);
        sendRecoveryEdit.setEnabled(false);
        long intervalStart = prefs.getLong(LoyaltyBonusModel.RECOVERY_CODE_TIME_INTERVAL,0);
        long intervalEnd = intervalStart+(30*1000);

        if(System.currentTimeMillis()<intervalEnd){
            resetPasswordEdit.setEnabled(true);
            passwordEdit.setEnabled(true);
            confirmPasswordEdit.setEnabled(true);
        }else{
            resetPasswordEdit.setEnabled(false);
            passwordEdit.setEnabled(false);
            confirmPasswordEdit.setEnabled(false);
        }

        confirmPasswordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    validatePassword();
                    return false;
                }
                return false;
            }
        });

        sendRecoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRecoveryCode();
            }
        });

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validatePassword();
            }
        });
    }

    void sendRecoveryCode(){
        final SharedPreferences prefs = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        long intervalStart = prefs.getLong(LoyaltyBonusModel.RECOVERY_CODE_TIME_INTERVAL,0);
        long intervalEnd = intervalStart+(30*1000);
        if(intervalStart!=0 && System.currentTimeMillis()<intervalEnd){
            String pauseRequest = getString(R.string.reset_pin_pattern_pause_request);
            long interval = intervalEnd - System.currentTimeMillis();
            int minutes = (((int) interval/ (1000*60)) % 60);
            int seconds = ((int) interval/ 1000) % 60;
            Toast.makeText(activity,pauseRequest+" "+minutes+":"+seconds + " minutes",Toast.LENGTH_LONG)
                    .show();
            return;
        }

        Pattern emailMatcher = Patterns.EMAIL_ADDRESS;
        if(!emailMatcher.matcher(sendRecoveryEdit.getText()).matches()){
            postEmailError();
            return;
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if(networkInfo == null){
            if(networkProcessDialog !=null){
                networkProcessDialog.dismiss();
            }
            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                    ,getString(R.string.reset_pin_pattern_network_failed)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative)
                    ,"networkConnectionFailed");
            return;
        }

        if(networkInfo.isConnected()) {
            if (networkProcessDialog != null) {
                networkProcessDialog.dismiss();
            }
            networkProcessDialog = new NetworkProcessDialog();
            Bundle bundle = new Bundle();
            bundle.putInt(NetworkProcessDialog.NETWORK_DIALOG_TYPE, NetworkProcessDialog.NETWORK_DIALOG_TYPE_LOADING);
            bundle.putString(NetworkProcessDialog.NETWORK_INFO_HEADER
                    , getString(R.string.reset_pin_pattern_network_loading_header));
            bundle.putString(NetworkProcessDialog.NETWORK_INFO_MESSAGE
                    , getString(R.string.reset_pin_pattern_network_loading_message));
            networkProcessDialog.setArguments(bundle);
            final FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack("resetLoading");
            networkProcessDialog.show(fragmentTransaction, "resetLoading");

            final LoyaltyBonusRequest resetPasswordRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
            Call<LoyaltyBonusRecoveryResponse> retroCall = resetPasswordRequest.requestRecoverCode("forgetPassword",
                    sendRecoveryEdit.getText().toString());

            retroCall.enqueue(new Callback<LoyaltyBonusRecoveryResponse>() {
                @Override
                public void onResponse(Call<LoyaltyBonusRecoveryResponse> call, Response<LoyaltyBonusRecoveryResponse> response) {
                    if(response.isSuccessful()) {
                        LoyaltyBonusRecoveryResponse resetResponse = response.body();
                        if(resetResponse!=null && resetResponse.code.equals("200")){
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putLong(LoyaltyBonusModel.RECOVERY_CODE_TIME_INTERVAL,System.currentTimeMillis());
                            edit.apply();
                            if(networkProcessDialog!=null){
                                networkProcessDialog.dismiss();
                            }
                            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_success_header)
                                    ,getString(R.string.reset_pin_pattern_network_success_message)
                                    ,getString(R.string.reset_pin_pattern_network_success_negative)
                                    ,"resetSuccessResponse");
                            activity.isRecoverySent =true;
                            activity.shouldTrackUserPresence=false;
                            resetPasswordEdit.setEnabled(true);
                            passwordEdit.setEnabled(true);
                            confirmPasswordEdit.setEnabled(true);
                        }
                        if(resetResponse!=null && resetResponse.code.equals("100")){
                            if(networkProcessDialog!=null){
                                networkProcessDialog.dismiss();
                            }
                            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                                    ,resetResponse.message
                                    ,getString(R.string.reset_pin_pattern_network_failed_negative)
                                    ,"resetResponseFailure");
                        }
                    }else {
                        if (networkProcessDialog != null) {
                            networkProcessDialog.dismiss();
                        }
                        displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                                , getString(R.string.reset_pin_pattern_response_failed)
                                , getString(R.string.reset_pin_pattern_network_failed_negative)
                                , "resetResponseFailure");
                    }
                }

                @Override
                public void onFailure(Call<LoyaltyBonusRecoveryResponse> call, Throwable t) {
                    if(networkProcessDialog!=null){
                        networkProcessDialog.dismiss();
                    }
                    displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                            ,getString(R.string.reset_pin_pattern_response_failed)
                            ,getString(R.string.reset_pin_pattern_network_failed_negative)
                            ,"resetResponseFailure");
                }
            });

        }else{
            if(networkProcessDialog !=null){
                networkProcessDialog.dismiss();
            }
            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                    ,getString(R.string.reset_pin_pattern_network_failed)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative)
                    ,"networkConnectionFailed");
        }

    }

    void validatePassword(){
        if(passwordEdit.getText().toString().isEmpty() || confirmPasswordEdit.getText().toString().isEmpty()){
            displayCompleteDialog(getString(R.string.loyalty_password_recovery_reset_error),
                    getString(R.string.loyalty_bonus_signup_password_empty_error)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative),
                    "resetPasswordError");
        }else if(passwordEdit.getText().toString().length()<8 || confirmPasswordEdit.getText().toString().length()<8){
            displayCompleteDialog(getString(R.string.loyalty_password_recovery_reset_error),
                    getString(R.string.loyalty_password_recovery_reset_password_length_error)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative),
                    "resetPasswordError");
        }else if(resetPasswordEdit.getText().toString().isEmpty()){
            displayCompleteDialog(getString(R.string.loyalty_password_recovery_reset_error),
                    getString(R.string.loyalty_password_recovery_code_error)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative),
                    "resetPasswordError");
        }else if(!passwordEdit.getText().toString().equals(confirmPasswordEdit.getText().toString())){
            displayCompleteDialog(getString(R.string.loyalty_password_recovery_reset_error),
                    getString(R.string.loyalty_bonus_signup_password_error)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative),
                    "resetPasswordError");
        }else{
            resetNewPassword(resetPasswordEdit.getText().toString());
        }

    }

    void resetNewPassword(final String pin){
        final SharedPreferences prefs = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,Context.MODE_PRIVATE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo == null){
            if(networkProcessDialog !=null){
                networkProcessDialog.dismiss();
            }
            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                    ,getString(R.string.reset_pin_pattern_network_failed)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative)
                    ,"networkConnectionFailed");
            return;
        }

        if(networkInfo!=null && networkInfo.isConnected()) {
            if (networkProcessDialog != null) {
                networkProcessDialog.dismiss();
            }
            networkProcessDialog = new NetworkProcessDialog();
            Bundle bundle = new Bundle();
            bundle.putInt(NetworkProcessDialog.NETWORK_DIALOG_TYPE, NetworkProcessDialog.NETWORK_DIALOG_TYPE_LOADING);
            bundle.putString(NetworkProcessDialog.NETWORK_INFO_HEADER
                    , getString(R.string.reset_pin_pattern_network_loading_header));
            bundle.putString(NetworkProcessDialog.NETWORK_INFO_MESSAGE
                    , getString(R.string.loyalty_password_recovery_reset_info));
            networkProcessDialog.setArguments(bundle);
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack("resetLoading");
            networkProcessDialog.show(fragmentTransaction, "resetLoading");

            final LoyaltyBonusRequest resetPasswordRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
            String email = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE)
                    .getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"No Email Registered");
            Call<LoyaltyBonusResetResponse> resetCall = resetPasswordRequest.requestResetPassword("ChangePassword",
                    email,
                    passwordEdit.getText().toString(),pin);

            resetCall.enqueue(new Callback<LoyaltyBonusResetResponse>() {
                @Override
                public void onResponse(Call<LoyaltyBonusResetResponse> call, Response<LoyaltyBonusResetResponse> response) {
                    if(response.isSuccessful()) {
                        LoyaltyBonusResetResponse resetResponse = response.body();
                        if(resetResponse!=null && resetResponse.code.equals("200")){
                            if(networkProcessDialog!=null){
                                networkProcessDialog.dismiss();
                            }
                           displaySuccessDialog(getString(R.string.loyalty_password_recovery_reset_message),
                                   getString(R.string.loyalty_password_recovery_reset_positive),"resetSuccess");
                            activity.isRecoverySent =false;
                            activity.shouldTrackUserPresence = true;
                            resetPasswordEdit.setEnabled(false);
                            passwordEdit.setEnabled(false);
                            confirmPasswordEdit.setEnabled(false);
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putLong(LoyaltyBonusModel.RECOVERY_CODE_TIME_INTERVAL,0);
                            edit.apply();
                        }
                        if(resetResponse!=null && resetResponse.code.equals("100")){
                            if(networkProcessDialog!=null){
                                networkProcessDialog.dismiss();
                            }
                            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                                    ,resetResponse.message
                                    ,getString(R.string.reset_pin_pattern_network_failed_negative)
                                    ,"resetResponseFailure");
                        }
                    }else {
                        if (networkProcessDialog != null) {
                            networkProcessDialog.dismiss();
                        }
                        displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                                , getString(R.string.reset_pin_pattern_response_failed)
                                , getString(R.string.reset_pin_pattern_network_failed_negative)
                                , "resetResponseFailure");
                    }
                }

                @Override
                public void onFailure(Call<LoyaltyBonusResetResponse> call, Throwable t) {
                    if(networkProcessDialog!=null){
                        networkProcessDialog.dismiss();
                    }
                    displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                            ,getString(R.string.reset_pin_pattern_response_failed)
                            ,getString(R.string.reset_pin_pattern_network_failed_negative)
                            ,"resetResponseFailure");
                }
            });

        }else{
            if(networkProcessDialog !=null){
                networkProcessDialog.dismiss();
            }
            displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                    ,getString(R.string.reset_pin_pattern_network_failed)
                    ,getString(R.string.reset_pin_pattern_network_failed_negative)
                    ,"networkConnectionFailed");
        }
    }

    private void displayCompleteDialog(String header, String message, String negative, String tag){
        networkProcessDialog = new NetworkProcessDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(NetworkProcessDialog.NETWORK_DIALOG_TYPE,NetworkProcessDialog.NETWORK_DIALOG_TYPE_COMPLETE);
        bundle.putString(NetworkProcessDialog.NETWORK_INFO_HEADER
                ,header);
        bundle.putString(NetworkProcessDialog.NETWORK_INFO_MESSAGE
                ,message);
        bundle.putString(NetworkProcessDialog.NETWORK_INFO_BUTTON
                ,negative);
        networkProcessDialog.setArguments(bundle);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(tag);
        networkProcessDialog.show(fragmentTransaction,tag);
    }

    private void displaySuccessDialog(String message, String negative, String tag){
        if(networkProcessDialog !=null){
            networkProcessDialog.dismiss();
        }
        operationSuccessDialog = new OperationSuccessDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(OperationSuccessDialog.OPERATION_TYPE_KEY,OperationSuccessDialog.OPERATION_TYPE_RESET_SUCCESS);
        bundle.putString(OperationSuccessDialog.NETWORK_INFO_MESSAGE
                ,message);
        bundle.putString(OperationSuccessDialog.NETWORK_INFO_BUTTON
                ,negative);
        operationSuccessDialog.setArguments(bundle);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(tag);
        operationSuccessDialog.show(fragmentTransaction,tag);
    }

    private void postEmailError(){
        if(networkProcessDialog !=null){
            networkProcessDialog.dismiss();
        }
        displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                ,getString(R.string.loyalty_bonus_signup_email_error)
                ,getString(R.string.reset_pin_pattern_network_failed_negative)
                ,"networkConnectionFailed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}

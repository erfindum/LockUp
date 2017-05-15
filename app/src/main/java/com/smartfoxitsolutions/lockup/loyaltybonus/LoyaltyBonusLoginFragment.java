package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by RAAJA on 13-01-2017.
 */

public class LoyaltyBonusLoginFragment extends Fragment {

    private AppCompatEditText emailEdit,passwordEdit;
    private ProgressBar progress;
    private TextView loginInfo;
    private LoyaltyBonusMain activity;
    private Button loginButton, resetPasswordButton;
    private boolean shouldValidateLogin;
    private ConnectivityManager connectivityManager;
    private Toolbar toolbar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_login,container,false);
        emailEdit = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_login_email_edit);
        passwordEdit = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_login_password_edit);
        progress = (ProgressBar) parent.findViewById(R.id.loyalty_bonus_login_progress_bar);
        loginInfo = (TextView) parent.findViewById(R.id.loyalty_bonus_login_info);
        loginButton = (Button) parent.findViewById(R.id.loyalty_bonus_login_button);
        toolbar = (Toolbar) parent.findViewById(R.id.loyalty_bonus_login_tool_bar);
        resetPasswordButton = (Button) parent.findViewById(R.id.loyalty_bonus_login_forgot_password_button);
        if(savedInstanceState!=null){
            shouldValidateLogin = savedInstanceState.getBoolean("shouldValidateLogin");
        }else{
            shouldValidateLogin= true;
        }
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
            actionToolbar.setTitle(getString(R.string.loyalty_bonus_signin_button));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onBackPressed();
                }
            });
        }
        setListeners();
    }

    void setListeners(){

        passwordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    validateLogin();
                    return false;
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateLogin();
            }
        });

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startForgotPassword();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        int[][] editState = new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_enabled},
                new int[]{-android.R.attr.state_enabled}
        };

        int[] editColors = new int[]{
                Color.parseColor("#f7e830"),
                Color.parseColor("#f7e830"),
                Color.WHITE,
                Color.WHITE
        };
        ColorStateList colorStateList = new ColorStateList(editState,editColors);
        emailEdit.setSupportBackgroundTintList(colorStateList);
        passwordEdit.setSupportBackgroundTintList(colorStateList);

        emailEdit.setText(activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE)
                .getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"No Email Registered"));
    }

    void validateLogin(){
        progress.setVisibility(View.VISIBLE);
        loginInfo.setVisibility(View.INVISIBLE);
        passwordEdit.setEnabled(false);
        shouldValidateLogin = false;
        Pattern emailMatcher = Patterns.EMAIL_ADDRESS;
        if(emailEdit.getText().toString().isEmpty()){
            postError(getString(R.string.loyalty_bonus_signin_email_empty_error));
        }else if(!emailMatcher.matcher(emailEdit.getText()).matches()){
            postError(getString(R.string.loyalty_bonus_signin_email_error));
        }else if(passwordEdit.getText().toString().isEmpty()){
            postError(getString(R.string.loyalty_bonus_signin_password_empty_error));
        }else {
            loginUser();
        }
    }

    void postError(String message){
        if(activity!=null) {
            progress.setVisibility(View.INVISIBLE);
            loginInfo.setVisibility(View.VISIBLE);
            shouldValidateLogin = true;
            loginInfo.setText(message);
            passwordEdit.setEnabled(true);
        }
    }

    void loginUser(){
        final SharedPreferences preferences = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,Context.MODE_PRIVATE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo == null){
            postError(getString(R.string.loyalty_bonus_signup_no_connection));
            return;
        }

        if(networkInfo.isConnected()){

            LoyaltyBonusRequest loginRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
            Call<LoyaltyBonusLoginResponse> loginCall = loginRequest.requestLogin("Login",passwordEdit.getText().toString()
                                                ,emailEdit.getText().toString());
            loginCall.enqueue(new Callback<LoyaltyBonusLoginResponse>() {
                @Override
                public void onResponse(Call<LoyaltyBonusLoginResponse> call, Response<LoyaltyBonusLoginResponse> response) {
                    if(response.isSuccessful()) {
                        LoyaltyBonusLoginResponse loginResponse = response.body();
                        if (loginResponse != null && loginResponse.code.equals("200")) {
                            if(activity!=null) {
                                LoyaltyBonusLoginData data = loginResponse.data.get(0);
                                SharedPreferences.Editor edit = preferences.edit();
                                edit.putString(LoyaltyBonusModel.LOGIN_EMAIL_KEY, data.emailId);
                                edit.putString(LoyaltyBonusModel.LOGIN_USER_NAME_KEY, data.fullname);
                                edit.putBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY, true);
                                edit.putString(LoyaltyBonusModel.LOYALTY_SEND_REQUEST, data.auth_code);
                                edit.putString(LoyaltyBonusModel.USER_LOYALTY_BONUS,data.TotalPoint);
                                edit.apply();
                                activity.startLoyaltyUserMain();
                                Log.d("LoyaltyBonus", "User name " + data.fullname);
                                Log.d("LoyaltyBonus", "E-mail " + data.emailId);
                            }
                        }
                        else {
                            postError("Invalid Credentials");
                        }
                    }else{
                        postError(getString(R.string.loyalty_bonus_signup_unknown_error));
                    }
                }

                @Override
                public void onFailure(Call<LoyaltyBonusLoginResponse> call, Throwable t) {
                    postError(getString(R.string.loyalty_bonus_signup_unknown_error));
                }
            });

        }else{
            postError(getString(R.string.loyalty_bonus_signup_no_connection));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("shouldValidateLogin",shouldValidateLogin);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}

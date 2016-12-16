package com.smartfoxitsolutions.lockup;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.smartfoxitsolutions.lockup.dialogs.NetworkProcessDialog;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Interceptor;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by RAAJA on 14-12-2016.
 */

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String RESET_PASSWORD_TIME_INTERVAL_KEY = "resetPasswordTimeInterval";
    private static final String RESET_PASSWORD_PIN_KEY ="resetPasswordPin";

    private Toolbar toolbar;
    private AppCompatEditText securityEdit;
    private TextView emailText, sendMailButton, resetButton;
    private ConnectivityManager connectivityManager;
    private NetworkProcessDialog networkProcessDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_pin_pattern_activity);
        toolbar = (Toolbar) findViewById(R.id.reset_pin_pattern_activity_tool_bar);
        emailText = (TextView) findViewById(R.id.reset_pin_pattern_send_email);
        sendMailButton = (TextView) findViewById(R.id.reset_pin_pattern_send_button);
        resetButton = (TextView) findViewById(R.id.reset_pin_pattern_reset_button);
        securityEdit = (AppCompatEditText) findViewById(R.id.reset_pin_pattern_reset_edit);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.reset_pin_pattern_toolbar_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final SharedPreferences prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        emailText.setText(prefs.getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,"No email registered"));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        long intervalStart = prefs.getLong(RESET_PASSWORD_TIME_INTERVAL_KEY,0);
        long intervalEnd = intervalStart+(2*60*1000);
        if(intervalStart!=0 && System.currentTimeMillis()<intervalEnd){
           securityEdit.setEnabled(true);
        }else{
            securityEdit.setEnabled(false);
        }
        securityEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    Log.d("ResetEmail","Request Code " + securityEdit.getText().toString());
                    resetPassword(securityEdit.getText().toString());
                    return false;
                }
                return false;
            }
        });
        securityEdit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                return false;
            }
        });
        sendMailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               sendResetCode();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ResetEmail","Request Code " + securityEdit.getText().toString());
                resetPassword(securityEdit.getText().toString());
            }
        });
    }

    private void sendResetCode(){
        final SharedPreferences prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        long intervalStart = prefs.getLong(RESET_PASSWORD_TIME_INTERVAL_KEY,0);
        long intervalEnd = intervalStart+(60*300);
        if(intervalStart!=0 && System.currentTimeMillis()<intervalEnd){
            String pauseRequest = getString(R.string.reset_pin_pattern_pause_request);
            long interval = intervalEnd - System.currentTimeMillis();
            int minutes = (((int) interval/ (1000*60)) % 60);
            int seconds = ((int) interval/ 1000) % 60 ;
            Toast.makeText(getBaseContext(),pauseRequest+" "+minutes+":"+seconds + " minutes",Toast.LENGTH_LONG)
                    .show();
            return;
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo!=null && networkInfo.isConnected()) {
            if(networkProcessDialog !=null){
                networkProcessDialog.dismiss();
            }
            networkProcessDialog = new NetworkProcessDialog();
            Bundle bundle = new Bundle();
            bundle.putInt(NetworkProcessDialog.NETWORK_DIALOG_TYPE,NetworkProcessDialog.NETWORK_DIALOG_TYPE_LOADING);
            bundle.putString(NetworkProcessDialog.NETWORK_INFO_HEADER
                    ,getString(R.string.reset_pin_pattern_network_loading_header));
            bundle.putString(NetworkProcessDialog.NETWORK_INFO_MESSAGE
                    ,getString(R.string.reset_pin_pattern_network_loading_message));
            networkProcessDialog.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack("resetLoading");
            networkProcessDialog.show(fragmentTransaction,"resetLoading");

            final ResetPasswordRequest resetPasswordRequest = ServiceGenerator.createService(ResetPasswordRequest.class);
            Call<ResetPasswordResponse> retroCall = resetPasswordRequest.requestRecovery("forgetPassword",
                    prefs.getString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY, "No email registered")
            );

            retroCall.enqueue(new Callback<ResetPasswordResponse>() {
                @Override
                public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {
                    if(response.isSuccessful()) {
                        ResetPasswordResponse resetResponse = response.body();
                        if(resetResponse!=null){
                            if(resetResponse.code!=null){
                                Log.d("EmailResponse",resetResponse.code);
                            }
                            if(resetResponse.status!=null){
                                Log.d("EmailResponse",resetResponse.status);
                            }
                            if(resetResponse.pin!=null){
                                Log.d("EmailResponse",resetResponse.pin);
                            }
                            if(resetResponse.email!=null){
                                Log.d("EmailResponse",resetResponse.email);
                            }
                        }
                        if(resetResponse!=null && resetResponse.code.equals("200")){
                            long salt = System.currentTimeMillis();
                            byte[] pinByte = (resetResponse.pin+String.valueOf(salt)).getBytes();
                            try {
                                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                                byte[] messageDigest = digest.digest(pinByte);
                                StringBuilder sb1 = new StringBuilder();
                                for (int i = 0; i < messageDigest.length; ++i) {
                                    sb1.append(Integer.toHexString((messageDigest[i] & 0xFF) | 0x100).substring(1,3));
                                }
                                SharedPreferences.Editor edit = prefs.edit();
                                edit.putLong(RESET_PASSWORD_TIME_INTERVAL_KEY,salt);
                                edit.putString(RESET_PASSWORD_PIN_KEY,sb1.toString());
                                edit.apply();
                                Log.d("ResetEmail","Before hash " + sb1.toString() + " salt " + salt);
                                if(networkProcessDialog!=null){
                                    networkProcessDialog.dismiss();
                                }
                                displayCompleteDialog(getString(R.string.reset_pin_pattern_network_success_header)
                                        ,getString(R.string.reset_pin_pattern_network_success_message)
                                        ,getString(R.string.reset_pin_pattern_network_success_negative)
                                        ,"resetSuccessResponse");
                                securityEdit.setEnabled(true);

                            }catch (NoSuchAlgorithmException e){
                                e.printStackTrace();
                            }
                            return;
                        }
                    }
                    if(networkProcessDialog!=null){
                        networkProcessDialog.dismiss();
                    }
                    displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                            ,getString(R.string.reset_pin_pattern_response_failed)
                            ,getString(R.string.reset_pin_pattern_network_failed_negative)
                            ,"resetResponseFailure");
                }

                @Override
                public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
                    if(networkProcessDialog!=null){
                        networkProcessDialog.dismiss();
                    }
                    displayCompleteDialog(getString(R.string.reset_pin_pattern_network_failed_header)
                            ,getString(R.string.reset_pin_pattern_response_failed)
                            ,getString(R.string.reset_pin_pattern_network_failed_negative)
                            ,"resetResponseFailure");
                    Log.d("Email-Error", t.getMessage());
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(tag);
        networkProcessDialog.show(fragmentTransaction,tag);
        securityEdit.setEnabled(false);
    }

    private void resetPassword(String pin){
        SharedPreferences prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        String salt = String.valueOf(prefs.getLong(RESET_PASSWORD_TIME_INTERVAL_KEY,0));
        String originalRequestCode = prefs.getString(RESET_PASSWORD_PIN_KEY,"noCode");
        Log.d("ResetEmail",originalRequestCode + " salt " + salt);
        byte[] pinByte = (pin+salt).getBytes();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = digest.digest(pinByte);
            StringBuilder sb1 = new StringBuilder();
            for (int i = 0; i < messageDigest.length; ++i) {
                sb1.append(Integer.toHexString((messageDigest[i] & 0xFF) | 0x100).substring(1,3));
            }
            Log.d("ResetEmail",sb1.toString() + " hashed code");
            if(originalRequestCode.equals(sb1.toString())){
               SharedPreferences.Editor edit = prefs.edit();
               edit.putLong(RESET_PASSWORD_TIME_INTERVAL_KEY,0);
               edit.putString(RESET_PASSWORD_PIN_KEY,"noCode");
               edit.apply();
               startActivity(new Intent(this,SetPinPatternActivity.class)
                            .putExtra(SetPinPatternActivity.INTENT_PIN_PATTERN_START_TYPE_KEY,
                                        SetPinPatternActivity.INTENT_RESET_PASSWORD));
               securityEdit.setEnabled(false);
                securityEdit.setText("");
           }else{
               Toast.makeText(getBaseContext(),getString(R.string.reset_pin_pattern_reset_code_invalid),Toast.LENGTH_LONG)
                       .show();
           }
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
    }

}

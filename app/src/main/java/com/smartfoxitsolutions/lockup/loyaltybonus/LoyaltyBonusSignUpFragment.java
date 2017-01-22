package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.LoyaltyBonusDatePicker;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.OnUserSignUpListener;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.OperationSuccessDialog;
import com.smartfoxitsolutions.lockup.loyaltybonus.dialogs.SignUpErrorDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by RAAJA on 16-01-2017.
 */

public class LoyaltyBonusSignUpFragment extends Fragment implements AdapterView.OnItemSelectedListener
,OnUserSignUpListener {

    private AppCompatSpinner genderSpinner, countrySpinner;
    private AppCompatEditText firstName,lastName,email,password,confirmPassword;
    private Button signInButton,signUpButton;
    private LoyaltyBonusMain activity;
    private TextView datePicker, signUpInfo;
    private ProgressBar signUpProgress;
    private int day = 0, month = 0,year = 0;
    private String genderText, countryText;
    private boolean shouldValidateUserData;
    private ConnectivityManager connectivityManager;
    private DialogFragment errorFragment, operationSuccessDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_signup,container,false);
        genderSpinner = (AppCompatSpinner) parent.findViewById(R.id.loyalty_bonus_signup_gender);
        countrySpinner = (AppCompatSpinner) parent.findViewById(R.id.loyalty_bonus_signup_country);
        firstName = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_signup_first_edit);
        lastName = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_signup_last_edit);
        email = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_signup_email_edit);
        password = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_signup_password_edit);
        confirmPassword = (AppCompatEditText) parent.findViewById(R.id.loyalty_bonus_signup_confirm_password_edit);
        signInButton = (Button) parent.findViewById(R.id.loyalty_bonus_signup_login_button);
        signUpButton = (Button) parent.findViewById(R.id.loyalty_bonus_signup_button);
        datePicker = (TextView) parent.findViewById(R.id.loyalty_bonus_signup_dob);
        signUpInfo = (TextView) parent.findViewById(R.id.loyalty_bonus_signup_info);
        signUpProgress = (ProgressBar) parent.findViewById(R.id.loyalty_bonus_signup_progress_bar);
        if(savedInstanceState!=null){
            shouldValidateUserData = savedInstanceState.getBoolean("shouldSignup");
        }else {
            shouldValidateUserData = true;
        }
        return parent;
    }

    void setupSpinners(){
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(getActivity(),R.array.gender_list
                                                            ,R.layout.loyalty_spinner);
        genderAdapter.setDropDownViewResource(R.layout.loyalty_spinner_dropdown);
        genderSpinner.setAdapter(genderAdapter);
        Locale[] localeList = Locale.getAvailableLocales();
        ArrayList<String> countryList = new ArrayList<>();
        for(Locale locale:localeList){
            String country = locale.getDisplayCountry();
            if(country.length()>0 && !countryList.contains(country)){
                countryList.add(country);
            }
        }
        Collections.sort(countryList,String.CASE_INSENSITIVE_ORDER);
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(getActivity(),R.layout.loyalty_spinner,countryList);
        countryAdapter.setDropDownViewResource(R.layout.loyalty_spinner_dropdown);
        countrySpinner.setAdapter(countryAdapter);
        countrySpinner.setOnItemSelectedListener(this);
        genderSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyBonusMain) getActivity();
        activity.setOnUserSignUpListener(this);
        connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        setupSpinners();

    }

    @Override
    public void onStart() {
        super.onStart();
        int[][] editState = new int[][]{
             new int[]{android.R.attr.state_focused},
             new int[]{android.R.attr.state_pressed},
             new int[]{android.R.attr.state_enabled}
        };

        int[] editColors = new int[]{
                Color.parseColor("#f7e830"),
                Color.parseColor("#f7e830"),
                Color.WHITE
        };
        ColorStateList colorStateList = new ColorStateList(editState,editColors);
        genderSpinner.setSupportBackgroundTintList(colorStateList);
        countrySpinner.setSupportBackgroundTintList(colorStateList);
        firstName.setSupportBackgroundTintList(colorStateList);
        lastName.setSupportBackgroundTintList(colorStateList);
        email.setSupportBackgroundTintList(colorStateList);
        password.setSupportBackgroundTintList(colorStateList);
        confirmPassword.setSupportBackgroundTintList(colorStateList);

        setListeners();

    }

    void setListeners(){
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignIn();
            }
        });

        datePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDatePickerDialog();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shouldValidateUserData){
                    valtidateUserData();
                }
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        AppCompatSpinner spinner = (AppCompatSpinner) parent;
            if(spinner.getId() == R.id.loyalty_bonus_signup_gender) {
                genderText = (String) spinner.getItemAtPosition(position);
                Log.d("LoyaltySignUp",genderText);
            }
            if(spinner.getId() == R.id.loyalty_bonus_signup_country){
                countryText = (String) spinner.getItemAtPosition(position);
                Log.d("LoyaltySignUp",countryText);
            }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    void startSignIn(){
        activity.startSignIn();
    }

    void startDatePickerDialog(){
        if(activity!=null) {
            DialogFragment dateFragment = new LoyaltyBonusDatePicker();
            if(day>0 && month >0 && year >0){
                Bundle argBundle = new Bundle();
                argBundle.putInt("date",day);
                argBundle.putInt("month",month-1);
                argBundle.putInt("year",year);
                dateFragment.setArguments(argBundle);
            }
            dateFragment.show(activity.getSupportFragmentManager(),"user_date_picker");
        }
    }

    @Override
    public void onUserDatePicked(int year, int monthOfYear, int dayOfMonth) {
        this.year = year;
        this.month = monthOfYear;
        this.day = dayOfMonth;
        datePicker.setText(dayOfMonth+"/"+monthOfYear+"/"+year);
    }

    @Override
    public void onErrorDialogCancel() {
        shouldValidateUserData = true;
        signUpInfo.setVisibility(View.INVISIBLE);
        signUpProgress.setVisibility(View.INVISIBLE);
        signUpButton.setText(getString(R.string.loyalty_bonus_signup_button));
    }

    void valtidateUserData(){
        shouldValidateUserData = false;
        signUpInfo.setVisibility(View.INVISIBLE);
        signUpProgress.setVisibility(View.VISIBLE);
        signUpButton.setText(getString(R.string.loyalty_bonus_signup_wait_text));

        Pattern emailPattern = Patterns.EMAIL_ADDRESS;

        if(firstName.getText().toString().isEmpty()){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_first_name_error));
        }
        else
        if(lastName.getText().toString().isEmpty()){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_last_name_error));
        }else if(email.getText().toString().isEmpty()){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_email_empty_error));
        }else if(!emailPattern.matcher(email.getText()).matches()){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_email_error));
        }else if(password.getText().toString().isEmpty() || confirmPassword.getText().toString().isEmpty()){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_password_empty_error));
        }else if(password.getText().toString().length()<8 || confirmPassword.getText().toString().length()<8){
            displayErrorDialog(getString(R.string.loyalty_password_recovery_reset_password_length_error));
        }else if(!password.getText().toString().equals(confirmPassword.getText().toString())){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_password_error));
        }else if(day<=0 || month<=0 || year<=0){
            displayErrorDialog(getString(R.string.loyalty_bonus_signup_date_error));
        }else{
            completeSignUp();
        }
    }

    void displayErrorDialog(String errorMessage){
        if(errorFragment !=null){
            errorFragment.dismiss();
        }
        errorFragment = new SignUpErrorDialog();
        Bundle argBundle = new Bundle();
        argBundle.putString(SignUpErrorDialog.SIGNUP_ERROR_MESSAGE,errorMessage);
        errorFragment.setArguments(argBundle);
        errorFragment.show(activity.getSupportFragmentManager(),"sign_up_error");
    }

    void completeSignUp(){
        final String emailText = email.getText().toString();
        NetworkInfo connectInfo = connectivityManager.getActiveNetworkInfo();
        if(connectInfo!=null && connectInfo.isConnected()){

            LoyaltyBonusRequest signupRequest = LoyaltyServiceGenerator.createService(LoyaltyBonusRequest.class);
            Call<LoyaltyBonusSignUpResponse> signUpCall = signupRequest.requestSignUp("register",
                    firstName.getText().toString()+" "+lastName.getText().toString(),password.getText().toString()
            ,getFormattedDate(year,month,day),countryText,genderText,email.getText().toString());

            signUpCall.enqueue(new Callback<LoyaltyBonusSignUpResponse>() {
                @Override
                public void onResponse(Call<LoyaltyBonusSignUpResponse> call, Response<LoyaltyBonusSignUpResponse> response) {
                    if(response.isSuccessful()){
                        LoyaltyBonusSignUpResponse signUpResponse = response.body();
                        if(signUpResponse!=null && signUpResponse.status.equals("success")){
                            List<LoyaltyBonusSignUpData> dataList = signUpResponse.data;
                            LoyaltyBonusSignUpData data = dataList.get(0);
                            if(data.emailId.equals(emailText)){
                                displaySuccessDialog(getString(R.string.loyalty_bonus_signup_success_message),
                                        getString(R.string.loyalty_bonus_signup_success_positive),"signUp Success");
                            }
                            return;
                        }
                        if(signUpResponse!=null && signUpResponse.status.equals("error")){
                            displayErrorDialog(signUpResponse.message);
                        }

                    }else{
                        displayErrorDialog(getString(R.string.loyalty_bonus_signup_unknown_error));
                    }
                }

                @Override
                public void onFailure(Call<LoyaltyBonusSignUpResponse> call, Throwable t) {
                    displayErrorDialog(getString(R.string.loyalty_bonus_signup_unknown_error));
                }
            });


        }else{
           displayErrorDialog(getString(R.string.loyalty_bonus_signup_no_connection));
        }
    }

    String getFormattedDate(int year, int month, int date){
        return year+"-"+month+"-"+date;
    }

    private void displaySuccessDialog(String message, String negative, String tag){
        operationSuccessDialog = new OperationSuccessDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(OperationSuccessDialog.OPERATION_TYPE_KEY,OperationSuccessDialog.OPERATION_TYPE_SINGUP_SUCCESS);
        bundle.putString(OperationSuccessDialog.NETWORK_INFO_MESSAGE
                ,message);
        bundle.putString(OperationSuccessDialog.NETWORK_INFO_BUTTON
                ,negative);
        operationSuccessDialog.setArguments(bundle);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(tag);
        operationSuccessDialog.show(fragmentTransaction,tag);
        signUpInfo.setVisibility(View.INVISIBLE);
        signUpProgress.setVisibility(View.INVISIBLE);
        signUpButton.setText(getString(R.string.loyalty_bonus_signup_button));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("shouldSignup",shouldValidateUserData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
    }
}


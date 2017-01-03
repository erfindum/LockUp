package com.smartfoxitsolutions.lockup;

import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;

import java.util.regex.Pattern;

/**
 * Created by RAAJA on 10-12-2016.
 */

public class SetEmailActivity extends AppCompatActivity {

    private EditText emailEdit;
    private Button addEmailButton, chooseEmailButton;
    private boolean shouldCloseAffinity;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_email_activity);
        emailEdit = (EditText) findViewById(R.id.set_email_activity_email_edit);
        chooseEmailButton = (Button) findViewById(R.id.set_email_activity_choose_button);
        addEmailButton = (Button) findViewById(R.id.set_email_activity_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        emailEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    addEmail(emailEdit.getText().toString());
                    return false;
                }
                return false;
            }
        });
        chooseEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
                    Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, null, null, null, null);
                    startActivityForResult(intent,44);
                }else{
                    Intent intent= AccountManager.newChooseAccountIntent(null,null,new String[]{"com.google"},false,null,null,null,null);
                    startActivityForResult(intent,44);
                }
            }
        });

        addEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               addEmail(emailEdit.getText().toString());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 44){
            if(data!=null){
                emailEdit.setText(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
            }else{
                displayToast(getString(R.string.set_email_activity_choose_email_help));
            }
        }
    }

    void addEmail(String emailString){
        if(emailString.isEmpty()){
            displayToast(getString(R.string.set_email_activity_empty_email));
            return;
        }
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        if(emailPattern.matcher(emailString).matches()){
            SharedPreferences.Editor edit = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                    .edit();
            edit.putString(LockUpSettingsActivity.RECOVERY_EMAIL_PREFERENCE_KEY,emailString);
            edit.putBoolean(AppLockModel.LOCK_UP_FIRST_LOAD_PREF_KEY,false);
            edit.apply();
            startLockUpMain();
        }else{
            displayToast(getString(R.string.set_email_activity_valid_email));
        }
    }

    void displayToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    void startLockUpMain(){
        ComponentName mediaMoveActivityComponent = new ComponentName(this, MediaMoveActivity.class);
        getPackageManager().setComponentEnabledSetting(mediaMoveActivityComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                ,PackageManager.DONT_KILL_APP);
        startActivity(new Intent(this, LockUpMainActivity.class));
        shouldCloseAffinity = false;
        finishAffinity();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        shouldCloseAffinity = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
        
    }
}

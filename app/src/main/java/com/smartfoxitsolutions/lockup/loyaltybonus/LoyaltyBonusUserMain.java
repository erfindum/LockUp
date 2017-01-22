package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 22-01-2017.
 */

public class LoyaltyBonusUserMain extends AppCompatActivity {

    Button logoutButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loyalty_bonus_user_main);
        logoutButton = (Button) findViewById(R.id.loyalty_bonus_user_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE)
                                                .edit();
                edit.putBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
                edit.apply();
            }
        });
    }
}

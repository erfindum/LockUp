package com.smartfoxitsolutions.lockup.userreward;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 12-01-2017.
 */

public class UserRewardInfo extends AppCompatActivity {

    TextView userVision,userPhysic;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_reward_info);
        userPhysic = (TextView) findViewById(R.id.user_reward_info_physic_info);
        userVision = (TextView) findViewById(R.id.user_reward_info_vision_info);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences(UserRewardModel.USER_REWARD_PREFERENCE_NAME,MODE_PRIVATE);
        long currentUserVision = prefs.getLong(UserRewardModel.DAILY_USER_VISION,0);
        long currentUserPhysic = prefs.getLong(UserRewardModel.DAILY_USER_PHYSIC,0);
        userVision.setText(String.valueOf(currentUserVision));
        userPhysic.setText(String.valueOf(currentUserPhysic));
    }
}

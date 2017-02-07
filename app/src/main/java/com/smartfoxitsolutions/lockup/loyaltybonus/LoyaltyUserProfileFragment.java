package com.smartfoxitsolutions.lockup.loyaltybonus;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by RAAJA on 29-01-2017.
 */

public class LoyaltyUserProfileFragment extends Fragment {

    private TextView fullName, noOfAppsLocked, appLockInfo,pointsEarned;
    private CardView paypalCard, paytmCard;
    private FloatingActionButton appLockButton;
    private LoyaltyUserActivity activity;
    public static int lockedRecommendApps,lockedApps;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.loyalty_bonus_user_main,container,false);
        fullName = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_welcome);
        noOfAppsLocked = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_lock_info_one);
        appLockInfo = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_lock_info_two);
        pointsEarned = (TextView) parent.findViewById(R.id.loyalty_bonus_user_main_point);

        appLockButton = (FloatingActionButton) parent.findViewById(R.id.loyalty_bonus_user_main_lock_fab);

        paypalCard = (CardView) parent.findViewById(R.id.loyalty_bonus_user_paypal_group);
        paytmCard = (CardView) parent.findViewById(R.id.loyalty_bonus_user_paytm_group);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (LoyaltyUserActivity) getActivity();
        setListeners();
        setAppLockInfo();
    }

    void setListeners(){
        paypalCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startRedeemFragment("paypal");
            }
        });

        paytmCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startRedeemFragment("paytm");
            }
        });

        appLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.checkAndSetUsagePermissions();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences loyaltyPrefs = activity.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME
                , Context.MODE_PRIVATE);
        String welcome = getString(R.string.loyalty_user_main_hello)+"! " +
                loyaltyPrefs.getString(LoyaltyBonusModel.LOGIN_USER_NAME_KEY,"Unknown");
        fullName.setText(welcome);
        pointsEarned.setText(loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_BONUS,"00.00"));

        String lockInfo =(lockedRecommendApps+lockedApps)+" "+getString(R.string.loyalty_user_main_lock_info_one);
        noOfAppsLocked.setText(lockInfo);
        if((lockedRecommendApps + lockedApps)<=3){
            appLockInfo.setText(getString(R.string.loyalty_user_main_lock_info_two));
        }else{
            appLockInfo.setVisibility(View.INVISIBLE);
        }
    }

    private void setAppLockInfo(){
        SharedPreferences appPrefs = activity.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME
                , Context.MODE_PRIVATE);
        AppLockModel appModel = new AppLockModel(appPrefs);

        String installerPackage = getAppInstallerPackage();
        LinkedHashMap<String,HashMap<String,Boolean>> recommendedMap = appModel.getRecommendedAppsMap();
        for(Map.Entry<String,HashMap<String,Boolean>> entry:recommendedMap.entrySet()){
            if(!entry.getKey().equals(installerPackage)){
                ArrayList<Boolean> tempList = new ArrayList<>(entry.getValue().values());
                if(tempList.get(0)){
                    lockedRecommendApps++;
                }
            }
        }
        TreeMap<String,String> checkedAppMap = appModel.getCheckedAppsMap();
        lockedApps = checkedAppMap.size();
    }

    private String getAppInstallerPackage(){
        Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        installIntent.addCategory(Intent.CATEGORY_DEFAULT);
        installIntent.setDataAndType(Uri.parse("file:///"),"application/vnd.android.package-archive");
        List<ResolveInfo> installerPackages = activity.getPackageManager().queryIntentActivities(installIntent, PackageManager.GET_META_DATA);

        if(installerPackages!=null && !installerPackages.isEmpty()){
            ResolveInfo installerInfo = installerPackages.get(0);
            return installerInfo.activityInfo.packageName;
        }else{
            return "none";
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        activity = null;
        lockedRecommendApps = 0;
        lockedApps = 0;
    }
}

package com.smartfoxitsolutions.lockup;


import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;
import com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusModel;
import com.smartfoxitsolutions.lockup.loyaltybonus.UserLoyaltyReport;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Created by RAAJA on 24-05-2017.
 */

public class SlideLockActivity extends FragmentActivity implements View.OnTouchListener{

    private MoPubView bannerAd;
    private AdView bannerAdView;
    private TextView timeText, dateText, slideText;
    private ImageView img_arrow;
    private ImageButton img_btn_settings;
    private Button btn_turnOff;
    private RelativeLayout slideParent;
    private UserLoyaltyReport userLoyaltyReport;
    private Type userReportMapToken;
    private Gson gson;
    private SharedPreferences appPrefs;
    public static boolean shouldCloseLock, isUserLoggedIn;
    float initialX = 0;
    int viewWidth = 0;
    private TelephonyManager telephonyManager;
    private PhoneCallStateListener phoneCallStateListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldCloseLock = true;
        setContentView(R.layout.slide_lock_activity);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneCallStateListener = new PhoneCallStateListener(new WeakReference<>(this));
        telephonyManager.listen(phoneCallStateListener,PhoneStateListener.LISTEN_CALL_STATE);
        try{
            MobileAds.initialize(this.getApplicationContext(),"ca-app-pub-2878097117146801~6210554174");
        }catch (Exception e){
            Log.d("LockUp","Main Lock Exception : " + e);
        }
        bannerAdView = (AdView) findViewById(R.id.ad_slide_lock);

        slideParent = (RelativeLayout) findViewById(R.id.parent_slide_view);
        slideParent.setOnTouchListener(this);
        timeText = (TextView) findViewById(R.id.tv_slide_lock_time);
        dateText = (TextView) findViewById(R.id.tv_slide_lock_date);
        slideText = (TextView) findViewById(R.id.tv_slide_info);
        img_arrow = (ImageView) findViewById(R.id.img_slide_arrow_one);
        img_btn_settings = (ImageButton) findViewById(R.id.img_btn_slide_settings);
        btn_turnOff = (Button) findViewById(R.id.btn_slide_off);
        appPrefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        setTrackingDetails();
        long adInterval = appPrefs.getLong(AppLockModel.SWIPE_LOCK_AD_DISPLAY,0);
        if(System.currentTimeMillis()>=adInterval){
            requestAd();
        }else{
            changeClockPosition();
        }
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/arquitectabook.ttf");
        timeText.setTypeface(typeface);
        dateText.setTypeface(typeface);
        slideText.setTypeface(typeface);
        btn_turnOff.setTypeface(typeface);
        setListeners();
    }

    private void requestAd(){
        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAdView.loadAd(adRequest);
        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Log.d("SlideLockMopub","ad closed");

            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                if (isUserLoggedIn && userLoyaltyReport != null) {
                    userLoyaltyReport.setTotalClicked2(Integer.parseInt(userLoyaltyReport.getTotalClicked2()) + 1);
                    SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
                    String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
                    LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
                    if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
                        userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(), userLoyaltyReport);
                    }
                    SharedPreferences.Editor edit = loyaltyPrefs.edit();
                    String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
                    edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT, userReportUpdateString);
                    edit.apply();
                }
                SharedPreferences.Editor editor = appPrefs.edit();
                editor.putLong(AppLockModel.SWIPE_LOCK_AD_DISPLAY,System.currentTimeMillis()+(5*60000));
                editor.apply();
                finish();

            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if (isUserLoggedIn && userLoyaltyReport != null) {
                    userLoyaltyReport.setTotalImpression2(Integer.parseInt(userLoyaltyReport.getTotalImpression2()) + 1);
                    SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
                    String userReportCurrentString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
                    LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportCurrentString, userReportMapToken);
                    if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
                        userLoyaltyReportMap.put(userLoyaltyReport.getReportDate(), userLoyaltyReport);
                    }
                    SharedPreferences.Editor edit = loyaltyPrefs.edit();
                    String userReportUpdateString = gson.toJson(userLoyaltyReportMap, userReportMapToken);
                    edit.putString(LoyaltyBonusModel.USER_LOYALTY_REPORT, userReportUpdateString);
                    edit.apply();
                    Log.d("SlideLockMopub","ad Impressed");
                }

            }

        });
    }

    private void changeClockPosition(){
        RelativeLayout.LayoutParams timeParams = (RelativeLayout.LayoutParams) timeText.getLayoutParams();
        timeParams.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
        timeParams.topMargin = 0;
        timeText.setLayoutParams(timeParams);
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP,80);
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String time = timeFormat.format(date);
        timeText.setText(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd");
        String dateString = dateFormat.format(date);
        dateText.setText(dateString);
        final ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1,0.4f);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                slideText.setAlpha((Float) animation.getAnimatedValue());
                img_arrow.setAlpha((Float) animation.getAnimatedValue());
            }
        });
        alphaAnimator.setDuration(600);
        alphaAnimator.setInterpolator(new LinearInterpolator());
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
        alphaAnimator.start();

    }

    private void setListeners(){
        img_btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_turnOff.setVisibility(View.VISIBLE);
            }
        });

        btn_turnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SlideLockActivity.this,MainLockActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("show_settings",true));
                finish();
            }
        });
    }

    private void setTrackingDetails(){
        SharedPreferences loyaltyPrefs = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME, MODE_PRIVATE);
        String userReportString = loyaltyPrefs.getString(LoyaltyBonusModel.USER_LOYALTY_REPORT, null);
        gson = new Gson();
        userReportMapToken = new TypeToken<LinkedHashMap<String, UserLoyaltyReport>>() {
        }.getType();
        LinkedHashMap<String, UserLoyaltyReport> userLoyaltyReportMap = gson.fromJson(userReportString, userReportMapToken);
        if (userLoyaltyReportMap != null && !userLoyaltyReportMap.isEmpty()) {
            ArrayList<String> dateKeyList = new ArrayList<>(userLoyaltyReportMap.keySet());
            String dateKey = dateKeyList.get(dateKeyList.size() - 1);
            userLoyaltyReport = userLoyaltyReportMap.get(dateKey);
        }
        isUserLoggedIn = loyaltyPrefs.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY, false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("LockMopup",String.valueOf(v.getId()== R.id.parent_slide_view));

        if(v.getId()== R.id.parent_slide_view){
            switch (event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    if(btn_turnOff.getVisibility() == View.VISIBLE){
                        btn_turnOff.setVisibility(View.INVISIBLE);
                    }
                    viewWidth = v.getRight();
                    initialX = event.getRawX();
                    Log.d("LockMopub",event.getRawX()+ " " + event.getX() + " X");
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX()-initialX);
                    break;

                case MotionEvent.ACTION_UP:
                    if(((event.getRawX()-initialX)/viewWidth)*100 < 20){
                        v.setX(0);
                    }else{
                        finish();
                    }
            }
            return true;
        }
        return false;
    }

    void checkCallState(int state){
        switch (state){
            case TelephonyManager.CALL_STATE_IDLE:
                Log.d("LockUpSlide","Call Idle");
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.d("LockUpSlide","Call OffHook");
                finish();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                finish();
                Log.d("LockUpSlide","Call Ringing");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
       // bannerAd.destroy();
        super.onDestroy();
        telephonyManager.listen(phoneCallStateListener,PhoneStateListener.LISTEN_NONE);
        shouldCloseLock = false;
    }

    private static class PhoneCallStateListener extends PhoneStateListener{

        WeakReference<SlideLockActivity> slideActivity;

        PhoneCallStateListener(WeakReference<SlideLockActivity> activity){
            slideActivity = activity;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            slideActivity.get().checkCallState(state);
        }
    }
}

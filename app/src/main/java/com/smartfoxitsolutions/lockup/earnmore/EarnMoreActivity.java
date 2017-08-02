package com.smartfoxitsolutions.lockup.earnmore;

import android.os.Bundle;
import android.provider.Settings;
import android.renderscript.Double2;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.offertoro.sdk.OTOfferWallSettings;
import com.offertoro.sdk.OTSurveySettings;
import com.offertoro.sdk.interfaces.OfferWallListener;
import com.offertoro.sdk.interfaces.SurveyWallListener;
import com.offertoro.sdk.sdk.OffersInit;
import com.pollfish.interfaces.PollfishClosedListener;
import com.pollfish.interfaces.PollfishOpenedListener;
import com.pollfish.interfaces.PollfishSurveyCompletedListener;
import com.pollfish.interfaces.PollfishSurveyNotAvailableListener;
import com.pollfish.interfaces.PollfishSurveyReceivedListener;
import com.pollfish.main.PollFish;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.dialogs.NetworkProcessDialog;

/**
 * Created by RAAJA on 04-07-2017.
 */

public class EarnMoreActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private boolean pollfishOpened,shouldOpenClosePollfish = true,hasPollfishFetched, hasOTSurveyFetched, isOTOfferReady;
    private double pollfishPoints,OTSurveyPoints, OTOfferPoints;
    private NetworkProcessDialog processDialog;
    private final int SURVEY_TYPE_POLLFISH = 34, SURVEY_TYPE_OFFERTORO = 35, OFFER_TYPE_OFFERTORO = 36;
    private int offerType;
    private String deviceId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earn_more_activity);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_earn_more);
        ViewPager pager = (ViewPager) findViewById(R.id.view_pager_earn_more);
        pager.setAdapter(new EarnMorePagerAdapter(getSupportFragmentManager(),
                getResources().getStringArray(R.array.extra_earning_fragment_title)));
        tabLayout.setupWithViewPager(pager);
        toolbar = (Toolbar) findViewById(R.id.toolbar_earn_more);
        setSupportActionBar(toolbar);

    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setTitle(getString(R.string.extra_earning_title));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initOTSurvey();
        initPollfish();
        initOTOfferWall();
    }

    private void initPollfish(){
        PollFish.ParamsBuilder paramsBuilder = new PollFish.ParamsBuilder(getString(R.string.survey_pollfish_api_key))
                .customMode(true)
                .requestUUID(deviceId)
                .pollfishOpenedListener(new PollfishOpenedListener() {
                    @Override
                    public void onPollfishOpened() {
                        pollfishOpened = true;
                        setShouldOpenClosePollfish(true);
                        Toast.makeText(EarnMoreActivity.this,"Complete Survey to earn "+ pollfishPoints+" $",Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .pollfishClosedListener(new PollfishClosedListener() {
                    @Override
                    public void onPollfishClosed() {
                        pollfishOpened = false;
                        setShouldOpenClosePollfish(true);
                    }
                })
                .pollfishSurveyReceivedListener(new PollfishSurveyReceivedListener() {
                    @Override
                    public void onPollfishSurveyReceived(boolean b, int i) {
                        hasPollfishFetched = true;
                        pollfishPoints = i;
                        hideProcessDialog();
                    }
                })
                .pollfishSurveyNotAvailableListener(new PollfishSurveyNotAvailableListener() {
                    @Override
                    public void onPollfishSurveyNotAvailable() {
                        hideProcessDialog();
                        Toast.makeText(EarnMoreActivity.this,getString(R.string.survey_pollfish_error),Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .pollfishSurveyCompletedListener(new PollfishSurveyCompletedListener() {
                    @Override
                    public void onPollfishSurveyCompleted(boolean b, int i) {
                        String credits = String.format(getString(R.string.survey_credited),i);
                        Toast.makeText(EarnMoreActivity.this,credits,Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .build();
        PollFish.initWith(EarnMoreActivity.this,paramsBuilder);
        PollFish.hide();
    }

    void setShouldOpenClosePollfish(boolean shouldOpenClose){
        this.shouldOpenClosePollfish = shouldOpenClose;
    }

    void openPollfishPanel(){
        if(!hasPollfishFetched){
            displayProcessDialog(getString(R.string.survey_process_dialog_title));
            offerType = SURVEY_TYPE_POLLFISH;
            return;
        }
        if(!shouldOpenClosePollfish){
            return;
        }
        setShouldOpenClosePollfish(false);
        if(pollfishOpened){
            PollFish.hide();
            return;
        }
        PollFish.show();
    }

    private void initOTSurvey(){
        OTSurveySettings.getInstance().configInit(getString(R.string.survey_offertoro_app_ID),
                getString(R.string.survey_offertoro_app_key),deviceId);


        OffersInit.getInstance().create(EarnMoreActivity.this);
        OffersInit.getInstance().setSurveyListener(new SurveyWallListener() {
            @Override
            public void onOTSurveyInitSuccess() {
                hasOTSurveyFetched = true;
                hideProcessDialog();
            }

            @Override
            public void onOTSurveyInitFail(String s) {
                hideProcessDialog();
                Toast.makeText(EarnMoreActivity.this,getString(R.string.survey_offertoro_error),Toast.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onOTSurveyOpened() {

            }

            @Override
            public void onOTSurveyCredited(double v, double v1) {
                OTSurveyPoints = v;

            }

            @Override
            public void onOTSurveyClosed() {
                OffersInit.getInstance().getOTSurveyCredits();
                String credits = String.format(getString(R.string.survey_credited),OTSurveyPoints);
                Toast.makeText(EarnMoreActivity.this,credits,Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    void openOTSurveyWall(){
        if(!hasOTSurveyFetched){
            displayProcessDialog(getString(R.string.survey_process_dialog_title));
            offerType = SURVEY_TYPE_OFFERTORO;
            return;
        }
        OffersInit.getInstance().showSurvey(EarnMoreActivity.this);
    }

    private void initOTOfferWall(){
        OTOfferWallSettings.getInstance().configInit(getString(R.string.offerwall_offertoro_app_ID),
                        getString(R.string.offerwall_offertoro_app_key),
                        deviceId);
        OffersInit.getInstance().create(EarnMoreActivity.this);
        OffersInit.getInstance().setOfferWallListener(new OfferWallListener() {
            @Override
            public void onOTOfferWallInitSuccess() {
                isOTOfferReady = true;
                hideProcessDialog();

            }

            @Override
            public void onOTOfferWallInitFail(String s) {
                hideProcessDialog();
                Toast.makeText(EarnMoreActivity.this,getString(R.string.offerwall_offertoro_error),Toast.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onOTOfferWallOpened() {

            }

            @Override
            public void onOTOfferWallCredited(double v, double v1) {
                OTOfferPoints = v;
            }

            @Override
            public void onOTOfferWallClosed() {

            }
        });
    }

    void openOTOfferWall(){
        if(!isOTOfferReady){
            displayProcessDialog(getString(R.string.offerwall_process_dialog_title));
            offerType = OFFER_TYPE_OFFERTORO;
            return;
        }
        OffersInit.getInstance().showOfferWall(EarnMoreActivity.this);

    }

    private void displayProcessDialog(String title){
        processDialog = new NetworkProcessDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(NetworkProcessDialog.NETWORK_DIALOG_TYPE,NetworkProcessDialog.NETWORK_DIALOG_TYPE_LOADING);
        bundle.putString(NetworkProcessDialog.NETWORK_INFO_HEADER,title);
        bundle.putString(NetworkProcessDialog.NETWORK_INFO_MESSAGE,getString(R.string.survey_process_dialog_message));
        processDialog.setArguments(bundle);
        processDialog.show(getSupportFragmentManager(),"survey_process_dialog");
    }

    private void hideProcessDialog(){
        if(processDialog!=null){
            if(processDialog.isVisible()){
                processDialog.dismiss();
                if(offerType == SURVEY_TYPE_POLLFISH){
                    openPollfishPanel();
                }
                if(offerType == SURVEY_TYPE_OFFERTORO){
                    openOTSurveyWall();
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hasPollfishFetched = false;
        hasOTSurveyFetched = false;
        isOTOfferReady = false;
    }
}

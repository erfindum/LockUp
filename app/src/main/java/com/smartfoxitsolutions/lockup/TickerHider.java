package com.smartfoxitsolutions.lockup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by RAAJA on 19-10-2016.
 */

public class TickerHider extends FrameLayout implements View.OnTouchListener {

    Context context;
    OnTickerHiderActiveListener tickerHiderListener;
    ObjectAnimator tickerStart,tickerStill, tickerEnd;
    AnimatorSet animatorSet;
    ImageView appIcon;
    TextView subText,textTitle;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof RelativeLayout){
            if(tickerHiderListener !=null){
                cancelAnimation();
                tickerHiderListener.onTickerHidden();
            }
            return true;
        }
        return false;
    }

    interface OnTickerHiderActiveListener{
        void onTickerHidden();
    }

    public TickerHider(Context context) {
        super(context);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.ticker_hider,this,true);
        textTitle = (TextView) findViewById(R.id.ticker_hider_app_text);
        subText = (TextView) findViewById(R.id.ticker_hider_app_sub_text);
        appIcon = (ImageView) findViewById(R.id.ticker_hider_app_icon);
    }

    void setAppIcon(String packageName){
        try{
           appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageName));
        }
        catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    void setTickerText(String head, String subHead){
        try {
            PackageManager pkgManager = context.getPackageManager();
            ApplicationInfo info = pkgManager.getApplicationInfo(head,PackageManager.GET_META_DATA);
            String name = (String) pkgManager.getApplicationLabel(info);
            textTitle.setText(name);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        subText.setText(subHead);
    }

    void setOnTickerHiderActiveListener(OnTickerHiderActiveListener listener){
        this.tickerHiderListener = listener;
    }

    void setAnimation(){

            tickerStart = ObjectAnimator.ofFloat(this,"bottom",0f,(float) this.getBottom());
            tickerStart.setDuration(800).setInterpolator(new AnticipateOvershootInterpolator());
            tickerStart.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (float) animation.getAnimatedValue();
                   // Log.d("NotificationLock",val + " value");
                }
            });

            tickerStill = ObjectAnimator.ofInt(this,"bottom",this.getBottom(),this.getBottom());


            tickerEnd = ObjectAnimator.ofInt(this,"bottom",this.getBottom(),0);
            tickerEnd.setInterpolator(new AccelerateDecelerateInterpolator());
            tickerEnd.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if(tickerHiderListener!=null){
                        tickerHiderListener.onTickerHidden();
                    }
                }
            });

            animatorSet =new AnimatorSet();
            animatorSet.setDuration(1300);
            animatorSet.playSequentially(tickerStart,tickerStill,tickerEnd);

    }

    void playAnimation(){
        animatorSet.start();
    }

    void cancelAnimation(){
        animatorSet.cancel();
    }

    void closeView(){
        context = null;
        this.tickerHiderListener = null;
    }
}
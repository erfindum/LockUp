package com.smartfoxitsolutions.lockup.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 19-10-2016.
 */

public class TickerHider extends FrameLayout  {

    Context context;
    OnTickerHiderActiveListener tickerHiderListener;
    ObjectAnimator tickerStart,tickerStill, tickerEnd;
    AnimatorSet animatorSet;
    ImageView appIcon;
    TextView subText,textTitle;
    RelativeLayout tickerHiderParent;

   /* @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(ev.getActionMasked() == KeyEvent.ACTION_UP) {
            if (tickerHiderListener != null) {
                cancelAnimation();
                tickerHiderListener.onTickerHidden();
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    } */

    public interface OnTickerHiderActiveListener{
        void onTickerHidden();
    }

    public TickerHider(Context context) {
        super(context);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.ticker_hider,this,true);
        tickerHiderParent = (RelativeLayout) findViewById(R.id.ticker_hider_parent);
        textTitle = (TextView) findViewById(R.id.ticker_hider_app_text);
        subText = (TextView) findViewById(R.id.ticker_hider_app_sub_text);
        appIcon = (ImageView) findViewById(R.id.ticker_hider_app_icon);
    }

    public void setAppIcon(String packageName){
        try{
           appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageName));
        }
        catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    public void setTickerText(String head, String subHead){
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

    public void setOnTickerHiderActiveListener(OnTickerHiderActiveListener listener){
        this.tickerHiderListener = listener;
        tickerHiderParent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tickerHiderListener != null) {
                    cancelAnimation();
                    tickerHiderListener.onTickerHidden();
                }
            }
        });
    }

    public void setAnimation(){

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

    public void playAnimation(){
        animatorSet.start();
    }

    void cancelAnimation(){
        animatorSet.cancel();
    }

    public void closeView(){
        context = null;
        this.tickerHiderListener = null;
    }
}
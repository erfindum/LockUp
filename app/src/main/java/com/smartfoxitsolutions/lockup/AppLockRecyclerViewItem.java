package com.smartfoxitsolutions.lockup;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockRecyclerViewItem extends RecyclerView.ViewHolder  {
    private View itemView;
    private TextView appName;
    private AppCompatImageView appImage;
    private AppCompatImageButton lockButton;
    private onAppListItemClickListener onAppListItemClickListener;
    private AnimatorSet lockAnimator;

    public interface onAppListItemClickListener{
        void onAppListItemClicked(AppLockRecyclerViewItem itemView,int listItemPosition);
    }
    public void setOnAppListItemClickListener(onAppListItemClickListener listener){
        this.onAppListItemClickListener = listener;
    }

    public AppLockRecyclerViewItem(View itemView) {
        super(itemView);
        this.itemView = itemView;
        this.lockAnimator =new AnimatorSet();
        setItemView();
    }
    private void setItemView(){
        appName = (TextView) itemView.findViewById(R.id.app_lock_activity_recycler_app_appName);
        appImage = (AppCompatImageView) itemView.findViewById(R.id.app_lock_activity_recycler_app_icon_imageView);
        lockButton = (AppCompatImageButton) itemView.findViewById(R.id.app_lock_activity_recycler_app_lock_button);

        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ClickListener "," IMAGGGGE VIEW CLick" + String.valueOf(onAppListItemClickListener == null));
                if(onAppListItemClickListener!=null){
                    sendClickEvent();;
                }
            }
        });
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ClickListener "," ItemView CLick" + String.valueOf(onAppListItemClickListener == null));
                if(onAppListItemClickListener!=null){
                    sendClickEvent();
                }
            }
        });

       ObjectAnimator lockAnimatorX = ObjectAnimator.ofInt(getLockButton(),"scaleX",0,1);
       // ObjectAnimator lockAnimatorY = ObjectAnimator.ofInt(getNotificationButton(),"scaleY",0,1);
        lockAnimatorX.setDuration(500);
      //  lockAnimatorY.setDuration(500);
       // lockAnimator.setTarget(getNotificationButton());
       // lockAnimator.play(lockAnimatorX).with(lockAnimatorY);
        lockAnimator.setInterpolator(new BounceInterpolator());
        lockAnimator.play(lockAnimatorX);

    }

    private void sendClickEvent(){
        onAppListItemClickListener.onAppListItemClicked(this,getLayoutPosition());
    }

    TextView getAppName(){
        return this.appName;
    }

    ImageView getAppImage(){
        return this.appImage;
    }

    AppCompatImageButton getLockButton(){
        return this.lockButton;
    }

    AnimatorSet getLockButtonAnimator(){return this.lockAnimator;}

    View getItemView(){
        return  this.itemView;
    }
}

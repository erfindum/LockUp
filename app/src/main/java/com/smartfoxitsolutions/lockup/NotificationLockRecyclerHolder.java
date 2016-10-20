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
 * Created by RAAJA on 20-10-2016.
 */

public class NotificationLockRecyclerHolder extends RecyclerView.ViewHolder {
    private View itemView;
    private TextView appName;
    private AppCompatImageView appImage;
    private AppCompatImageButton notificationButton;
    private OnListItemClickListener onListItemClickListener;
    private AnimatorSet notifAnimator;

    public interface OnListItemClickListener {
        void onListItemClicked(NotificationLockRecyclerHolder itemView,int listItemPosition);
    }
    public void setOnListItemClickListener(OnListItemClickListener listener){
        this.onListItemClickListener = listener;
    }

    public NotificationLockRecyclerHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        this.notifAnimator =new AnimatorSet();
        setItemView();
    }

    private void setItemView(){
        appName = (TextView) itemView.findViewById(R.id.notification_item_app_appName);
        appImage = (AppCompatImageView) itemView.findViewById(R.id.notification_item_app_icon_imageView);
        notificationButton = (AppCompatImageButton) itemView.findViewById(R.id.notification_item_lock_button);

        notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ClickListener "," IMAGGGGE VIEW CLick" + String.valueOf(onListItemClickListener == null));
                if(onListItemClickListener!=null){
                    sendClickEvent();;
                }
            }
        });
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ClickListener "," ItemView CLick" + String.valueOf(onListItemClickListener == null));
                if(onListItemClickListener!=null){
                    sendClickEvent();
                }
            }
        });

        ObjectAnimator lockAnimatorX = ObjectAnimator.ofInt(getNotificationButton(),"scaleX",0,1);
        // ObjectAnimator lockAnimatorY = ObjectAnimator.ofInt(getNotificationButton(),"scaleY",0,1);
        lockAnimatorX.setDuration(500);
        //  lockAnimatorY.setDuration(500);
        // notifAnimator.setTarget(getNotificationButton());
        // notifAnimator.play(lockAnimatorX).with(lockAnimatorY);
        notifAnimator.setInterpolator(new BounceInterpolator());
        notifAnimator.play(lockAnimatorX);

    }

    private void sendClickEvent(){
        onListItemClickListener.onListItemClicked(this,getLayoutPosition());
    }

    TextView getAppName(){
        return this.appName;
    }

    ImageView getAppImage(){
        return this.appImage;
    }

    AppCompatImageButton getNotificationButton(){
        return this.notificationButton;
    }

    AnimatorSet getNotifButtonAnimator(){return this.notifAnimator;}

    View getItemView(){
        return  this.itemView;
    }
}

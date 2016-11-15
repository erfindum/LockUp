package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 24-09-2016.
 */

public class MediaPickerHolder extends RecyclerView.ViewHolder {

    private View itemView;
    private AppCompatImageView thumbnailView;
    private AppCompatImageView ticker;
    private View selectedThumbnail;
    private OnMediaPickedListener listener;
    private ValueAnimator itemAnimator;

     interface OnMediaPickedListener {
        void onMediaPicked(int mediaPosition, MediaPickerHolder holder);
    }

    void setOnMediaPickedListener(OnMediaPickedListener listener){
        this.listener = listener;
    }

     MediaPickerHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        thumbnailView = (AppCompatImageView) itemView.findViewById(R.id.vault_media_picker_item_image);
        selectedThumbnail = itemView.findViewById(R.id.vault_media_picker_item_selected);
        ticker = (AppCompatImageView) itemView.findViewById(R.id.vault_media_picker_item_tick);
        setItemAnimation();
        setClickListener();
    }

    AppCompatImageView getThumbnailView(){
        return this.thumbnailView;
    }

    ValueAnimator getItemAnimator(){
        return itemAnimator;
    }

    void setItemSelected(){
        selectedThumbnail.setVisibility(View.VISIBLE);
        ticker.setVisibility(View.VISIBLE);
    }

    void setItemDeselected(){
        selectedThumbnail.setVisibility(View.INVISIBLE);
        ticker.setVisibility(View.INVISIBLE);
    }

    private void setClickListener(){
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                  sendMediaSelected();
            }
        });
    }

    private void sendMediaSelected(){
        listener.onMediaPicked(getLayoutPosition(),this);
    }

    private void setItemAnimation(){
        itemAnimator = ValueAnimator.ofInt(0,1);
        itemAnimator.setDuration(200).setInterpolator(new OvershootInterpolator());
        itemAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                ticker.setScaleY(0);
                ticker.setScaleX(0);
                ticker.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                selectedThumbnail.setVisibility(View.VISIBLE);
                Log.d("Vault",selectedThumbnail.getHeight()+" Height");
            }
        });
        itemAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animationValue = (int) animation.getAnimatedValue();
                ticker.setScaleX(animationValue);
                ticker.setScaleY(animationValue);
            }
        });
    }
}

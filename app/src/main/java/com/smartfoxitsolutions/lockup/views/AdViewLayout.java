package com.smartfoxitsolutions.lockup.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 01-04-2017.
 */

public class AdViewLayout extends RelativeLayout {
    int statusBarHeight, marginFive, marginTwo, requiredWidthMargin;
    public AdViewLayout(Context context) {
        super(context);
    }

    public AdViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int adParentHeight = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int adParentWidth = getDensityPixels();
        if(MeasureSpec.getMode(heightMeasureSpec)== MeasureSpec.AT_MOST) {
            int childCount = getChildCount();
            statusBarHeight = getStatusBarHeight();
            marginFive = getResources().getDimensionPixelSize(R.dimen.fiveDpDimension);
            marginTwo = getResources().getDimensionPixelSize(R.dimen.twoDpDimension);
            requiredWidthMargin = getResources().getDimensionPixelSize(R.dimen.tenDpDimension);
            if (childCount > 0) {
                ImageView imageView = (ImageView) getChildAt(0);
                View callText = getChildAt(2);
                View adInfo = getChildAt(3);
                int height;
                int width;
                    height = (int) Math.round(adParentHeight - (callText.getMeasuredHeight() + adInfo.getMeasuredHeight()
                            + (marginFive * 2) + (marginTwo * 2))) - (statusBarHeight);
                    width = (int) Math.round((height * 1.85));
                    if((width+requiredWidthMargin)>adParentWidth){
                        width = (int) Math.round( adParentWidth*0.70);
                        height = (int) Math.round(width*0.60);
                    }
                    imageView.setMinimumWidth(width);
                    imageView.setMinimumHeight(height);
                    imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(height + (marginFive * 2) + callText.getMeasuredHeight()
                            + adInfo.getMeasuredHeight() + (marginTwo * 2), MeasureSpec.EXACTLY);
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(width + (marginFive * 2), MeasureSpec.EXACTLY);

            }
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }

    private int getStatusBarHeight(){
        int statusHeight;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
           statusHeight = getResources().getDimensionPixelSize(R.dimen.twentyFiveDpDimension) *2;
        }else{
            statusHeight = getResources().getDimensionPixelSize(R.dimen.twentyFiveDpDimension);
        }
        return statusHeight;
    }

    private int getDensityPixels(){
        return (int)getResources().getDisplayMetrics().widthPixels;
    }

}

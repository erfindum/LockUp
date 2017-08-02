package com.smartfoxitsolutions.lockup;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by RAAJA on 25-03-2017.
 */

public class AdViewPager extends ViewPager {
    public AdViewPager(Context context) {
        super(context);
    }

    public AdViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(getChildCount()==0){
            return;
        }
        ViewGroup childView = (ViewGroup)getChildAt(0);
        if(childView.getChildCount()>0 && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            int height = childView.getChildAt(0).getMeasuredHeight();
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        }
        if(childView.getChildCount()>0 && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            int width = childView.getChildAt(0).getMeasuredWidth();
            int padding = (getMeasuredWidth()-width)/2;
            setPadding(padding,0,padding,0);
            width = width+(padding*2);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }
}

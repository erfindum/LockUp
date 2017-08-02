package com.smartfoxitsolutions.lockup;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by RAAJA on 25-03-2017.
 */

public class AdPagerAdapter extends PagerAdapter {
    ArrayList<View> adViewList;

    public AdPagerAdapter(ConcurrentLinkedQueue<View> adQueue) {
        adViewList = new ArrayList<>(adQueue);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ViewGroup.LayoutParams parms;
        parms = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT);
        View adView = adViewList.get(position);
        container.addView(adView,parms);
        return adView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
    }

    @Override
    public int getCount() {
        return adViewList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == (View) object;
    }
}

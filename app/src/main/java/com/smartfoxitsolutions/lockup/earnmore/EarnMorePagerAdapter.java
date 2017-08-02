package com.smartfoxitsolutions.lockup.earnmore;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by RAAJA on 04-07-2017.
 */

public class EarnMorePagerAdapter extends FragmentStatePagerAdapter {

    private String[] fragmentTitle;

    public EarnMorePagerAdapter(FragmentManager fm,String[] title) {
        super(fm);
        this.fragmentTitle = title;
    }

    @Override
    public Fragment getItem(int position) {
        if(position==1){
            return new SurveyFragment();
        }else{
            return new OffersFragment();
        }
    }

    @Override
    public int getCount() {
        return fragmentTitle.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        super.getPageTitle(position);
        if(position==1){
            return fragmentTitle[1];
        }else{
            return fragmentTitle[0];
        }
    }
}

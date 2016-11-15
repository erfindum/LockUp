package com.smartfoxitsolutions.lockup.mediavault;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by RAAJA on 01-10-2016.
 */

public class MediaVaultAlbumPagerAdapter extends FragmentPagerAdapter {

    private String[] tabText;

     MediaVaultAlbumPagerAdapter(FragmentManager fm, String[] tabText) {
        super(fm);
        this.tabText = tabText;
    }

    @Override
    public Fragment getItem(int position) {
        MediaVaultAlbumFragment frag;
        switch (position){
            case 0:
                frag = new MediaVaultAlbumFragment();
                frag.setMediaType(MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA);
                return frag;
            case 1:
                frag = new MediaVaultAlbumFragment();
                frag.setMediaType(MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA);
                return frag;
            case 2:
                frag = new MediaVaultAlbumFragment();
                frag.setMediaType(MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA);
                return frag;
        }

        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        super.getPageTitle(position);
        return tabText[position];
    }
}

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
        switch (position){
            case 0:
                MediaVaultAlbumFragment audioFrag = new MediaVaultAlbumFragment();
                audioFrag.setMediaType(MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA);
                return audioFrag;
            case 1:
                MediaVaultAlbumFragment imageFrag = new MediaVaultAlbumFragment();
                imageFrag.setMediaType(MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA);
                return imageFrag;
            case 2:
                MediaVaultAlbumFragment videoFrag = new MediaVaultAlbumFragment();
                videoFrag.setMediaType(MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA);
                return videoFrag;
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

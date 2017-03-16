package com.smartfoxitsolutions.lockup.mediavault;

import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.smartfoxitsolutions.lockup.R;

import java.io.File;
import java.util.LinkedList;

/**
 * Created by RAAJA on 15-11-2016.
 */

public class VaultImageViewPagerAdapter extends PagerAdapter {

    VaultImageViewActivity activity;
    LinkedList<String> originalNameList,fileExtensionList, vaultPathList;
    ImageViewState viewState;

    VaultImageViewPagerAdapter(VaultImageViewActivity activity){
        originalNameList = VaultImageViewActivity.originalFileNameList;
        fileExtensionList = VaultImageViewActivity.fileExtensionList;
        vaultPathList = VaultImageViewActivity.vaultFileList;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return vaultPathList.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position){
        File currentFile = new File(vaultPathList.get(position));
        File currentRenameFile = new File(vaultPathList.get(position)+"."+fileExtensionList.get(position));
        if(currentFile.exists()) {
            currentFile.renameTo(currentRenameFile);
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        View parent = inflater.inflate(R.layout.vault_image_view_activity_item,container,false);
        SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) parent.findViewById(R.id.vault_image_view_pager_image_view);
        imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
        if(viewState==null) {
            imageView.setImage(
                    ImageSource.uri(vaultPathList.get(position) + "." + fileExtensionList.get(position)));
        }else{
            imageView.setImage(
                    ImageSource.uri(vaultPathList.get(position) + "." + fileExtensionList.get(position)),viewState);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(activity.isTopBottomVisible){
                    activity.hideTopBottomBars();
                }else{
                    activity.showTopBottomBars();
                }
            }
        });
        activity.viewState = imageView.getState();
        viewState = null;
        container.addView(parent);
        activity.originalFileName.setText(originalNameList.get(position));
        Log.d("VaultImage","Called instantiate View "+ position);
        return parent;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == (object);
    }

    void setImageViewState(ImageViewState imageViewState){
        this.viewState = imageViewState;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        File oldFile = new File(vaultPathList.get(position) + "." + fileExtensionList.get(position));
        if (oldFile.exists()) {
            oldFile.renameTo(new File(vaultPathList.get(position)));
            Log.d("VaultImage","Renamed file "+ position);
        }
    }

    void renameCurrentFile(int position){
        if(position>-1) {
            Log.d("VaultImage", "Current Position" + (position) + " Data size" + vaultPathList.size());
            File oldCurrentFile = new File(vaultPathList.get(position) + "." + fileExtensionList.get(position));
            if (oldCurrentFile.exists()) {
                oldCurrentFile.renameTo(new File(vaultPathList.get(position)));
                Log.d("VaultImage", "Renamed file " + (position));
            }
            if ((position - 1) > -1) {
                File oldFile = new File(vaultPathList.get(position - 1) + "." + fileExtensionList.get(position - 1));
                if (oldFile.exists()) {
                    oldFile.renameTo(new File(vaultPathList.get(position - 1)));
                    Log.d("VaultImage", "Renamed file " + (position - 1));
                }
            }
            if ((position + 1) < (vaultPathList.size())) {
                File oldFile = new File(vaultPathList.get(position + 1) + "." + fileExtensionList.get(position + 1));
                if (oldFile.exists()) {
                    oldFile.renameTo(new File(vaultPathList.get(position + 1)));
                    Log.d("VaultImage", "Renamed file " + (position + 1));
                }
            }
        }
    }

    void closeAdapter(){
        if(activity !=null) {
            activity = null;
        }
    }
}

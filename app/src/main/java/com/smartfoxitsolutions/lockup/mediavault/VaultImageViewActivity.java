package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.ImageViewDeleteDialog;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.ImageViewUnlockDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by RAAJA on 14-11-2016.
 */

public class VaultImageViewActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    static final String IMAGE_VIEW_STATE = "image_view_state";

    private ViewPager imageViewPager;
    private RelativeLayout topBar,bottomBar;
    private  AppCompatImageButton backButton, deleteButton, unlockButton;
    TextView originalFileName;
    private VaultImageViewPagerAdapter imageAdapter;
    ImageViewState viewState;
    private ValueAnimator displayTopBarAnim, hideTopBarAnim, displayBottomBarAnim,hideBottomBarAnim;
    private AnimatorSet displayBarAnimSet,hideBarAnimSet;
    static LinkedList<String> originalFileNameList, vaultFileList,vaultIdList, fileExtensionList;
    boolean isTopBottomVisible,isDeletePressed, isUnlockPressed;
    private static boolean isConfigChanged;
    private String imageBucketId;
    private boolean shouldCloseAffinity;
    private ImageViewScreenOffReceiver imageViewScreenOffReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_image_view_activity);
        imageViewPager = (ViewPager) findViewById(R.id.vault_image_view_activity_pager);
        topBar = (RelativeLayout) findViewById(R.id.vault_image_view_activity_top_bar);
        bottomBar = (RelativeLayout) findViewById(R.id.vault_image_view_activity_bottom_bar);
        backButton = (AppCompatImageButton) findViewById(R.id.vault_image_view_activity_back_button);
        deleteButton = (AppCompatImageButton) findViewById(R.id.vault_image_view_activity_delete_button);
        unlockButton = (AppCompatImageButton) findViewById(R.id.vault_image_view_activity_unlock);
        originalFileName = (TextView) findViewById(R.id.vault_image_view_activity_original_name);
        imageBucketId = getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
        if(savedInstanceState!=null){
            viewState = (ImageViewState) savedInstanceState.getSerializable(IMAGE_VIEW_STATE);
        }
        setListeners();
        setAnimators();
    }

    static void setOriginalFileNameList(LinkedList<String> originalNameList){
        originalFileNameList = new LinkedList<>();
        originalFileNameList.addAll(originalNameList);
    }

    static void setVaultFileList(LinkedList<String> vaultFilePathList){
        vaultFileList = new LinkedList<>();
        vaultFileList.addAll(vaultFilePathList);
    }

    static void setVaultIdList(LinkedList<String> vaultFileIdList){
        vaultIdList = new LinkedList<>();
        vaultIdList.addAll(vaultFileIdList);
    }

    static void setFileExtensionList(LinkedList<String> extensionList){
        fileExtensionList = new LinkedList<>();
        fileExtensionList.addAll(extensionList);
    }

    private WeakReference<VaultImageViewActivity> getWeakReference(){
        return new WeakReference<>(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        int currentItem = getIntent().getIntExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,0);
        if(imageAdapter == null) {
            imageAdapter = new VaultImageViewPagerAdapter(this);
            imageViewPager.setAdapter(imageAdapter);
            imageViewPager.setCurrentItem(currentItem);
            imageViewPager.addOnPageChangeListener(this);
        }
        if(viewState != null){
            imageAdapter.setImageViewState(viewState);
            imageAdapter.notifyDataSetChanged();
        }

        imageViewScreenOffReceiver = new ImageViewScreenOffReceiver(getWeakReference());
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(imageViewScreenOffReceiver,filter);
    }

    void setListeners(){
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDeletePressed) {
                    isDeletePressed=true;
                    DialogFragment deleteDialog = new ImageViewDeleteDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack("delete_image_dialog");
                    deleteDialog.show(fragmentTransaction, "delete_image_dialog");
                }
            }
        });

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isUnlockPressed){
                    isUnlockPressed = true;
                    DialogFragment unlockDialog = new ImageViewUnlockDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack("unlock_image_dialog");
                    unlockDialog.show(fragmentTransaction, "unlock_image_dialog");
                }
            }
        });
    }

    void setAnimators(){
        displayTopBarAnim = new ValueAnimator();
        displayTopBarAnim.setDuration(150).setInterpolator(new AccelerateDecelerateInterpolator());
        displayTopBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                topBar.setBottom(value);
                Log.d("VaultImage","Top Bar values " + value);

            }
        });

        hideTopBarAnim = new ValueAnimator();
        hideTopBarAnim.setDuration(150).setInterpolator(new AccelerateDecelerateInterpolator());
        hideTopBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                topBar.setBottom(value);
            }
        });

        displayBottomBarAnim = new ValueAnimator();
        displayBottomBarAnim.setDuration(150).setInterpolator(new AccelerateDecelerateInterpolator());
        displayBottomBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                bottomBar.setTop(value);
                Log.d("VaultImage","Bottom Bar values " + value);
            }
        });

        hideBottomBarAnim = new ValueAnimator();
        hideBottomBarAnim.setDuration(150).setInterpolator(new AccelerateDecelerateInterpolator());
        hideBottomBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                bottomBar.setTop(value);
                Log.d("VaultImage","Bottom Bar Hide values " + value);
            }
        });

        displayBarAnimSet = new AnimatorSet();
        displayBarAnimSet.playTogether(displayTopBarAnim,displayBottomBarAnim);
        displayBarAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isTopBottomVisible = true;
                topBar.setVisibility(View.VISIBLE);
                bottomBar.setVisibility(View.VISIBLE);
            }
        });


        hideBarAnimSet = new AnimatorSet();
        hideBarAnimSet.playTogether(hideTopBarAnim,hideBottomBarAnim);
        hideBarAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isTopBottomVisible = false;
                topBar.setVisibility(View.INVISIBLE);
                bottomBar.setVisibility(View.INVISIBLE);
            }
        });

    }
    void showTopBottomBars(){
        if(hideBarAnimSet.isStarted()){
            hideBarAnimSet.removeAllListeners();
            hideBarAnimSet.cancel();
        }
        displayBarAnimSet.start();
    }

    void hideTopBottomBars(){
       if(displayBarAnimSet.isStarted()){
            displayBarAnimSet.removeAllListeners();
            displayBarAnimSet.cancel();
        }
        hideBarAnimSet.start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            int topBarBottomPosition = topBar.getBottom();
            int topBarTopPosition = topBar.getTop();
            int bottomBarBottomPosition = bottomBar.getBottom();
            int bottomBarTopPosition = bottomBar.getTop();
            displayTopBarAnim.setIntValues(topBarTopPosition,topBarBottomPosition);
            hideTopBarAnim.setIntValues(topBarBottomPosition,topBarTopPosition);
            displayBottomBarAnim.setIntValues(bottomBarBottomPosition,bottomBarTopPosition);
            hideBottomBarAnim.setIntValues(bottomBarTopPosition,bottomBarBottomPosition);
        }
    }

    public void deleteImage(){
        ArrayList<String> selectedId = new ArrayList<>();
        selectedId.add(vaultIdList.get(imageViewPager.getCurrentItem()));
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(selectedId);
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,imageBucketId)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA)
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT));
        finishAffinity();
    }

    public void unlockImage(){
        ArrayList<String> selectedId = new ArrayList<>();
        selectedId.add(vaultIdList.get(imageViewPager.getCurrentItem()));
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(selectedId);
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,imageBucketId)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA)
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT));
        finishAffinity();
    }

    public void deleteImageCancelled(){
        isDeletePressed = false;
    }

    public void unlockImageCancelled(){
        isUnlockPressed = false;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if(isTopBottomVisible){
            isTopBottomVisible = false;
            hideTopBottomBars();
        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isConfigChanged = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
            if(viewState!=null){
                outState.putSerializable(IMAGE_VIEW_STATE,viewState);
            }
            isConfigChanged = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        shouldCloseAffinity = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(imageAdapter!=null){
            imageAdapter.renameCurrentFile(imageViewPager.getCurrentItem());
        }
        if(shouldCloseAffinity){
            if(imageAdapter!=null){
                imageAdapter.closeAdapter();
                imageViewPager.removeOnPageChangeListener(this);
            }
            originalFileNameList.clear();
            vaultFileList.clear();
            vaultIdList.clear();
            fileExtensionList.clear();
            originalFileNameList = null;
            vaultFileList = null;
            vaultIdList = null;
            fileExtensionList = null;
            finishActivityAffinity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!shouldCloseAffinity && !isConfigChanged){
             if(imageAdapter!=null){
                 imageAdapter.closeAdapter();
                 imageViewPager.removeOnPageChangeListener(this);
            }
            originalFileNameList.clear();
            vaultFileList.clear();
            vaultIdList.clear();
            fileExtensionList.clear();
            originalFileNameList = null;
            vaultFileList = null;
            vaultIdList = null;
            fileExtensionList = null;
        }
        unregisterReceiver(imageViewScreenOffReceiver);
    }

    private void finishActivityAffinity(){
        Glide.get(this).clearMemory();
        new Thread(new ClearImageViewCacheTask(getWeakReference())).start();
        finishAffinity();
    }

    static class ClearImageViewCacheTask implements Runnable
    {
        WeakReference<VaultImageViewActivity> activityWeakReference;
        ClearImageViewCacheTask(WeakReference<VaultImageViewActivity> activityReference){
            this.activityWeakReference = activityReference;
        }

        @Override
        public void run() {
            Log.d("CacheVault","Clear Image View Cache Started "+ System.currentTimeMillis());
            Glide.get(activityWeakReference.get()).clearDiskCache();
            Log.d("CacheVault","Clear Image View Cache Complete "+ + System.currentTimeMillis());
        }
    }

    static class ImageViewScreenOffReceiver extends BroadcastReceiver {

        WeakReference<VaultImageViewActivity> activity;
        ImageViewScreenOffReceiver(WeakReference<VaultImageViewActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishActivityAffinity();
            }
        }
    }
}

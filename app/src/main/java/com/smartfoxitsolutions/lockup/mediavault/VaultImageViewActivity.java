package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 14-11-2016.
 */

public class VaultImageViewActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    static final String IMAGE_VIEW_STATE = "image_view_state";

    ViewPager imageViewPager;
    RelativeLayout topBar,bottomBar;
    AppCompatImageButton backButton, deleteButton, unlockButton;
    TextView originalFileName;
    VaultImageViewPagerAdapter imageAdapter;
    ImageViewState viewState;
    ValueAnimator displayTopBarAnim, hideTopBarAnim, displayBottomBarAnim,hideBottomBarAnim;
    AnimatorSet displayBarAnimSet,hideBarAnimSet;
    boolean isTopBottomVisible;

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
        if(savedInstanceState!=null){
            viewState = (ImageViewState) savedInstanceState.getSerializable(IMAGE_VIEW_STATE);
        }
        setAnimators();
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(viewState != null){
            imageAdapter.setImageViewState(viewState);
            imageAdapter.notifyDataSetChanged();
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
            if(viewState!=null){
                outState.putSerializable(IMAGE_VIEW_STATE,viewState);
            }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(imageAdapter!=null){
            imageAdapter.renameCurrentFile(imageViewPager.getCurrentItem());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(imageAdapter!=null){
            imageAdapter.closeAdapter();
            imageViewPager.removeOnPageChangeListener(this);
        }
        HiddenFileContentModel.getMediaOriginalName().clear();
        HiddenFileContentModel.getMediaVaultFile().clear();
        HiddenFileContentModel.getMediaExtension().clear();
    }
}

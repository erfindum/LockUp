package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 14-11-2016.
 */

public class VaultImageViewActivity extends AppCompatActivity {

    static final String IMAGE_VIEW_STATE = "image_view_state";

    ViewPager imageViewPager;
    RelativeLayout topBar;
    RelativeLayout bottomBar;
    AppCompatImageButton backButton, deleteButton, unlockButton;
    TextView originalFileName;
    VaultImageViewPagerAdapter imageAdapter;
    ImageViewState viewState;
    ValueAnimator displayAnimator, hideAnimator;
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
        displayAnimator = ValueAnimator.ofInt(0,1);
        displayAnimator.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        displayAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isTopBottomVisible = true;
            }
        });
        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                topBar.setBottom(value);
                bottomBar.setTop(value);
            }
        });

        hideAnimator = ValueAnimator.ofInt(1,0);
        hideAnimator.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        hideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isTopBottomVisible = false;
                topBar.setVisibility(View.INVISIBLE);
                bottomBar.setVisibility(View.INVISIBLE);
            }
        });
        hideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                topBar.setBottom(value);
                bottomBar.setTop(value);
            }
        });
    }
    void showTopBottomBars(){
        topBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        displayAnimator.start();
    }

    void hideTopBottomBars(){
        hideAnimator.start();
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
        }
    }
}

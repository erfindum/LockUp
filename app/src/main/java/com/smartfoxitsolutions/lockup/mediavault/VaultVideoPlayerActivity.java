package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.smartfoxitsolutions.lockup.R;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by RAAJA on 16-11-2016.
 */

public class VaultVideoPlayerActivity extends AppCompatActivity{

    static final String CURRENT_VIDEO_FILE_POSITION = "current_video_file_position";
    static final String CURRENT_VIDEO_SEEK_PROGRESS = "current_video_seek_progress";

    RelativeLayout topBar;
    RelativeLayout bottomBar;
    AppCompatImageButton backButton, playPauseButton, playPreviousButton,playNextButton,deleteButton, unlockButton;
    LinkedList<String> originalFileNameList, vaultFileList, fileExtensionList;
    SeekBar videoSeekBar;
    TextView titleText, durationText;
    VideoView videoView;
    Handler uiHandler;
    int currentPosition, currentFileSeekProgress;
    Runnable seekBarTask;
    ValueAnimator displayTopBarAnim, displayBottomBarAnim, hideTopBarAnim, hideBottomBarAnim;
    AnimatorSet displayTopBottomSet, hideTopBottomSet;
    boolean isTopBottomVisible, isOrientationChange, isUserSeeking, isVideoViewStopped, isAnimationRunning;
    AtomicInteger mediaControlDurationCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_video_player_activity);
        topBar = (RelativeLayout) findViewById(R.id.vault_video_player_activity_top_bar);
        bottomBar = (RelativeLayout) findViewById(R.id.vault_video_player_activity_bottom_bar);
        backButton = (AppCompatImageButton) findViewById(R.id.vault_video_player_activity_back_button);
        playPauseButton = (AppCompatImageButton) findViewById(R.id.vault_video_player_activity_play_button);
        playPreviousButton = (AppCompatImageButton) findViewById(R.id.vault_video_player_activity_previous_button);
        playNextButton = (AppCompatImageButton) findViewById(R.id.vault_video_player_activity_next_button);
        deleteButton = (AppCompatImageButton) findViewById(R.id.vault_video_player_activity_delete_button);
        unlockButton = (AppCompatImageButton) findViewById(R.id.vault_video_player_activity_unlock);
        videoSeekBar = (SeekBar) findViewById(R.id.vault_video_player_activity_seek_bar);
        titleText = (TextView) findViewById(R.id.vault_video_player_activity_original_name);
        durationText = (TextView) findViewById(R.id.vault_video_player_activity_duration_text);
        videoView = (VideoView) findViewById(R.id.vault_video_player_activity_player);
        originalFileNameList = HiddenFileContentModel.getMediaOriginalName();
        vaultFileList = HiddenFileContentModel.getMediaVaultFile();
        fileExtensionList = HiddenFileContentModel.getMediaExtension();
        mediaControlDurationCount = new AtomicInteger(0);
        uiHandler = new Handler(getMainLooper());
        if(savedInstanceState == null){
            currentPosition = getIntent().getIntExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,0);
        }
        setRunnable();
        setListeners();
        setAnimations();
    }

    void setRunnable(){
        seekBarTask = new Runnable() {
            @Override
            public void run() {
                if(videoSeekBar.getVisibility()==View.VISIBLE){
                    videoSeekBar.setProgress(videoView.getCurrentPosition());
                    int duration = videoView.getCurrentPosition();
                    int seconds = (duration/ 1000) % 60 ;
                    int minutes = ((duration/ (1000*60)) % 60);
                    int hours   = ((duration/ (1000*60*60)));
                    String secondsString = ""+seconds;
                    String minutesString = ""+minutes;
                    String hoursString = ""+hours;
                    if(hours<10){
                        hoursString = "0"+hoursString;
                    }
                    if(minutes<10){
                        minutesString = "0"+minutesString;
                    }
                    if(seconds<10){
                        secondsString = "0"+secondsString;
                    }
                    if(hours==0) {
                        durationText.setText(minutesString + ":" + secondsString);
                    }
                    if(hours>0){
                        durationText.setText(hoursString + ":" + minutesString + ":" + secondsString);
                    }
                    if(isTopBottomVisible && !isUserSeeking){
                        mediaControlDurationCount.set(mediaControlDurationCount.incrementAndGet());
                        if(mediaControlDurationCount.get()>3 && !isAnimationRunning){
                            isTopBottomVisible = false;
                            isAnimationRunning = true;
                            if(displayTopBottomSet.isStarted()){
                                displayTopBottomSet.end();
                                displayTopBottomSet.cancel();
                            }
                            hideTopBottomBars();
                            mediaControlDurationCount.set(0);
                        }
                    }
                }
                uiHandler.postDelayed(this,1000);
            }
        };
    }

    void setListeners(){
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isVideoViewStopped){
                    videoView.setVideoPath(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition));
                    isVideoViewStopped = false;
                    return;
                }
                if(videoView.isPlaying() && videoView.canPause()){
                    videoView.pause();
                    playPauseButton.setImageResource(R.drawable.ic_video_play);
                }else if(!isVideoViewStopped){
                    videoView.seekTo(videoSeekBar.getProgress());
                    playPauseButton.setImageResource(R.drawable.ic_video_pause);
                }
            }
        });

        playPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPosition>0){
                    File previousFile = new File(vaultFileList.get(currentPosition-1));
                    if(previousFile.exists()){
                        previousFile.renameTo(new File(vaultFileList.get(currentPosition-1)+"."
                                +fileExtensionList.get(currentPosition-1)));
                    }
                    videoView.setVideoPath(vaultFileList.get(currentPosition-1)+"."
                            +fileExtensionList.get(currentPosition-1));
                    File currentFile = new File(vaultFileList.get(currentPosition)+"."
                            +fileExtensionList.get(currentPosition));
                    if(currentFile.exists()){
                        currentFile.renameTo(new File(vaultFileList.get(currentPosition)));
                    }
                    titleText.setText(originalFileNameList.get(currentPosition-1));
                    currentPosition-=1;
                }
            }
        });

        playNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPosition<vaultFileList.size()-1){
                    File nextFile = new File(vaultFileList.get(currentPosition+1));
                    if(nextFile.exists()){
                        nextFile.renameTo(new File(vaultFileList.get(currentPosition+1)+"."
                                +fileExtensionList.get(currentPosition+1)));
                    }
                    videoView.setVideoPath(vaultFileList.get(currentPosition+1)+"."
                            +fileExtensionList.get(currentPosition+1));
                    File currentFile = new File(vaultFileList.get(currentPosition)+"."
                            +fileExtensionList.get(currentPosition));
                    if(currentFile.exists()){
                        currentFile.renameTo(new File(vaultFileList.get(currentPosition)));
                    }
                    titleText.setText(originalFileNameList.get(currentPosition+1));
                    currentPosition+=1;
                }
            }
        });

        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if(fromUser) {
                        seekBar.setProgress(progress);
                        int seconds = (progress/ 1000) % 60 ;
                        int minutes = ((progress/ (1000*60)) % 60);
                        int hours   = ((progress/ (1000*60*60)));
                        String secondsString = ""+seconds;
                        String minutesString = ""+minutes;
                        String hoursString = ""+hours;
                        if(hours<10){
                            hoursString = "0"+hoursString;
                        }
                        if(minutes<10){
                            minutesString = "0"+minutesString;
                        }
                        if(seconds<10){
                            secondsString = "0"+secondsString;
                        }
                        if(hours==0) {
                            durationText.setText(minutesString + ":" + secondsString);
                        }
                        if(hours>0){
                            durationText.setText(hoursString + ":" + minutesString + ":" + secondsString);
                        }
                        videoView.seekTo(progress);
                        if(playPauseButton.getVisibility()==View.VISIBLE){
                            playPauseButton.setImageResource(R.drawable.ic_video_pause);
                        }
                    }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
                if(videoView.isPlaying()) {
                    videoView.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking=false;
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoSeekBar.setMax(videoView.getDuration());
                if(!videoView.isPlaying()){
                    if(isOrientationChange){
                        videoSeekBar.setProgress(currentFileSeekProgress);
                        videoView.seekTo(currentFileSeekProgress);
                        isOrientationChange = false;
                    }
                    videoView.start();
                    playPauseButton.setImageResource(R.drawable.ic_video_pause);
                    uiHandler.post(seekBarTask);
                    mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            videoView.start();
                        }
                    });
                }
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.stopPlayback();
                if(playPauseButton.getVisibility() == View.VISIBLE){
                    playPauseButton.setImageResource(R.drawable.ic_video_play);
                }
                isVideoViewStopped = true;
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()){
                    case MotionEvent.ACTION_UP:
                        if(isTopBottomVisible){
                            isTopBottomVisible = false;
                            isAnimationRunning = true;
                            if(displayTopBottomSet.isStarted()){
                                displayTopBottomSet.end();
                                displayTopBottomSet.cancel();
                            }
                            hideTopBottomBars();
                        }else{
                            isTopBottomVisible = true;
                            isAnimationRunning = true;
                            if(hideTopBottomSet.isStarted()){
                                hideTopBottomSet.end();
                                hideTopBottomSet.cancel();
                            }
                            displayTopBottomBars();
                        }
                }
                return true;
            }
        });
    }

    void setAnimations(){
        displayTopBarAnim = new ValueAnimator();
        displayTopBarAnim.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        displayTopBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(topBar.getVisibility() == View.INVISIBLE){
                    topBar.setVisibility(View.VISIBLE);
                }
                int value = (int) animation.getAnimatedValue();
                topBar.setBottom(value);
            }
        });

        displayBottomBarAnim = new ValueAnimator();
        displayBottomBarAnim.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        displayBottomBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(bottomBar.getVisibility() == View.INVISIBLE){
                    bottomBar.setVisibility(View.VISIBLE);
                }
                int value = (int) animation.getAnimatedValue();
                bottomBar.setTop(value);
            }
        });

        hideTopBarAnim = new ValueAnimator();
        hideTopBarAnim.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        hideTopBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                topBar.setBottom(value);
            }
        });

        hideBottomBarAnim = new ValueAnimator();
        hideBottomBarAnim.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        hideBottomBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                bottomBar.setTop(value);
            }
        });

        displayTopBottomSet = new AnimatorSet();
        displayTopBottomSet.playTogether(displayTopBarAnim,displayBottomBarAnim);
        displayTopBottomSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mediaControlDurationCount.set(0);
                if(playPauseButton.getVisibility()==View.VISIBLE){
                    if(videoView.isPlaying()){
                        playPauseButton.setImageResource(R.drawable.ic_video_pause);
                    }else{
                        playPauseButton.setImageResource(R.drawable.ic_video_play);
                    }
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimationRunning = false;
            }
        });

       hideTopBottomSet = new AnimatorSet();
        hideTopBottomSet.playTogether(hideTopBarAnim,hideBottomBarAnim);
        hideTopBottomSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationStart(animation);
                isAnimationRunning = false;
                topBar.setVisibility(View.INVISIBLE);
                bottomBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    void displayTopBottomBars(){
        displayTopBottomSet.start();
    }

    void hideTopBottomBars(){

        hideTopBottomSet.start();
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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState!=null){
            isOrientationChange=true;
            currentPosition= savedInstanceState.getInt(CURRENT_VIDEO_FILE_POSITION);
            currentFileSeekProgress = savedInstanceState.getInt(CURRENT_VIDEO_SEEK_PROGRESS,0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        File currentFile = new File(vaultFileList.get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition)));
        }
        videoView.setVideoPath(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition));
        titleText.setText(originalFileNameList.get(currentPosition));
    }

    @Override
    protected void onPause() {
        super.onPause();
        File currentFile = new File(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(vaultFileList.get(currentPosition)));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_VIDEO_FILE_POSITION,currentPosition);
        outState.putInt(CURRENT_VIDEO_SEEK_PROGRESS,videoSeekBar.getProgress());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HiddenFileContentModel.getMediaOriginalName().clear();
        HiddenFileContentModel.getMediaVaultFile().clear();
        HiddenFileContentModel.getMediaExtension().clear();
    }
}

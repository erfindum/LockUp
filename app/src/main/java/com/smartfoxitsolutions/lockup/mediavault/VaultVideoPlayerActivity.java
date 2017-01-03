package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.VideoPlayerDeleteDialog;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.VideoPlayerUnlockDialog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by RAAJA on 16-11-2016.
 */

public class VaultVideoPlayerActivity extends AppCompatActivity{

    private RelativeLayout topBar;
    private RelativeLayout bottomBar;
    private AppCompatImageButton backButton, playPauseButton, playPreviousButton,playNextButton,deleteButton, unlockButton;
    private static LinkedList<String> originalFileNameList, vaultFileList, fileExtensionList, vaultIdList;
    private SeekBar videoSeekBar;
    private TextView titleText, durationText;
    private VideoView videoView;
    private Handler uiHandler;
    private static int currentPosition = -1, currentSeekProgressState;
    private int currentAudioProgress;
    private Runnable seekBarTask;
    private ValueAnimator displayTopBarAnim, displayBottomBarAnim, hideTopBarAnim, hideBottomBarAnim;
    private AnimatorSet displayTopBottomSet, hideTopBottomSet;
    boolean isTopBottomVisible, isOrientationChange, isUserSeeking, isVideoViewStopped, isAnimationRunning
            ,isDeletePressed, isUnlockPressed;
    private AtomicInteger mediaControlDurationCount;
    private String videoBucketId;
    private boolean shouldCloseAffinity;
    private VideoPlayerScreenOffReceiver videoPlayerScreenOffReceiver;

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
        mediaControlDurationCount = new AtomicInteger(0);
        videoBucketId = getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
        uiHandler = new Handler(getMainLooper());
        if(currentPosition == -1){
            currentPosition = getIntent().getIntExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,0);
        }
        setRunnable();
        setListeners();
        setAnimations();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.d("VaultVideo","Called onCreate");
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

    void setRunnable(){
        seekBarTask = new Runnable() {
            @Override
            public void run() {
                if(videoSeekBar.getVisibility()==View.VISIBLE){
                    int progress = videoView.getCurrentPosition();
                    videoSeekBar.setProgress(progress);
                    setDurationText(progress);
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
                Log.d("VaultVideo","SeekTask Running");
            }
        };
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
                    DialogFragment deleteDialog = new VideoPlayerDeleteDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack("delete_video_dialog");
                    deleteDialog.show(fragmentTransaction, "delete_video_dialog");
                    videoView.pause();
                }
            }
        });

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isUnlockPressed){
                    isUnlockPressed = true;
                    DialogFragment unlockDialog = new VideoPlayerUnlockDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack("unlock_video_dialog");
                    unlockDialog.show(fragmentTransaction, "unlock_video_dialog");
                    videoView.pause();
                }
            }
        });

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
                        setDurationText(progress);
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
                if(!videoView.isPlaying()){
                    uiHandler.removeCallbacks(seekBarTask);
                    videoView.start();
                    videoSeekBar.setMax(videoView.getDuration());
                    videoSeekBar.setProgress(currentAudioProgress);
                    videoView.seekTo(currentAudioProgress);
                    currentAudioProgress = 0;
                    playPauseButton.setImageResource(R.drawable.ic_video_pause);
                    uiHandler.post(seekBarTask);
                    isOrientationChange = false;
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
        Log.d("VaultVideo",mediaControlDurationCount.get() + " media control duration count");
        mediaControlDurationCount.set(0);
        displayTopBottomSet.start();
    }

    void hideTopBottomBars(){
        hideTopBottomSet.start();
    }

    void setDurationText(int currentProgress){
        int seconds = (currentProgress/ 1000) % 60 ;
        int minutes = ((currentProgress/ (1000*60)) % 60);
        int hours   = ((currentProgress/ (1000*60*60)));
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
        Log.d("VaultVideo","Called Window Focus Change");
    }

    public void deleteVideo(){
        ArrayList<String> selectedId = new ArrayList<>();
        selectedId.add(vaultIdList.get(currentPosition));
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(selectedId);
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,videoBucketId)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA)
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT));
        finishAffinity();
    }

    public void unlockVideo(){
        ArrayList<String> selectedId = new ArrayList<>();
        selectedId.add(vaultIdList.get(currentPosition));
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(selectedId);
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,videoBucketId)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA)
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT));
        finishAffinity();

    }

    public void deleteVideoCancelled(){
        isDeletePressed = false;
        videoView.seekTo(videoSeekBar.getProgress());
    }

    public void unlockVideoCancelled(){
        isUnlockPressed = false;
        videoView.seekTo(videoSeekBar.getProgress());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isOrientationChange = false;
        Log.d("VaultVideo","Called onRestore");
    }

    @Override
    protected void onStart() {
        super.onStart();
        File currentFile = new File(vaultFileList.get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition)));
        }
        currentAudioProgress = currentSeekProgressState;
        videoView.setVideoPath(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition));
        titleText.setText(originalFileNameList.get(currentPosition));
        videoPlayerScreenOffReceiver = new VideoPlayerScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(videoPlayerScreenOffReceiver,filter);
        Log.d("VaultVideo","Called onResume");
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        shouldCloseAffinity = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        File currentFile = new File(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(vaultFileList.get(currentPosition)));
        }
        uiHandler.removeCallbacks(seekBarTask);
        videoView.stopPlayback();
        if(shouldCloseAffinity){
            currentPosition = -1;
            currentSeekProgressState = 0;
            vaultFileList.clear();
            originalFileNameList.clear();
            vaultIdList.clear();
            fileExtensionList.clear();
            vaultFileList = null;
            originalFileNameList = null;
            vaultIdList = null;
            fileExtensionList = null;
            finishAffinity();
        }
        Log.d("VaultVideo","Called onPause");
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isOrientationChange = true;
        currentSeekProgressState = videoView.getCurrentPosition();
        Log.d("VaultVideo","Called onSave");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!shouldCloseAffinity && !isOrientationChange) {
            currentPosition = -1;
            currentSeekProgressState = 0;
            vaultFileList.clear();
            originalFileNameList.clear();
            vaultIdList.clear();
            fileExtensionList.clear();
            vaultFileList = null;
            originalFileNameList = null;
            vaultIdList = null;
            fileExtensionList = null;
            Log.d("VaultVideo","Called Destroy Clear");
        }
        unregisterReceiver(videoPlayerScreenOffReceiver);
        Log.d("VaultVideo","Called onDestroy");
    }

    static class VideoPlayerScreenOffReceiver extends BroadcastReceiver {

        WeakReference<VaultVideoPlayerActivity> activity;
        VideoPlayerScreenOffReceiver(WeakReference<VaultVideoPlayerActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishAffinity();
            }
        }
    }
}

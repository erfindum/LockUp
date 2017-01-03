package com.smartfoxitsolutions.lockup.mediavault;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.AudioPlayerDeleteDialog;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.AudioPlayerUnlockDialog;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by RAAJA on 17-11-2016.
 */

public class VaultAudioPlayerActivity extends AppCompatActivity {

    private RelativeLayout parentView;
    private AppCompatImageView albumArtView;
    private AppCompatImageButton backButton, playPauseButton, playPreviousButton,playNextButton,deleteButton, unlockButton;
    private SeekBar audioSeekBar;
    private TextView titleText, durationText;
    private static LinkedList<String> originalFileNameList, vaultFileList, fileExtensionList, vaultIdList;
    private MediaPlayer audioPlayer;
    private Handler uiHandler;
    private Runnable audioSeekTask;
    private  static int currentPosition = -1,currentAudioProgress;
    private int imageViewWidth,imageViewHeight;
    private Drawable placeholder;
    boolean isAlbumArtLoaded,isAudioStopped,isConfigChanged;
    private String audioBucketId;
    private boolean isDeletePressed,isUnlockPressed, shouldCloseAffinity;
    private AudioPlayerScreenOffReceiver audioPlayerScreenOffReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_audio_player_activity);
        parentView = (RelativeLayout) findViewById(R.id.vault_audio_player_activity_parent);
        albumArtView = (AppCompatImageView) findViewById(R.id.vault_audio_player_activity_album_image_view);
        backButton = (AppCompatImageButton) findViewById(R.id.vault_audio_player_activity_back_button);
        playPauseButton = (AppCompatImageButton) findViewById(R.id.vault_audio_player_activity_play_button);
        playPreviousButton = (AppCompatImageButton) findViewById(R.id.vault_audio_player_activity_previous_button);
        playNextButton = (AppCompatImageButton) findViewById(R.id.vault_audio_player_activity_next_button);
        deleteButton = (AppCompatImageButton) findViewById(R.id.vault_audio_player_activity_delete_button);
        unlockButton = (AppCompatImageButton) findViewById(R.id.vault_audio_player_activity_unlock);
        audioSeekBar = (SeekBar) findViewById(R.id.vault_audio_player_activity_seek_bar);
        titleText = (TextView) findViewById(R.id.vault_audio_player_activity_original_name);
        durationText = (TextView) findViewById(R.id.vault_audio_player_activity_duration_text);
        if(currentPosition == -1) {
            currentPosition = getIntent().getIntExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY, 0);
        }
        uiHandler = new Handler(getMainLooper());
        audioBucketId = getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
        setActivityBackground();
        setRunnable();
        loadPlaceHolder();
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

    void setActivityBackground(){
        GradientDrawable drawable = new GradientDrawable();
        int[] colors = {Color.parseColor("#448AFF"), Color.parseColor("#1565C0")};
        drawable.setColors(colors);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float radius = Math.round(metrics.heightPixels*.95);
        drawable.setGradientRadius(radius);
        drawable.setGradientCenter(0.5f,1f);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setShape(GradientDrawable.RECTANGLE);
        parentView.setBackground(drawable);
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
                    DialogFragment deleteDialog = new AudioPlayerDeleteDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack("delete_audio_dialog");
                    deleteDialog.show(fragmentTransaction, "delete_audio_dialog");
                    audioPlayer.pause();
                }
            }
        });

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isUnlockPressed){
                    isUnlockPressed = true;
                    DialogFragment unlockDialog = new AudioPlayerUnlockDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack("unlock_audio_dialog");
                    unlockDialog.show(fragmentTransaction, "unlock_audio_dialog");
                    audioPlayer.pause();
                }
            }
        });

        audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                    uiHandler.removeCallbacks(audioSeekTask);
                    audioPlayer.start();
                    audioSeekBar.setMax(audioPlayer.getDuration());
                    audioPlayer.seekTo(currentAudioProgress);
                    audioSeekBar.setProgress(currentAudioProgress);
                    currentAudioProgress=0;
                    playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);
                    uiHandler.post(audioSeekTask);
            }
        });

        audioPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                audioPlayer.start();
            }
        });

        audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                audioPlayer.pause();
                isAudioStopped = true;
                uiHandler.removeCallbacks(audioSeekTask);
                setDuration(0);
                audioSeekBar.setProgress(0);
                playPauseButton.setImageResource(R.drawable.selector_audio_vault_play);
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAudioStopped){
                   audioPlayer.seekTo(audioSeekBar.getProgress());
                   uiHandler.post(audioSeekTask);
                   playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);
                   isAudioStopped=false;
                    return;
                }
                if(audioPlayer.isPlaying()){
                    audioPlayer.pause();
                    playPauseButton.setImageResource(R.drawable.selector_audio_vault_play);

                }else{
                    audioPlayer.seekTo(audioPlayer.getCurrentPosition());
                    playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);
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
                    try {
                        audioPlayer.reset();
                        audioPlayer.setDataSource(vaultFileList.get(currentPosition-1) + "." + fileExtensionList.get(currentPosition-1));
                        audioPlayer.prepare();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    File currentFile = new File(vaultFileList.get(currentPosition)+"."
                            +fileExtensionList.get(currentPosition));
                    if(currentFile.exists()){
                        currentFile.renameTo(new File(vaultFileList.get(currentPosition)));
                    }
                    currentPosition-=1;
                    setTitleText(currentPosition);
                    loadAlbumArt(currentPosition);
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
                    try {
                        audioPlayer.reset();
                        audioPlayer.setDataSource(vaultFileList.get(currentPosition+1) + "." + fileExtensionList.get(currentPosition+1));
                        audioPlayer.prepare();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    File currentFile = new File(vaultFileList.get(currentPosition)+"."
                            +fileExtensionList.get(currentPosition));
                    if(currentFile.exists()){
                        currentFile.renameTo(new File(vaultFileList.get(currentPosition)));
                    }
                    currentPosition+=1;
                    setTitleText(currentPosition);
                    loadAlbumArt(currentPosition);
                }
            }
        });

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    seekBar.setProgress(progress);
                    setDuration(progress);
                    playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);
                    audioPlayer.seekTo(progress);
                    if(isAudioStopped){
                        uiHandler.post(audioSeekTask);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    void loadPlaceHolder(){
        placeholder = ContextCompat.getDrawable(this, R.drawable.ic_audio_player_placeholder);
    }

    void setRunnable(){
        audioSeekTask = new Runnable() {
            @Override
            public void run() {
                    int currentDuration =0;
                        currentDuration = audioPlayer.getCurrentPosition();
                        audioSeekBar.setProgress(currentDuration);
                        setDuration(currentDuration);
                        uiHandler.postDelayed(this,1000);
            }
        };
    }

    public void deleteAudio(){
        ArrayList<String> selectedId = new ArrayList<>();
        selectedId.add(vaultIdList.get(currentPosition));
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(selectedId);
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,audioBucketId)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA)
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT));
        finishAffinity();
    }

    public void unlockAudio(){
        ArrayList<String> selectedId = new ArrayList<>();
        selectedId.add(vaultIdList.get(currentPosition));
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(selectedId);
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,audioBucketId)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA)
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT));
        finishAffinity();
    }

    public void deleteAudioCancelled(){
        isDeletePressed = false;
        audioPlayer.seekTo(audioSeekBar.getProgress());
    }

    public void unlockAudioCancelled(){
        isUnlockPressed = false;
        audioPlayer.seekTo(audioSeekBar.getProgress());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
            isConfigChanged = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        File currentFile = new File(vaultFileList.get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(vaultFileList.get(currentPosition)
                    +"."+fileExtensionList.get(currentPosition)));
        }
        audioPlayer = new MediaPlayer();
        setListeners();
        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            audioPlayer.reset();
            audioPlayer.setDataSource(vaultFileList.get(currentPosition) + "." + fileExtensionList.get(currentPosition));
            audioPlayer.prepareAsync();
        }catch (IOException e){
            e.printStackTrace();
        }catch (IllegalStateException f){
            f.printStackTrace();
        }
        setTitleText(currentPosition);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        audioPlayerScreenOffReceiver = new AudioPlayerScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(audioPlayerScreenOffReceiver,filter);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            if(!isAlbumArtLoaded) {
                isAlbumArtLoaded = true;
                imageViewHeight = albumArtView.getHeight();
                imageViewWidth = albumArtView.getWidth();
                loadAlbumArt(currentPosition);
            }
        }
    }

    void loadAlbumArt(int position){
        Uri uri = Uri.fromFile(new File(vaultFileList.get(position)
                +"."+fileExtensionList.get(position)));
        Glide.with(this).load(new AlbumArtModel(uri,this.getBaseContext()))
                .placeholder(placeholder).error(placeholder).centerCrop().crossFade().skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE).override(imageViewWidth,imageViewHeight)
                .into(albumArtView);
    }

    void setTitleText(int position){
        titleText.setText(originalFileNameList.get(position));
    }

    void setDuration(int duration){
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
        uiHandler.removeCallbacks(audioSeekTask);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        audioPlayer.stop();
        audioPlayer.release();
        audioPlayer = null;
        if(shouldCloseAffinity){
            vaultFileList.clear();
            originalFileNameList.clear();
            vaultIdList.clear();
            fileExtensionList.clear();
            vaultIdList = null;
            fileExtensionList = null;
            originalFileNameList = null;
            vaultFileList = null;
            finishAffinity();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isConfigChanged = true;
        currentAudioProgress = audioPlayer.getCurrentPosition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!shouldCloseAffinity && !isConfigChanged) {
            currentPosition = -1;
            vaultFileList.clear();
            originalFileNameList.clear();
            vaultIdList.clear();
            fileExtensionList.clear();
            vaultIdList = null;
            fileExtensionList = null;
            originalFileNameList = null;
            vaultFileList = null;
        }
        unregisterReceiver(audioPlayerScreenOffReceiver);
    }

    static class AudioPlayerScreenOffReceiver extends BroadcastReceiver {

        WeakReference<VaultAudioPlayerActivity> activity;
        AudioPlayerScreenOffReceiver(WeakReference<VaultAudioPlayerActivity> activity){
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

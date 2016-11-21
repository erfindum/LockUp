package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.services.AudioPlayerService;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 17-11-2016.
 */

public class VaultAudioPlayerActivity extends AppCompatActivity {

    public static final int PLAY_BUTTON_PRESSED = 23;
    public static final int PREVIOUS_TRACK_PRESSED = 24;
    public static final int NEXT_TRACK_PRESSED = 25;
    public static final int SEEK_BAR_POSITION_CHANGED = 26;
    public static final int NEW_ALBUM_SET = 27;

    public static final String CURRENT_AUDIO_POSITION = "current_audio_position";
    public static final String SHOULD_STOP_AUDIO_PLAYER = "should_stop_audio_player";


    RelativeLayout parentView;
    AppCompatImageView albumArtView;
    AppCompatImageButton backButton, playPauseButton, playPreviousButton,playNextButton,deleteButton, unlockButton;
    SeekBar audioSeekBar;
    TextView titleText, durationText;
    int currentPosition, imageViewHeight, imageViewWidth;
    static Messenger audioServiceMessenger, audioActivityMessenger;
    Drawable placeholder;
    boolean isAlbumArtLoaded;

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
        currentPosition = getIntent().getIntExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,0);
        setActivityBackground();
        setListeners();
        loadPlaceHolder();
    }

    public static void setAudioServiceMessenger(Messenger serviceMessenger){
        audioServiceMessenger = serviceMessenger;
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
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioServiceMessenger!=null) {
                 try {
                     Message msg = Message.obtain();
                     msg.what = PLAY_BUTTON_PRESSED;
                     audioServiceMessenger.send(msg);
                 }catch (RemoteException e){
                     e.printStackTrace();
                 }
                }
            }
        });

        playPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioServiceMessenger!=null) {
                    try {
                        Message msg = Message.obtain();
                        msg.what = PREVIOUS_TRACK_PRESSED;
                        audioServiceMessenger.send(msg);
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        playNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioServiceMessenger!=null) {
                    try {
                        Message msg = Message.obtain();
                        msg.what = NEXT_TRACK_PRESSED;
                        audioServiceMessenger.send(msg);
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                    if (audioServiceMessenger != null) {
                        try {
                            Message msg = Message.obtain();
                            msg.what = SEEK_BAR_POSITION_CHANGED;
                            msg.arg1 = progress;
                            audioServiceMessenger.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);
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

    @Override
    protected void onStart() {
        super.onStart();
        if(HiddenFileContentModel.getMediaVaultFile().isEmpty()){
            finish();
        }
        audioActivityMessenger = new Messenger(new AudioActivityMessenger(new WeakReference<>(this)));
        AudioPlayerService.setAudioActivityMessenger(audioActivityMessenger);
        File currentFile = new File(HiddenFileContentModel.getMediaVaultFile().get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(HiddenFileContentModel.getMediaVaultFile().get(currentPosition)
                    +"."+HiddenFileContentModel.getMediaExtension().get(currentPosition)));
        }
        if(HiddenFileContentModel.getIsAudioAlbumChanged()) {
            if(AudioPlayerService.isAudioServiceStarted){
                if (audioServiceMessenger != null) {
                    try {
                        Message msg = Message.obtain();
                        msg.what = NEW_ALBUM_SET;
                        audioServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                startAudioPlayer();
            }
        }
    }

    void startAudioPlayer(){
        startService(new Intent(this.getBaseContext(), AudioPlayerService.class)
                .putExtra(CURRENT_AUDIO_POSITION, currentPosition)
                .putExtra(SHOULD_STOP_AUDIO_PLAYER,false));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            if(!isAlbumArtLoaded) {
                isAlbumArtLoaded = true;
                imageViewHeight = albumArtView.getHeight();
                imageViewWidth = albumArtView.getWidth();
                loadAlbumArt();
            }
        }
    }

    void loadAlbumArt(){
        Uri uri = Uri.fromFile(new File(HiddenFileContentModel.getMediaVaultFile().get(currentPosition)
                +"."+HiddenFileContentModel.getMediaExtension().get(currentPosition)));
        Glide.with(this).load(new AlbumArtModel(uri,this.getBaseContext()))
                .placeholder(placeholder).error(placeholder).centerCrop().crossFade().skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE).override(imageViewWidth,imageViewHeight)
                .into(albumArtView);
    }

    void setTitleText(int currentPosition){
        titleText.setText(HiddenFileContentModel.getMediaOriginalName().get(currentPosition));
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

    void finishActivity(){
        finish();
    }

    static class AudioActivityMessenger extends Handler{

        WeakReference<VaultAudioPlayerActivity> activity;

        AudioActivityMessenger(WeakReference<VaultAudioPlayerActivity> activity){
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == AudioPlayerService.AUDIO_PLAYED_STATE){
                    activity.get().currentPosition = msg.arg1;
                    activity.get().audioSeekBar.setMax(msg.arg2);
                    activity.get().setTitleText(msg.arg1);
                    activity.get().setDuration(msg.arg2);
                    activity.get().loadAlbumArt();
                activity.get().playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);

            }
            if(msg.what == AudioPlayerService.AUDIO_STOPPED_STATE){
                    activity.get().playPauseButton.setImageResource(R.drawable.selector_audio_vault_play);
            }
            if(msg.what == AudioPlayerService.PLAY_BUTTON_PLAY_STATE){
                activity.get().playPauseButton.setImageResource(R.drawable.selector_audio_vault_pause);
            }
            if(msg.what == AudioPlayerService.PLAY_BUTTON_PAUSE_STATE){
                activity.get().playPauseButton.setImageResource(R.drawable.selector_audio_vault_play);
            }
            if(msg.what == AudioPlayerService.PREVIOUS_NEXT_TRACK_PRESSED){
                activity.get().currentPosition = msg.arg1;
            }
            if(msg.what == AudioPlayerService.CURRENT_SEEK_PROGRESS){
                activity.get().audioSeekBar.setProgress(msg.arg1);
                activity.get().setDuration(msg.arg1);
            }
            if(msg.what == AudioPlayerService.OLD_FILE_RENAMED){
                activity.get().startAudioPlayer();
            }
            if(msg.what == AudioPlayerService.AUDIO_SERVICE_STOPPED){
                activity.get().finish();
            }

        }
    }
}

package com.smartfoxitsolutions.lockup.mediavault.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.HiddenFileContentModel;
import com.smartfoxitsolutions.lockup.mediavault.VaultAudioPlayerActivity;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

/**
 * Created by RAAJA on 17-11-2016.
 */

public class AudioPlayerService extends Service {

    public static final int PLAY_BUTTON_PLAY_STATE= 38;
    public static final int PLAY_BUTTON_PAUSE_STATE= 39;
    public static final int PREVIOUS_NEXT_TRACK_PRESSED = 40;
    public static final int AUDIO_PLAYED_STATE = 41;
    public static final int AUDIO_STOPPED_STATE= 42;
    public static final int CURRENT_SEEK_PROGRESS = 43;
    public static final int OLD_FILE_RENAMED = 44;
    public static final int AUDIO_SERVICE_STOPPED = 45;

    LinkedList<String> originalFileNameList, vaultFileList, fileExtensionList;
    MediaPlayer audioPlayer;
    Handler uiHandler;
    int currentPosition, currentProgress;
    Runnable currentProgressTask;
    boolean isAudioStopped;
    static Messenger audioServiceMessenger, audioActivityMessenger;
    public static boolean isAudioServiceStarted;
    String currentFileName, currentFileExtension;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioPlayer = new MediaPlayer();
        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        uiHandler = new Handler(getMainLooper());
        originalFileNameList = HiddenFileContentModel.getMediaOriginalName();
        vaultFileList = HiddenFileContentModel.getMediaVaultFile();
        fileExtensionList = HiddenFileContentModel.getMediaExtension();
        setRunnable();
        setListeners();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       super.onStartCommand(intent, flags, startId);
        isAudioServiceStarted = true;
       if(intent.getBooleanExtra(VaultAudioPlayerActivity.SHOULD_STOP_AUDIO_PLAYER,false)){
           Message mssg = Message.obtain();
           mssg.what = AUDIO_SERVICE_STOPPED;
           try {
               if(audioActivityMessenger!=null) {
                   audioActivityMessenger.send(mssg);
               }
           }catch (RemoteException e){
               e.printStackTrace();
           }
            stopForeground(true);
            stopSelf();
        }
        currentPosition = intent.getIntExtra(VaultAudioPlayerActivity.CURRENT_AUDIO_POSITION,0);
        HiddenFileContentModel.setIsAudioAlbumChanged(false);
        File currentFile = new File(vaultFileList.get(currentPosition));
        if(currentFile.exists()){
            currentFile.renameTo(new File(vaultFileList.get(currentPosition)+"."+fileExtensionList.get(currentPosition)));
        }
        currentFileName = vaultFileList.get(currentPosition);
        currentFileExtension = fileExtensionList.get(currentPosition);
        try {
            audioPlayer.reset();
            audioPlayer.setDataSource(vaultFileList.get(currentPosition) + "." + fileExtensionList.get(currentPosition));
            audioPlayer.prepare();
        }catch (IOException e){
            e.printStackTrace();
        }
        audioServiceMessenger = new Messenger(new AudioServiceHandler(new WeakReference<>(this)));
        VaultAudioPlayerActivity.setAudioServiceMessenger(audioServiceMessenger);
        startForeground(273459,getPlayerNotification());
        return Service.START_NOT_STICKY;
    }

    public static void setAudioActivityMessenger(Messenger activityMessenger){
        audioActivityMessenger = activityMessenger;
    }

    Notification getPlayerNotification(){
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
        notifBuilder.setContentTitle("Audio is being Played")
                .setContentText("Touch to stop audio player")
                .setOngoing(true)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getService(getBaseContext()
                        ,234,new Intent(getBaseContext(),AudioPlayerService.class)
                        .putExtra(VaultAudioPlayerActivity.SHOULD_STOP_AUDIO_PLAYER,true),PendingIntent.FLAG_ONE_SHOT))
                .setColor(Color.parseColor("#ffffff"));
        return notifBuilder.build();
    }

    void setRunnable(){
        currentProgressTask = new Runnable() {
            @Override
            public void run() {
                try {
                    if (audioPlayer.isPlaying()) {
                        currentProgress = audioPlayer.getCurrentPosition();
                        if (audioActivityMessenger != null && audioActivityMessenger.getBinder().isBinderAlive()) {
                            try {
                                Message msg = Message.obtain();
                                msg.what = CURRENT_SEEK_PROGRESS;
                                msg.arg1 = currentProgress;
                                audioActivityMessenger.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }catch (IllegalStateException e){
                    e.printStackTrace();
                }
                uiHandler.postDelayed(this,1000);
            }
        };

    }

    void setListeners(){
        audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if(!audioPlayer.isPlaying()) {
                    audioPlayer.start();
                    if(audioActivityMessenger!=null && audioActivityMessenger.getBinder().isBinderAlive()){
                        try{
                            Message msg = Message.obtain();
                            msg.what = AUDIO_PLAYED_STATE;
                            msg.arg1 = currentPosition;
                            msg.arg2 = audioPlayer.getDuration();
                            audioActivityMessenger.send(msg);
                        }catch (RemoteException e){
                            e.printStackTrace();
                        }
                    }
                    uiHandler.post(currentProgressTask);
                }
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
                audioPlayer.stop();
                isAudioStopped = true;
                if(audioActivityMessenger!=null && audioActivityMessenger.getBinder().isBinderAlive()){
                    try{
                        Message msg = Message.obtain();
                        msg.what = AUDIO_STOPPED_STATE;
                        audioActivityMessenger.send(msg);
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    void playButtonPressed(){
        if(isAudioStopped){
            try {
                audioPlayer.reset();
                audioPlayer.setDataSource(vaultFileList.get(currentPosition) + "." + fileExtensionList.get(currentPosition));
                audioPlayer.prepare();
            }catch (IOException e){
                e.printStackTrace();
            }
            isAudioStopped=false;
            return;
        }
        if(audioPlayer.isPlaying()){
            audioPlayer.pause();
            if(audioActivityMessenger!=null && audioActivityMessenger.getBinder().isBinderAlive()){
                try{
                    Message msg = Message.obtain();
                    msg.what = PLAY_BUTTON_PAUSE_STATE;
                    audioActivityMessenger.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }

        }else if(!isAudioStopped){
            audioPlayer.seekTo(currentProgress);
            if(audioActivityMessenger!=null && audioActivityMessenger.getBinder().isBinderAlive()){
                try{
                    Message msg = Message.obtain();
                    msg.what = PLAY_BUTTON_PLAY_STATE;
                    msg.arg1 = audioPlayer.getCurrentPosition();
                    audioActivityMessenger.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        }
    }

    void previousTrackPressed(){
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
            currentFileName = vaultFileList.get(currentPosition);
            currentFileExtension = fileExtensionList.get(currentPosition);
            if(audioActivityMessenger!=null && audioActivityMessenger.getBinder().isBinderAlive()){
                try{
                    Message msg = Message.obtain();
                    msg.what = PREVIOUS_NEXT_TRACK_PRESSED;
                    msg.arg1 = currentPosition;
                    audioActivityMessenger.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        }

    }

    void nextTrackPressed(){
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
            currentFileName = vaultFileList.get(currentPosition);
            currentFileExtension = fileExtensionList.get(currentPosition);
            if(audioActivityMessenger!=null && audioActivityMessenger.getBinder().isBinderAlive()){
                try{
                    Message msg = Message.obtain();
                    msg.what = PREVIOUS_NEXT_TRACK_PRESSED;
                    msg.arg1 = currentPosition;
                    audioActivityMessenger.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        }

    }

    static class AudioServiceHandler extends Handler{
        WeakReference<AudioPlayerService> service;

        AudioServiceHandler(WeakReference<AudioPlayerService> service){
            this.service = service;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == VaultAudioPlayerActivity.PLAY_BUTTON_PRESSED){
                service.get().playButtonPressed();
            }
            if(msg.what == VaultAudioPlayerActivity.PREVIOUS_TRACK_PRESSED){
                service.get().previousTrackPressed();
            }
            if(msg.what == VaultAudioPlayerActivity.NEXT_TRACK_PRESSED){
                service.get().nextTrackPressed();
            }
            if(msg.what == VaultAudioPlayerActivity.SEEK_BAR_POSITION_CHANGED){
                service.get().audioPlayer.seekTo(msg.arg1);
            }
            if(msg.what == VaultAudioPlayerActivity.NEW_ALBUM_SET){
                service.get().audioPlayer.stop();
                service.get().renameCurrentFile();
                try {
                    Message mssg = Message.obtain();
                    mssg.what = OLD_FILE_RENAMED;
                    audioActivityMessenger.send(mssg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        renameCurrentFile();
        stopForeground(true);
        stopSelf();
    }

    void renameCurrentFile(){
        File currentFile = new File(currentFileName+"."+currentFileExtension);
        if(currentFile.exists()){
            currentFile.renameTo(new File(currentFileName));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        renameCurrentFile();
        audioPlayer.stop();
        audioPlayer.release();
        isAudioServiceStarted = false;
        HiddenFileContentModel.getMediaOriginalName().clear();
        HiddenFileContentModel.getMediaVaultFile().clear();
        HiddenFileContentModel.getMediaExtension().clear();
    }
}

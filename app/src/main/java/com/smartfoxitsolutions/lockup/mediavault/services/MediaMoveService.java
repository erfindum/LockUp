package com.smartfoxitsolutions.lockup.mediavault.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveService extends Service implements Handler.Callback{

    public static boolean SERVICE_STARTED = false;

    public static final int MEDIA_SUCCESSFULLY_MOVED = 4;
    public static final int MEDIA_MOVE_COMPLETED = 5;


    int moveType, mediaSelectionType;
    String albumBucketId, mediaType;
    String[] selectedMediaId,fileNames;
    ExecutorService mediaMoveService;
    MediaMoveInTask moveInTask;
    MediaMoveOutTask moveOutTask;
    MediaDeleteTask mediaDeleteTask;
    NotificationCompat.Builder notifBuilder;
    NotificationManager notifManager;
    static Messenger activityMessenger;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaMoveService = Executors.newFixedThreadPool(1);
        notifManager = (NotificationManager) getBaseContext().getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
            moveType = intent.getIntExtra(MediaMoveActivity.VAULT_TYPE_KEY,2);
            setMoveInfo(intent);
            startMediaMoveTask();
        SERVICE_STARTED = true;
        return START_REDELIVER_INTENT;
    }

    void setMoveInfo(Intent intent){
            mediaSelectionType = intent.getIntExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE,2);
            albumBucketId = intent.getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
            mediaType = intent.getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
            Messenger messenger = intent.getParcelableExtra(MediaMoveActivity.MEDIA_MOVE_MESSENGER_KEY);
            updateMessenger(messenger);
            if(mediaSelectionType==MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE) {
                selectedMediaId = intent.getStringArrayExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY);
            }
            if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
                fileNames = intent.getStringArrayExtra(MediaMoveActivity.MEDIA_FILE_NAMES_KEY);
            }
    }

    public static void updateMessenger(Messenger messenger){
        activityMessenger = messenger;
    }

    void startMediaMoveTask(){
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            startForeground(3542124, getNotifBuilder().build());
            moveInTask = new MediaMoveInTask(getBaseContext(), this);
            moveInTask.setTaskRequirements(mediaSelectionType, albumBucketId, mediaType, selectedMediaId, fileNames);
            mediaMoveService.submit(moveInTask);


        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
            startForeground(6584923, getNotifBuilder().build());
            moveOutTask = new MediaMoveOutTask(getBaseContext(), this);
            moveOutTask.setTaskRequirements(mediaSelectionType,albumBucketId,mediaType,selectedMediaId);
            mediaMoveService.submit(moveOutTask);

        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
            startForeground(5421359, getNotifBuilder().build());
            mediaDeleteTask = new MediaDeleteTask(getBaseContext(), this);
            mediaDeleteTask.setTaskRequirements(mediaSelectionType,albumBucketId,mediaType,selectedMediaId);
            mediaMoveService.submit(mediaDeleteTask);

        }
    }

    Notification getForegroundNotification(){
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
        notifBuilder.setContentTitle("Media is being moved");
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            notifBuilder.setContentTitle("Moving files into vault");
        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
            notifBuilder.setContentTitle("Moving files out of vault");
        }
        if (moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
            notifBuilder.setContentTitle("Deleting vault media files");
        }
                notifBuilder.setOngoing(true)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#ffffff"));
        return notifBuilder.build();
    }

    NotificationCompat.Builder getNotifBuilder(){
        notifBuilder = new NotificationCompat.Builder(getBaseContext());
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_in_text))
            .setProgress(fileNames.length,0,false);
        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
            notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_in_text))
            .setProgress(0,0,false);
        }
        if (moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
            notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_delete_files))
            .setProgress(0,0,false);
        }
                notifBuilder.setContentText("0 / 0")
                .setOngoing(true)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(getBaseContext(),4,new Intent(getBaseContext(), MediaMoveActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        ,0))
                .setColor(Color.parseColor("#ffffff"));
        return notifBuilder;
    }


    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == MEDIA_SUCCESSFULLY_MOVED){
            int moveCount = msg.arg1;
            int totalCount = msg.arg2;
            String notifString = moveCount + " / "+totalCount;
            notifBuilder.setContentText(notifString);
            notifBuilder.setProgress(totalCount,moveCount,false);
            if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT){
                notifManager.notify(3542124,notifBuilder.build());
            }
            if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
                notifManager.notify(6584923,notifBuilder.build());
            }
            if(moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
                notifManager.notify(5421359,notifBuilder.build());
            }
            if(activityMessenger!=null && activityMessenger.getBinder().isBinderAlive()){
                Message mssg = Message.obtain();
                mssg.what = msg.what;
                mssg.arg1 = moveCount;
                mssg.arg2 = totalCount;
                try {
                    activityMessenger.send(mssg);
                }
                catch (RemoteException e){
                    e.printStackTrace();
                }
            }
            return true;
        }
        if(msg.what == MEDIA_MOVE_COMPLETED){
            if(activityMessenger!=null && activityMessenger.getBinder().isBinderAlive()){
                Message mssg = Message.obtain();
                mssg.what = msg.what;
                try {
                    activityMessenger.send(mssg);
                }
                catch (RemoteException e){
                    e.printStackTrace();
                }
            }
            closeService();
            return true;
        }

        return false;
    }

    void closeService(){
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!mediaMoveService.isShutdown()){
            mediaMoveService.shutdown();
            if(moveInTask!=null) {
                moveInTask.closeTask();
            }
            if(moveOutTask!=null){
                moveOutTask.closeTask();
            }
            if(mediaDeleteTask!=null){
                mediaDeleteTask.closeTask();
            }
        }
        activityMessenger = null;
        SERVICE_STARTED = false;
    }
}

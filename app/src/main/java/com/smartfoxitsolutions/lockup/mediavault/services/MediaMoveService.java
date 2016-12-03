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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultAlbumActivity;
import com.smartfoxitsolutions.lockup.services.AppLockForegroundService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveService extends Service implements Handler.Callback{

    public static boolean SERVICE_STARTED = false;

    public static final int MEDIA_SUCCESSFULLY_MOVED = 4;
    public static final int MEDIA_MOVE_COMPLETED = 5;
    private static String MEDIA_TYPE = MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA;


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
        startForeground(25648751,getForegroundNotification());
        startService(new Intent(getBaseContext(), AppLockForegroundService.class)
                        .putExtra(AppLockForegroundService.FOREGROUND_SERVICE_TYPE
                                ,AppLockForegroundService.MEDIA_MOVE_SERVICE));
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
            updateMoveMessenger(messenger);
            if(mediaSelectionType==MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE) {
                selectedMediaId = intent.getStringArrayExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY);
            }
            if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
                fileNames = intent.getStringArrayExtra(MediaMoveActivity.MEDIA_FILE_NAMES_KEY);
            }
    }

    public static void updateMoveMessenger(Messenger messenger){
        activityMessenger = messenger;
    }

    void startMediaMoveTask(){
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            moveInTask = new MediaMoveInTask(getBaseContext(), this);
            moveInTask.setTaskRequirements(mediaSelectionType, albumBucketId, mediaType, selectedMediaId, fileNames);
            mediaMoveService.submit(moveInTask);
        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
            moveOutTask = new MediaMoveOutTask(getBaseContext(), this);
            moveOutTask.setTaskRequirements(mediaSelectionType,albumBucketId,mediaType,selectedMediaId);
            mediaMoveService.submit(moveOutTask);

        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
            mediaDeleteTask = new MediaDeleteTask(getBaseContext(), this);
            mediaDeleteTask.setTaskRequirements(mediaSelectionType,albumBucketId,mediaType,selectedMediaId);
            mediaMoveService.submit(mediaDeleteTask);
        }
        notifManager.notify(3542124, getNotifBuilder().build());
    }

    Notification getForegroundNotification(){
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
        notifBuilder.setContentTitle("LockUp Vault");
        notifBuilder.setContentText("Moving Media");
                notifBuilder.setOngoing(true)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#2874F0"));
        return notifBuilder.build();
    }

    NotificationCompat.Builder getNotifBuilder(){
        notifBuilder = new NotificationCompat.Builder(getBaseContext());
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_in_text))
            .setProgress(fileNames.length,0,false);
        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
            notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_out_text))
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
                .setColor(Color.parseColor("#2874F0"));
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
            notifManager.notify(3542124,notifBuilder.build());

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
                mssg.obj = mediaType;
                try {
                    activityMessenger.send(mssg);
                }
                catch (RemoteException e){
                    e.printStackTrace();
                }
            }
            notifManager.cancel(3542124);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
            if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
                builder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_complete));
            }
            if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
                builder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_complete));
            }
            if (moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
                builder.setContentTitle(getResources().getString(R.string.vault_move_activity_delete_complete));
            }
            MEDIA_TYPE = mediaType;
            builder.setContentText(getResources().getString(R.string.vault_move_activity_vault_redirect_message))
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(PendingIntent.getActivity(getBaseContext(),29,new Intent(getBaseContext()
                                    , MediaVaultAlbumActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, MEDIA_TYPE)
                            ,PendingIntent.FLAG_UPDATE_CURRENT))
                    .setOnlyAlertOnce(true)
                    .setColor(Color.parseColor("#2874F0"));
            notifManager.notify(7549682, builder.build());
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

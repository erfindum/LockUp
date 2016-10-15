package com.smartfoxitsolutions.lockup.mediavault;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.smartfoxitsolutions.lockup.LockUpMainActivity;
import com.smartfoxitsolutions.lockup.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveService extends Service implements Handler.Callback{

    public static boolean SERVICE_STARTED = false;

    public static final int MEDIA_SUCCESSFULLY_MOVED = 4;
    public static final int MEDIA_MOVE_COMPLETED = 5;


    int moveType, mediaSelectionType, serviceStartType;
    String albumBucketId, mediaType;
    String[] selectedMediaId,fileNames;
    ExecutorService mediaMoveService;
    MediaMoveInTask moveTask;
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
        serviceStartType= intent.getIntExtra(MediaMoveActivity.SERVICE_START_TYPE_KEY,0);

            moveType = intent.getIntExtra(MediaMoveActivity.VAULT_TYPE_KEY,2);
            mediaSelectionType = intent.getIntExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE,2);
            albumBucketId = intent.getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
            mediaType = intent.getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
            Messenger messenger = intent.getParcelableExtra(MediaMoveActivity.MEDIA_MOVE_MESSENGER_KEY);
            updateMessenger(messenger);
            if(mediaSelectionType==MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE) {
                selectedMediaId = intent.getStringArrayExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY);
            }
            fileNames = intent.getStringArrayExtra(MediaMoveActivity.MEDIA_FILE_NAMES_KEY);
            startMediaMoveTask();
        SERVICE_STARTED = true;
        return START_REDELIVER_INTENT;
    }

    static void updateMessenger(Messenger messenger){
        activityMessenger = messenger;
    }

    void startMediaMoveTask(){
        try {
            startForeground(3542124, getNotifBuilder().build());
            if (moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
                moveTask = new MediaMoveInTask(getBaseContext(), this);
                moveTask.setTaskRequirements(mediaSelectionType, albumBucketId, mediaType, selectedMediaId, fileNames);
                mediaMoveService.submit(moveTask);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    NotificationCompat.Builder getNotifBuilder(){
        notifBuilder = new NotificationCompat.Builder(getBaseContext())
            .setContentTitle(getResources().getString(R.string.vault_move_activity_move_in_text))
            .setContentText("0 / 0")
            .setOngoing(true)
            .setAutoCancel(false)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
                .setProgress(fileNames.length,0,false)
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
            notifBuilder.setProgress(fileNames.length,moveCount,false);
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
            moveTask.closeTask();
        }
        activityMessenger = null;
        SERVICE_STARTED = false;
    }
}

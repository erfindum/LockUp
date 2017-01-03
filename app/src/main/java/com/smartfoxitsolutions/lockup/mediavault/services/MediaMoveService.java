package com.smartfoxitsolutions.lockup.mediavault.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.smartfoxitsolutions.lockup.MainLockActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultAlbumActivity;
import com.smartfoxitsolutions.lockup.mediavault.SelectedMediaModel;
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

    int moveType;
    String albumBucketId, mediaType;
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
            setMoveInfo(intent);
            startMediaMoveTask();
        SERVICE_STARTED = true;
        return START_REDELIVER_INTENT;
    }

    void setMoveInfo(Intent intent){
        moveType = intent.getIntExtra(MediaMoveActivity.VAULT_TYPE_KEY,2);
        albumBucketId = intent.getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
            mediaType = intent.getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
            Messenger messenger = intent.getParcelableExtra(MediaMoveActivity.MEDIA_MOVE_MESSENGER_KEY);
            updateMoveMessenger(messenger);
    }

    public static void updateMoveMessenger(Messenger messenger){
        activityMessenger = messenger;
    }

    void startMediaMoveTask(){
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            moveInTask = new MediaMoveInTask(getBaseContext(), this);
            moveInTask.setTaskRequirements(albumBucketId, mediaType);
            mediaMoveService.submit(moveInTask);
        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT){
            moveOutTask = new MediaMoveOutTask(getBaseContext(), this);
            moveOutTask.setTaskRequirements(albumBucketId,mediaType);
            mediaMoveService.submit(moveOutTask);

        }
        if(moveType == MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
            mediaDeleteTask = new MediaDeleteTask(getBaseContext(), this);
            mediaDeleteTask.setTaskRequirements(albumBucketId,mediaType);
            mediaMoveService.submit(mediaDeleteTask);
        }
        notifManager.notify(3542124, getNotifBuilder().build());
    }

    Bitmap getLauncherIcon(){
        return BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
    }

    Notification getForegroundNotification(){
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getBaseContext());
        notifBuilder.setContentTitle("LockUp Vault");
        notifBuilder.setContentText("Moving Media");
                notifBuilder.setOngoing(true)
                .setAutoCancel(false)
                .setLargeIcon(getLauncherIcon())
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#2874F0"));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            notifBuilder.setSmallIcon(R.drawable.ic_notification_small);
        }else{
            notifBuilder.setSmallIcon(R.mipmap.ic_launcher);
        }
        return notifBuilder.build();
    }

    NotificationCompat.Builder getNotifBuilder(){
        notifBuilder = new NotificationCompat.Builder(getBaseContext());
        if(moveType == MediaMoveActivity.MOVE_TYPE_INTO_VAULT) {
            notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_in_text))
            .setProgress(SelectedMediaModel.getInstance().getSelectedMediaFileNameList().size(),0,false);
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
                .setLargeIcon(getLauncherIcon())
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#2874F0"));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            notifBuilder.setSmallIcon(R.drawable.ic_notification_small);
        }else{
            notifBuilder.setSmallIcon(R.mipmap.ic_launcher);
        }
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
                    .setLargeIcon(getLauncherIcon())
                    .setContentIntent(PendingIntent.getActivity(getBaseContext(),29,new Intent(getBaseContext()
                                    , MainLockActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            ,PendingIntent.FLAG_UPDATE_CURRENT))
                    .setOnlyAlertOnce(true)
                    .setColor(Color.parseColor("#2874F0"));
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                builder.setSmallIcon(R.drawable.ic_notification_small);
            }else{
                builder.setSmallIcon(R.mipmap.ic_launcher);
            }
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

package com.smartfoxitsolutions.lockup.mediavault.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
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
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.services.AppLockForegroundService;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 02-12-2016.
 */

public class ShareMoveService extends Service implements Handler.Callback{

    public static final String TYPE_IMAGE_MEDIA = "image";
    public static final String TYPE_VIDEO_MEDIA = "video";
    public static final String TYPE_AUDIO_MEDIA = "audio";

    public static boolean SERVICE_STARTED = false;

    ArrayList<Uri> fileUriList;
    ExecutorService shareMoveService;
    ShareMoveTask shareMoveTask;
    NotificationCompat.Builder notifBuilder;
    NotificationManager notifManager;
    int itemWidth, viewHeight, viewWidth;
    static Messenger activityMessenger;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        shareMoveService = Executors.newFixedThreadPool(1);
        notifManager = (NotificationManager) getBaseContext().getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(215879634,getForegroundNotification());
        startService(new Intent(getBaseContext(), AppLockForegroundService.class)
                .putExtra(AppLockForegroundService.FOREGROUND_SERVICE_TYPE
                        ,AppLockForegroundService.SHARE_MOVE_SERVICE));
        fileUriList = intent.getParcelableArrayListExtra(MediaMoveActivity.SHARE_MEDIA_FILE_LIST_KEY);
        Messenger messenger = intent.getParcelableExtra(MediaMoveActivity.MEDIA_MOVE_MESSENGER_KEY);
        updateShareMessenger(messenger);
        startMediaMoveTask();
        SERVICE_STARTED = true;
        return START_REDELIVER_INTENT;
    }

    public static void updateShareMessenger(Messenger messenger){
        activityMessenger = messenger;
    }

    private void measureThumbnailSize(){
            viewWidth = Math.round(getResources().getDimension(R.dimen.vault_album_thumbnail_width));
            viewHeight = Math.round(getResources().getDimension(R.dimen.vault_album_thumbnail_height));
            itemWidth = Math.round(getResources().getDimension(R.dimen.vault_album_item_size));
    }

    private void startMediaMoveTask(){
        measureThumbnailSize();
        shareMoveTask = new ShareMoveTask(getBaseContext(),this);
        shareMoveTask.setTaskRequirements(fileUriList,viewWidth,viewHeight);
        shareMoveService.submit(shareMoveTask);
        notifManager.notify(541895, getNotifBuilder().build());
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
        notifBuilder.setContentTitle(getResources().getString(R.string.vault_move_activity_move_in_text))
                .setProgress(fileUriList.size(),0,false);
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
        if(msg.what == MediaMoveService.MEDIA_SUCCESSFULLY_MOVED){
            int moveCount = msg.arg1;
            int totalCount = msg.arg2;
            String notifString = moveCount + " / "+totalCount;
            notifBuilder.setContentText(notifString);
            notifBuilder.setProgress(totalCount,moveCount,false);
            notifManager.notify(541895,notifBuilder.build());

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
        if(msg.what == MediaMoveService.MEDIA_MOVE_COMPLETED){
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
            postCompleteNotification(getResources().getString(R.string.vault_move_activity_move_complete));
            closeService();
            return true;
        }

        if(msg.what == MediaMoveService.MOVE_INSUFFICIENT_SPACE){
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
            postCompleteNotification(getResources().getString(R.string.vault_move_activity_insufficient_space));
            closeService();
            return true;
        }

        return false;
    }

    void postCompleteNotification(String message){
        notifManager.cancel(541895);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
        builder.setContentTitle(message)
                .setContentText(getResources().getString(R.string.vault_move_activity_vault_redirect_message))
                .setOngoing(false)
                .setAutoCancel(true)
                .setLargeIcon(getLauncherIcon())
                .setContentIntent(PendingIntent.getActivity(getBaseContext(),29,new Intent(getBaseContext()
                                , MainLockActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        ,0))
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#2874F0"));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            builder.setSmallIcon(R.drawable.ic_notification_small);
        }else{
            builder.setSmallIcon(R.mipmap.ic_launcher);
        }
        notifManager.notify(7549682, builder.build());
    }

    void closeService(){
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!shareMoveService.isShutdown()){
            shareMoveService.shutdown();
            if(shareMoveTask!=null) {
                shareMoveTask.closeTask();
            }
        }
        activityMessenger = null;
        SERVICE_STARTED = false;
    }
}

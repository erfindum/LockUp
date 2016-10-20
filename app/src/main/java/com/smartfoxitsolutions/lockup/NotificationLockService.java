package com.smartfoxitsolutions.lockup;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by RAAJA on 08-09-2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationLockService extends NotificationListenerService implements TickerHider.OnTickerHiderActiveListener
                                                            ,Handler.Callback{
    static final int ADD_TICKER_VIEW = 4;
    static  final int REMOVE_TICKER_VIEW = 5;
    static final int UPDATE_TICKER_VIEW = 6;
    static final String UPDATE_LOCK_PACKAGES = "update_lock_packages";

    Gson gson;
    private Type notificationAppsMapToken;
    private HashMap<Integer,Integer> notificationIdMap;
    private TreeMap<String,String> notificationAppsMap;
    public static boolean isNotificationServiceConnected = false;
    private static boolean isUpdateReceiverEnabled = false;
    private NotificationManager notificationManager;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private TickerHider tickerHider;
    private Handler uiHandler;
    private UpdateLockPackagesReceiver lockPackageReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        isNotificationServiceConnected = true;
        Log.d("NotificationLock","Called Connected");
        lockPackageReceiver = new UpdateLockPackagesReceiver(getLockReference());
        if(!isUpdateReceiverEnabled){
            IntentFilter filter = new IntentFilter(UPDATE_LOCK_PACKAGES);
            LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(lockPackageReceiver,filter);
            isUpdateReceiverEnabled = true;
        }
        updateNotificationAppMap();
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isNotificationServiceConnected = false;
        Log.d("NotificationLock","Listener Connected");
        if(isUpdateReceiverEnabled){
           LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(lockPackageReceiver);
            isUpdateReceiverEnabled = false;
        }
        lockPackageReceiver = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        isNotificationServiceConnected = true;
        Log.d("NotificationLock","Listener Connected");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        notificationAppsMap = new TreeMap<>();
        notificationIdMap = new HashMap<>();
        notificationAppsMapToken = new TypeToken<TreeMap<String,String>>(){}.getType();
        notificationManager = (NotificationManager) getBaseContext().getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP) {
            windowManager = (WindowManager) getBaseContext().getSystemService(WINDOW_SERVICE);
            uiHandler = new Handler(Looper.getMainLooper(), this);
            setWindowParams();
        }
    }

    WeakReference<NotificationLockService> getLockReference(){
        return new WeakReference<>(this);
    }

    void updateNotificationAppMap(){
        String notifAppMapString = getBaseContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE)
                                    .getString(AppLockModel.NOTIFICATION_CHECKED_APPS_SHARED_PREF_KEY,null);
        if(notifAppMapString!=null){
            notificationAppsMap = gson.fromJson(notifAppMapString,notificationAppsMapToken);
        }
    }

    private void setWindowParams() {
        windowParams = new WindowManager.LayoutParams();
        windowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        windowParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.gravity = Gravity.TOP;
    }

    private WindowManager.LayoutParams getWindowParams(){
        return this.windowParams;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
            String packageName = sbn.getPackageName();
        if(isNotificationServiceConnected) {
            if (notificationAppsMap.containsKey(packageName) && sbn.isClearable()
                    && !sbn.isOngoing()) {
                clearAndPostNotification(sbn);
            }
        }
    }

    void clearAndPostNotification(StatusBarNotification sbn){
        Notification notif = sbn.getNotification();
        Log.d("NotificationLock",String.valueOf(notif.priority == Notification.PRIORITY_HIGH) + " priority High");
        Log.d("NotificationLock",String.valueOf(notif.priority == Notification.PRIORITY_MAX) + " priority Max");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            int notificationId = sbn.getId();
            if(notificationIdMap.get(notificationId)==null){
                notificationIdMap.put(notificationId,1);
            }else
            {
                int notificationCount = notificationIdMap.get(notificationId);
                notificationIdMap.put(notificationId,++notificationCount);
            }
            Notification.Builder builder = new Notification.Builder(getBaseContext());
            String notifSecondaryText = "";
           int notificationCount = notificationIdMap.get(notificationId);
            if(notificationCount == 1){
                notifSecondaryText = notificationCount +" New Message";
            }else{
                notifSecondaryText = notificationCount + " New Messages";
            }

            PendingIntent intent = notif.contentIntent;
            Bitmap icon = null;
            try{
                BitmapDrawable mpa = (BitmapDrawable)getBaseContext().getPackageManager().getApplicationIcon(sbn.getPackageName());
                if(mpa.getBitmap() != null){
                    icon = mpa.getBitmap();
                }
            }
            catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
            }
            builder.setContentIntent(intent)
                    .setContentTitle(notificationAppsMap.get(sbn.getPackageName()))
                    .setContentText(notifSecondaryText)
                    .setOngoing(false)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setColor(Color.parseColor("#37474F"))
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setSmallIcon(R.mipmap.ic_launcher);
            if(icon != null){
                builder.setLargeIcon(icon);
            }
            if(notif.priority== Notification.PRIORITY_HIGH || notif.priority == Notification.PRIORITY_MAX){
                builder.setPriority(Notification.PRIORITY_MAX);
                //
            }
            builder.build();

            notificationManager.notify(sbn.getId(),builder.build());

            cancelNotification(sbn.getKey());
        }
        else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            int notificationId = sbn.getId();
            if(notificationIdMap.get(notificationId)==null){
                notificationIdMap.put(notificationId,1);
            }
            else{
                int notificationCount = notificationIdMap.get(notificationId);
                notificationIdMap.put(notificationId,++notificationCount);
            }
            if(notif.priority== Notification.PRIORITY_HIGH || notif.priority == Notification.PRIORITY_MAX) {
                if (uiHandler.getLooper() != null) {
                    if (tickerHider == null) {
                        Message msg = uiHandler.obtainMessage(ADD_TICKER_VIEW);
                        msg.arg1 = notificationIdMap.get(notificationId);
                        msg.obj = sbn.getPackageName();
                        msg.sendToTarget();
                    } else {
                        Message msg = uiHandler.obtainMessage(UPDATE_TICKER_VIEW);
                        msg.arg1 = notificationIdMap.get(notificationId);
                        msg.obj = sbn.getPackageName();
                        msg.sendToTarget();
                    }
                }
            }
            Notification.Builder builder = new Notification.Builder(getBaseContext());
            String notifSecondaryText = "";
            int notificationCount = notificationIdMap.get(notificationId);
            if(notificationCount == 1){
                notifSecondaryText = notificationCount +" New Message";
            }else{
                notifSecondaryText = notificationCount + " New Messages";
            }

            PendingIntent intent = notif.contentIntent;
            Bitmap icon = null;
            try{
                BitmapDrawable mpa = (BitmapDrawable)getBaseContext().getPackageManager().getApplicationIcon(sbn.getPackageName());
                if(mpa.getBitmap() != null){
                    icon = mpa.getBitmap();
                }
            }
            catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
            }
            builder.setContentIntent(intent)
                    .setContentTitle(notificationAppsMap.get(sbn.getPackageName()))
                    .setContentText(notifSecondaryText)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher);
                    if(icon != null){
                        builder.setLargeIcon(icon);
                    }

            builder.build();

            notificationManager.notify(sbn.getId(),builder.build());

            Log.d("NotificationLock",sbn.getTag()+ " Tag");
            Log.d("NotificationLock",sbn.getId() + " ID");
            cancelNotification(sbn.getPackageName(), sbn.getTag(),sbn.getId());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(sbn.getPackageName().equals(getPackageName())){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                int notificationId = sbn.getId();
                if(notificationIdMap.get(notificationId)==null){
                    return;
                }
                else{
                    notificationIdMap.remove(notificationId);
                }
            }
            else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP) {
                int notificationId = sbn.getId();
                if(notificationIdMap.get(notificationId)==null){
                    return;
                }
                else{
                    notificationIdMap.remove(notificationId);
                }
            }
        }
        Log.d("NotificationLock","Removed  - " + sbn.getPackageName());
    }

    @Override
    public void onTickerHidden() {
        {
            Message msg = uiHandler.obtainMessage(REMOVE_TICKER_VIEW);
            msg.sendToTarget();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == ADD_TICKER_VIEW){
            String msgString = (String) msg.obj;
            tickerHider = new TickerHider(getBaseContext());
            tickerHider.setAppIcon(msgString);
            if(msg.arg1 == 1){
                tickerHider.setTickerText(msgString,msg.arg1 +" New Message");
            }else{
                tickerHider.setTickerText(msgString,msg.arg1 +" New Messages");
            }

            tickerHider.setOnTickerHiderActiveListener(this);
            tickerHider.setAnimation();
            windowManager.addView(tickerHider,getWindowParams());
            tickerHider.playAnimation();
            return true;
        }
        if(msg.what == REMOVE_TICKER_VIEW){
            if(tickerHider !=null){
                tickerHider.closeView();
            }
            windowManager.removeView(tickerHider);
            tickerHider = null;
        }
        if(msg.what == UPDATE_TICKER_VIEW){
            if(tickerHider != null){
                String msgString = (String) msg.obj;
                if(msg.arg1 == 1){
                    tickerHider.setTickerText(msgString,msg.arg1 +" New Message");
                }else{
                    tickerHider.setTickerText(msgString,msg.arg1 +" New Messages");
                }
            }
        }
        return false;
    }

    static class UpdateLockPackagesReceiver extends BroadcastReceiver{
        WeakReference<NotificationLockService> notificationReference;

        public UpdateLockPackagesReceiver(WeakReference<NotificationLockService> notificationReference) {
            this.notificationReference = notificationReference;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(UPDATE_LOCK_PACKAGES)){
                notificationReference.get().updateNotificationAppMap();
            }
        }
    }
}

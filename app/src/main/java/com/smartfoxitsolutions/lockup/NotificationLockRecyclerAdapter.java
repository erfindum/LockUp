package com.smartfoxitsolutions.lockup;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.smartfoxitsolutions.lockup.services.NotificationLockService;

/**
 * Created by RAAJA on 20-10-2016.
 */

public class NotificationLockRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
                                                    implements NotificationLockRecyclerHolder.OnListItemClickListener{
    private PackageManager pkgManager;
    private AppLockModel appModel;
    private TreeMap<String,String> checkedAppsMap, notificationCheckedAppsMap;
    private ArrayList<String> checkedAppsName, checkedAppsPackage;
    private ArrayList<NotificationLockRecyclerHolder> holderList;
    private Context context;

    public NotificationLockRecyclerAdapter(Context context,AppLockModel appLockModel) {
        this.context = context;
        this.appModel = appLockModel;
        pkgManager = context.getPackageManager();
        this.notificationCheckedAppsMap = appLockModel.getNotificationCheckedAppMap();
        this.holderList = new ArrayList<>();
    }

    void setCheckedAppsMap(TreeMap<String,String> appsMap){
        this.checkedAppsMap = appsMap;
        this.checkedAppsName = new ArrayList<>(appsMap.values());
        this.checkedAppsPackage = new ArrayList<>(appsMap.keySet());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View itemView = inflater.inflate(R.layout.notification_lock_recycler_item,parent,false);
            NotificationLockRecyclerHolder item =  new NotificationLockRecyclerHolder(itemView);
            item.setOnListItemClickListener(this);
            holderList.add(item);
            return item;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        NotificationLockRecyclerHolder itemView = (NotificationLockRecyclerHolder) holder;
        try {
            itemView.getAppImage().setBackgroundResource(0);
            Drawable appIcon = pkgManager.getApplicationIcon(checkedAppsPackage.get(position));
            itemView.getAppImage().setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        itemView.getAppName().setText(checkedAppsName.get(position));
        if(!notificationCheckedAppsMap.containsKey(checkedAppsPackage.get(position))){
            changeNotificationButtonImage(itemView.getNotificationButton(),false);
        }
        else {
            changeNotificationButtonImage(itemView.getNotificationButton(),true);
        }
    }

    @Override
    public int getItemCount() {
        return checkedAppsMap.size();
    }

    private void changeNotificationButtonImage(AppCompatImageButton button, boolean isSelected){
        if(isSelected){
            button.setImageResource(0);
            button.setImageResource(R.drawable.ic_app_lock_activity_hide_notif_selected);
        }
        else {
            button.setImageResource(0);
            button.setImageResource(R.drawable.ic_app_lock_activity_hide_notif);
        }
    }

    @Override
    public void onListItemClicked(NotificationLockRecyclerHolder itemView, int listItemPosition) {
        AppCompatImageButton lockButton = itemView.getNotificationButton();
        if(notificationCheckedAppsMap.containsKey(checkedAppsPackage.get(listItemPosition))) {
            changeNotificationButtonImage(lockButton,false);
            itemView.getNotifButtonAnimator().start();
            notificationCheckedAppsMap.remove(checkedAppsPackage.get(listItemPosition));
        }else{
            changeNotificationButtonImage(lockButton,true);
            itemView.getNotifButtonAnimator().start();
            notificationCheckedAppsMap.put(checkedAppsPackage.get(listItemPosition),checkedAppsName.get(listItemPosition));
        }
        Log.d("NotificationLock"," Item count " + getItemCount() +" "+ notificationCheckedAppsMap.size()+" ");
        for(Map.Entry<String,String> entry : notificationCheckedAppsMap.entrySet()){
            Log.d("NotificationLock",entry.getKey()+" PACKAGE : " +entry.getValue()+" NAME");
        }
    }

    void closeAppLockRecyclerAdapter(){
        for(NotificationLockRecyclerHolder holder : holderList){
            holder.setOnListItemClickListener(null);
        }
        context = null;
    }

    void updateAppModel(){
        appModel.updateAppPackages(notificationCheckedAppsMap,AppLockModel.NOTIFICATION_CHECKED_APPS_PACKAGE);
       int appListAdded =  appModel.loadAppPackages(AppLockModel.NOTIFICATION_CHECKED_APPS_PACKAGE);
        if(appListAdded == AppLockModel.APP_LIST_UPDATED){
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(NotificationLockService.UPDATE_LOCK_PACKAGES));
        }
    }
}

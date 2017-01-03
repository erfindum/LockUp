package com.smartfoxitsolutions.lockup;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.smartfoxitsolutions.lockup.receivers.PreventUninstallReceiver;
import com.smartfoxitsolutions.lockup.services.NotificationLockService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * Created by RAAJA on 09-09-2016.
 */
public class AppLockRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements AppLockRecyclerViewItem.onAppListItemClickListener {
    private static final String TAG ="AppLockRecyclerAdapter";

    private static final int LIST_VIEW_TYPE_ITEM =1;
    private static final int LIST_VIEW_TYPE_HEADER = 0;

    public static int HEADER_MARGIN_SIZE_TEN;
    public static int HEADER_MARGIN_SIZE_FIFTEEN;
    public  static int HEADER_MARGIN_SIZE_MINUS_SEVEN;


    private static final int ITEM_POSITION_RANGE_HEADER_ONE = 3;
    private static final int ITEM_POSITION_RANGE_RECOMMENDED_APPS = 4;
    private static final int ITEM_POSITION_RANGE_HEADER_TWO = 5;
    private static final int ITEM_POSITION_RANGE_CHECKED_APPS = 6;
    private static final int ITEM_POSITION_RANGE_HEADER_THREE = 7;
    private static final int ITEM_POSITION_RANGE_INSTALLED_APPS = 8;

    private ArrayList<String> installedAppsName,installedAppsPackage,checkedAppsName,checkedAppsPackage,
                        recommendedAppPackageList;
    private ArrayList<String> recommendedAppLockedName;
    private ArrayList<Boolean> recommendedAppLocked;
    private ArrayList<AppLockRecyclerViewItem> itemHolder;
    private TreeMap<String,String> installedAppsMap,checkedAppsMap, notificationAppsMap;
    private LinkedHashMap<String,HashMap<String,Boolean>> recommendedAppsMap;
    private PackageManager packageManager;
    private AppLockModel appLockModel;
    private AppLockActivity activity;


    public AppLockRecyclerAdapter(AppLockModel appModel, AppLockActivity activity) {
        this.installedAppsPackage =  appModel.getInstalledAppsPackage();
        this.installedAppsName = appModel.getInstalledAppsName();
        this.checkedAppsPackage = appModel.getCheckedAppsPackage();
        this.checkedAppsName= appModel.getCheckedAppsName();
        this.recommendedAppPackageList = appModel.getRecommendedAppPackageList();
        this.installedAppsMap = appModel.getInstalledAppsMap();
        this.checkedAppsMap = appModel.getCheckedAppsMap();
        this.notificationAppsMap = appModel.getNotificationCheckedAppMap();
        this.recommendedAppsMap= appModel.getRecommendedAppsMap();
        HashMap<String,Boolean> recommendedAppLockedMap = appModel.getRecommendedAppLocked();
        this.recommendedAppLockedName = new ArrayList<>(recommendedAppLockedMap.keySet());
        this.recommendedAppLocked = new ArrayList<>(recommendedAppLockedMap.values());
        this.itemHolder = new ArrayList<>();
        this.activity = activity;
        this.packageManager = activity.getPackageManager();
        this.appLockModel = appModel;
    }

    private int getHeaderOneSize(){
        return  1;
    }

    private int getRecommendedListSize(){
        return getHeaderOneSize() + recommendedAppPackageList.size();
    }

    private int getHeaderTwoSize(){
        return getRecommendedListSize()+ 1;
    }

    private int getCheckedListSize(){

        return getHeaderTwoSize()+checkedAppsPackage.size();
    }

    private int getHeaderThreeSize(){
        if(checkedAppsPackage.isEmpty()){
            return getHeaderTwoSize()+1;
        }else {
            return getCheckedListSize() + 1;
        }
    }

    private int getInstalledListSize(){

        if(!installedAppsPackage.isEmpty()){
            return getHeaderThreeSize()+ installedAppsPackage.size();
        }else{
            return getHeaderThreeSize()+1;
        }

    }

    /**
     * Returns the range of the item in data model from position of the Item in Adapter.
     * @param position position of the Item in Adapter.
     * @return int value of the range.
     */

    private int getItemPositionRange(int position){
        if(position==(getHeaderOneSize() -1)){
            return ITEM_POSITION_RANGE_HEADER_ONE;
        }
        if(position>(getHeaderOneSize()-1) && position<= (getRecommendedListSize() -1)){
            return ITEM_POSITION_RANGE_RECOMMENDED_APPS;
        }
        if(position==(getHeaderTwoSize()-1)){
            return ITEM_POSITION_RANGE_HEADER_TWO;
        }
        if((position>(getHeaderTwoSize()-1) && position<= (getCheckedListSize()-1))){
            return ITEM_POSITION_RANGE_CHECKED_APPS;
        }
        if(position == (getHeaderThreeSize()-1)){
            return ITEM_POSITION_RANGE_HEADER_THREE;
        }
        if (position> (getHeaderThreeSize()-1) && position<=(getInstalledListSize()-1)){
            return ITEM_POSITION_RANGE_INSTALLED_APPS;
        }
        return 0;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if(viewType==LIST_VIEW_TYPE_ITEM){
            View itemView = inflater.inflate(R.layout.app_lock_recycler_item_view,parent,false);
            AppLockRecyclerViewItem item =  new AppLockRecyclerViewItem(itemView);
            item.setOnAppListItemClickListener(this);
            itemHolder.add(item);
            return item;
        }
        if(viewType==LIST_VIEW_TYPE_HEADER){
            View itemView = inflater.inflate(R.layout.app_lock_recycler_header_view,parent,false);
            return new AppLockRecyclerViewHeader(itemView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(getItemPositionRange(position)==ITEM_POSITION_RANGE_HEADER_ONE){
            AppLockRecyclerViewHeader headerView = (AppLockRecyclerViewHeader) holder;
            headerView.getHeaderText().setText(R.string.appLock_activity_additional_settings_header);
        }
        else if(getItemPositionRange(position)==ITEM_POSITION_RANGE_RECOMMENDED_APPS){
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN_MR2){
                AppLockRecyclerViewItem itemView = (AppLockRecyclerViewItem) holder;
                int listPosition = position - getHeaderOneSize();
                if (listPosition == 0) {
                    itemView.getAppName().setText(recommendedAppLockedName.get(listPosition));
                    if(AppLockActivity.isDeviceAdminEnabled){
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion_selected);
                        changeLockButtonImage(itemView.getLockButton(), true);
                    }else{
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion);
                        changeLockButtonImage(itemView.getLockButton(), false);
                    }

                } else {
                    try {
                        itemView.getAppImage().setBackgroundResource(0);
                        Drawable appIcon = packageManager.getApplicationIcon(recommendedAppPackageList.get(listPosition));
                        itemView.getAppImage().setImageDrawable(appIcon);
                    } catch (PackageManager.NameNotFoundException e){
                        e.printStackTrace();
                    }
                    itemView.getAppName().setText(recommendedAppLockedName.get(listPosition));
                    if (recommendedAppLocked.get(listPosition)) {
                        changeLockButtonImage(itemView.getLockButton(), true);
                    } else {
                        changeLockButtonImage(itemView.getLockButton(), false);
                    }
                }
            }else {
                AppLockRecyclerViewItem itemView = (AppLockRecyclerViewItem) holder;
                int listPosition = position - getHeaderOneSize();
                if (listPosition == 0) {
                    itemView.getAppImage().setImageDrawable(null);
                    itemView.getAppImage().setBackgroundResource(R.drawable.ic_app_lock_activity_main_hide_notif);
                    itemView.getAppName().setText(recommendedAppLockedName.get(listPosition));
                    itemView.getLockButton().setImageResource(0);
                    itemView.getLockButton().setImageResource(R.drawable.ic_app_lock_activity_notif_navigation);
                } else if (listPosition == 1) {
                    itemView.getAppName().setText(recommendedAppLockedName.get(listPosition));
                    if(AppLockActivity.isDeviceAdminEnabled){
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion_selected);
                        changeLockButtonImage(itemView.getLockButton(), true);

                    }else{
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion);
                        changeLockButtonImage(itemView.getLockButton(), false);
                    }
                } else {
                    try {
                        itemView.getAppImage().setBackgroundResource(0);
                        Drawable appIcon = packageManager.getApplicationIcon(recommendedAppPackageList.get(listPosition));
                        itemView.getAppImage().setImageDrawable(appIcon);
                    } catch (PackageManager.NameNotFoundException e){
                        e.printStackTrace();
                    }
                    itemView.getAppName().setText(recommendedAppLockedName.get(listPosition));
                    if (recommendedAppLocked.get(listPosition)) {
                        changeLockButtonImage(itemView.getLockButton(), true);
                    } else {
                        changeLockButtonImage(itemView.getLockButton(), false);
                    }
                }
            }
        }
        else if(getItemPositionRange(position)==ITEM_POSITION_RANGE_HEADER_TWO){
            AppLockRecyclerViewHeader headerView = (AppLockRecyclerViewHeader) holder;
            if(checkedAppsPackage.isEmpty()){
                headerView.getHeaderText().setText(R.string.appLock_activity_checked_apps_empty);
                CardView.LayoutParams layoutParams = new FrameLayout.LayoutParams(headerView.getHeaderView().getLayoutParams());

                layoutParams.setMargins(HEADER_MARGIN_SIZE_TEN,
                        HEADER_MARGIN_SIZE_FIFTEEN,
                        HEADER_MARGIN_SIZE_TEN, HEADER_MARGIN_SIZE_FIFTEEN);
                headerView.getHeaderView().setLayoutParams(layoutParams);
            }else{
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    CardView.LayoutParams layoutParams = new FrameLayout.LayoutParams(headerView.getHeaderView().getLayoutParams());

                    layoutParams.setMargins(HEADER_MARGIN_SIZE_TEN,
                            HEADER_MARGIN_SIZE_FIFTEEN,
                            HEADER_MARGIN_SIZE_TEN, 0);
                    headerView.getHeaderView().setLayoutParams(layoutParams);
                }
                headerView.getHeaderText().setText(R.string.appLock_activity_checked_apps_header);
            }
        }

        else if(!checkedAppsPackage.isEmpty() && getItemPositionRange(position)==ITEM_POSITION_RANGE_CHECKED_APPS){
           AppLockRecyclerViewItem itemView = (AppLockRecyclerViewItem) holder;
            int listPosition = position-getHeaderTwoSize();
            try {
                itemView.getAppImage().setBackgroundResource(0);
                Drawable appIcon = packageManager.getApplicationIcon(checkedAppsPackage.get(listPosition));
                itemView.getAppImage().setImageDrawable(appIcon);
            } catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
            }
            itemView.getAppName().setText(checkedAppsName.get(listPosition));
            if(!checkedAppsMap.containsKey(checkedAppsPackage.get(listPosition))){
                changeLockButtonImage(itemView.getLockButton(),false);
            }
            else if (checkedAppsMap.containsKey(checkedAppsPackage.get(listPosition))){
                changeLockButtonImage(itemView.getLockButton(),true);
            }
        }
        else if(getItemPositionRange(position)==ITEM_POSITION_RANGE_HEADER_THREE){
            AppLockRecyclerViewHeader headerView = (AppLockRecyclerViewHeader) holder;
            if(installedAppsPackage.isEmpty()){
                headerView.getHeaderText().setText(R.string.appLock_activity_installed_apps_empty);
                CardView.LayoutParams layoutParams = new FrameLayout.LayoutParams(headerView.getHeaderView().getLayoutParams());

                layoutParams.setMargins(HEADER_MARGIN_SIZE_TEN,
                        HEADER_MARGIN_SIZE_FIFTEEN,
                        HEADER_MARGIN_SIZE_TEN, HEADER_MARGIN_SIZE_FIFTEEN);
                headerView.getHeaderView().setLayoutParams(layoutParams);
            }else{
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    headerView.getHeaderText().setText(R.string.appLock_activity_installed_apps_header);
                    CardView.LayoutParams layoutParams = new FrameLayout.LayoutParams(headerView.getHeaderView().getLayoutParams());

                    layoutParams.setMargins(HEADER_MARGIN_SIZE_TEN,
                            HEADER_MARGIN_SIZE_FIFTEEN,
                            HEADER_MARGIN_SIZE_TEN, 0);
                    headerView.getHeaderView().setLayoutParams(layoutParams);
                }else{
                    headerView.getHeaderText().setText(R.string.appLock_activity_installed_apps_header);
                    CardView.LayoutParams layoutParams = new FrameLayout.LayoutParams(headerView.getHeaderView().getLayoutParams());

                    layoutParams.setMargins(HEADER_MARGIN_SIZE_TEN,
                            HEADER_MARGIN_SIZE_FIFTEEN,
                            HEADER_MARGIN_SIZE_TEN,HEADER_MARGIN_SIZE_MINUS_SEVEN );
                    headerView.getHeaderView().setLayoutParams(layoutParams);
                }
                headerView.getHeaderText().setText(R.string.appLock_activity_installed_apps_header);
            }
        }
        else if(!installedAppsPackage.isEmpty() && getItemPositionRange(position)==ITEM_POSITION_RANGE_INSTALLED_APPS){
            AppLockRecyclerViewItem itemView = (AppLockRecyclerViewItem) holder;
            int listPosition = position-getHeaderThreeSize();
            try {
                itemView.getAppImage().setBackgroundResource(0);
                Drawable appIcon = packageManager.getApplicationIcon(installedAppsPackage.get(listPosition));
                itemView.getAppImage().setImageDrawable(appIcon);
            } catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
            }
            itemView.getAppName().setText(installedAppsName.get(listPosition));
            if(!installedAppsMap.containsKey(installedAppsPackage.get(listPosition))){
                changeLockButtonImage(itemView.getLockButton(),true);
            }
            else if (installedAppsMap.containsKey(installedAppsPackage.get(listPosition))){
                changeLockButtonImage(itemView.getLockButton(),false);
            }
        }
    }

    @Override
    public int getItemCount() {
        return 3+ recommendedAppPackageList.size()+installedAppsPackage.size()+checkedAppsPackage.size();
    }

    @Override
    public int getItemViewType(int position) {
         super.getItemViewType(position);
        if(getItemPositionRange(position) == ITEM_POSITION_RANGE_HEADER_ONE){
            return LIST_VIEW_TYPE_HEADER;
        }else if(getItemPositionRange(position) == ITEM_POSITION_RANGE_RECOMMENDED_APPS){
            return LIST_VIEW_TYPE_ITEM;
        }else if(getItemPositionRange(position) == ITEM_POSITION_RANGE_HEADER_TWO){
            return LIST_VIEW_TYPE_HEADER;
        }else if(!checkedAppsPackage.isEmpty() && (getItemPositionRange(position) == ITEM_POSITION_RANGE_CHECKED_APPS)){
            return LIST_VIEW_TYPE_ITEM;
        }else if(getItemPositionRange(position)== ITEM_POSITION_RANGE_HEADER_THREE){
            return LIST_VIEW_TYPE_HEADER;
        }else if(!installedAppsPackage.isEmpty() && getItemPositionRange(position) == ITEM_POSITION_RANGE_INSTALLED_APPS){
            return LIST_VIEW_TYPE_ITEM;
        }
        return 0;
    }

    private String[] getCurrentCheckedAppsPackages(){
            String[] packageArray = new String[0];
            String[] packages; packages = checkedAppsMap.keySet().toArray(packageArray);
        return packages;
    }

    private String[] getCurrentCheckedAppsNames(){

            String[] appNameArray = new String[0];
            String[] appNames = checkedAppsMap.values().toArray(appNameArray);
        return appNames;
    }


    @Override
    public void onAppListItemClicked(AppLockRecyclerViewItem itemView, int listItemPosition) {

        if(getItemPositionRange(listItemPosition)==ITEM_POSITION_RANGE_RECOMMENDED_APPS){
            int listPosition = listItemPosition-getHeaderOneSize();
            if(!AppLockActivity.shouldStartAppLock){
                activity.startAppLockSwitchOnDialog();
                return;
            }
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN_MR2){
                AppCompatImageButton lockButton = itemView.getLockButton();
                boolean val = recommendedAppLocked.get(listPosition);
                if(listPosition == 0) {
                    if (AppLockActivity.isDeviceAdminEnabled) {
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, false);
                        DevicePolicyManager manager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
                        manager.removeActiveAdmin(new ComponentName(activity,PreventUninstallReceiver.class));
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion);
                    } else {
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, true);
                        Intent enableDeviceIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        ComponentName componentName = new ComponentName(activity,PreventUninstallReceiver.class);
                        enableDeviceIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                        enableDeviceIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                activity.getResources().getString(R.string.appLock_activity_prevent_uninstall_message_text));
                        activity.startActivity(enableDeviceIntent);
                        activity.shouldTrackUserPresence = false;
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion_selected);
                    }
                }
                else{
                    if(val){
                        activity.startRecommendedAlertDialog(itemView,listPosition);
                        return;
                    }
                    else{
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, true);
                        HashMap<String,Boolean> recommendTempMap = new HashMap<>();
                        recommendTempMap.put(recommendedAppLockedName.get(listPosition),true);
                        recommendedAppsMap.put(recommendedAppPackageList.get(listPosition), recommendTempMap);
                        recommendedAppLocked.set(listPosition,true);
                    }
                }
            }else {
                if(listPosition == 0){
                    if(NotificationLockService.isNotificationServiceConnected) {
                            activity.startActivity(new Intent(activity.getBaseContext(), NotificationLockActivity.class)
                                    .putExtra(AppLockModel.NOTIFICATION_ACTIVITY_CHECKED_APPS_NAME_KEY, getCurrentCheckedAppsNames())
                                    .putExtra(AppLockModel.NOTIFICATION_ACTIVITY_CHECKED_APPS_PACKAGE_KEY, getCurrentCheckedAppsPackages()));
                            activity.shouldTrackUserPresence = false;
                        return;
                    }else{
                        activity.startNotificationPermissionDialog();
                        return;
                    }
                }
                boolean val = recommendedAppLocked.get(listPosition);
                AppCompatImageButton lockButton = itemView.getLockButton();
                if(listPosition == 1) {
                    if (AppLockActivity.isDeviceAdminEnabled) {
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, false);
                        DevicePolicyManager manager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
                        manager.removeActiveAdmin(new ComponentName(activity,PreventUninstallReceiver.class));
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion);
                    } else {
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, true);
                        Intent enableDeviceIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        ComponentName componentName = new ComponentName(activity,PreventUninstallReceiver.class);
                        enableDeviceIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                        enableDeviceIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                activity.getResources().getString(R.string.appLock_activity_prevent_uninstall_message_text));
                        activity.startActivity(enableDeviceIntent);
                        activity.shouldTrackUserPresence = false;
                        itemView.getAppImage().setImageDrawable(null);
                        itemView.getAppImage().setBackgroundResource(R.drawable.ic_uninstall_prevetion_selected);
                    }
                }
                else {
                    if(val){
                            activity.startRecommendedAlertDialog(itemView,listPosition);
                        return;
                    }
                    else{
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, true);
                        HashMap<String,Boolean> recommendTempMap = new HashMap<>();
                        recommendTempMap.put(recommendedAppLockedName.get(listPosition),true);
                        recommendedAppsMap.put(recommendedAppPackageList.get(listPosition), recommendTempMap);
                        recommendedAppLocked.set(listPosition,true);
                    }
                }
            }
        }
        else if(!checkedAppsPackage.isEmpty() && getItemPositionRange(listItemPosition)==ITEM_POSITION_RANGE_CHECKED_APPS){
            if(!AppLockActivity.shouldStartAppLock){
                activity.startAppLockSwitchOnDialog();
                return;
            }
            int listPosition = listItemPosition-getHeaderTwoSize();
            AppCompatImageButton lockButton = itemView.getLockButton();
            if(checkedAppsMap.containsKey(checkedAppsPackage.get(listPosition))) {
                changeLockButtonImage(lockButton,false);
                itemView.getLockButtonAnimator().start();
                if(notificationAppsMap.containsKey(checkedAppsPackage.get(listPosition))){
                    notificationAppsMap.remove(checkedAppsPackage.get(listPosition));
                    updateNotificationAppsMap();
                }
                installedAppsMap.put(checkedAppsPackage.get(listPosition),checkedAppsName.get(listPosition));
                checkedAppsMap.remove(checkedAppsPackage.get(listPosition));
            }else{
                changeLockButtonImage(lockButton,true);
                itemView.getLockButtonAnimator().start();
                checkedAppsMap.put(checkedAppsPackage.get(listPosition),checkedAppsName.get(listPosition));
                installedAppsMap.remove(checkedAppsPackage.get(listPosition));
            }
            LockUpMainActivity.installedAppsCount = installedAppsMap.size();
            LockUpMainActivity.lockedAppsCount = checkedAppsMap.size();
        }
        else if (!installedAppsPackage.isEmpty() && getItemPositionRange(listItemPosition)==ITEM_POSITION_RANGE_INSTALLED_APPS){
            if(!AppLockActivity.shouldStartAppLock){
                activity.startAppLockSwitchOnDialog();
                return;
            }
            int listPosition = listItemPosition-getHeaderThreeSize();
            AppCompatImageButton lockButton = itemView.getLockButton();
            if(installedAppsMap.containsKey(installedAppsPackage.get(listPosition))) {
                changeLockButtonImage(lockButton,true);
                itemView.getLockButtonAnimator().start();
                checkedAppsMap.put(installedAppsPackage.get(listPosition),installedAppsName.get(listPosition));
                installedAppsMap.remove(installedAppsPackage.get(listPosition));
            }else{
                changeLockButtonImage(lockButton,false);
                itemView.getLockButtonAnimator().start();
                installedAppsMap.put(installedAppsPackage.get(listPosition),installedAppsName.get(listPosition));
                checkedAppsMap.remove(installedAppsPackage.get(listPosition));
            }
            LockUpMainActivity.installedAppsCount = installedAppsMap.size();
            LockUpMainActivity.lockedAppsCount = checkedAppsMap.size();
            Log.d(TAG," Item count " + getItemCount() +" "+ installedAppsMap.size()+" "+ checkedAppsMap.size());
        }
    }

    private void changeLockButtonImage(AppCompatImageButton button,boolean checked){
        if(checked){
            button.setImageResource(0);
            button.setImageResource(R.drawable.ic_app_lock_activity_lock_selected);
        }
        else {
            button.setImageResource(0);
            button.setImageResource(R.drawable.ic_app_lock_activity_lock_unselected);
        }
    }

    void removeRecommendedApp(AppLockRecyclerViewItem itemView,int position){
        AppCompatImageButton lockButton = itemView.getLockButton();
        itemView.getLockButtonAnimator().start();
        changeLockButtonImage(lockButton, false);
        HashMap<String,Boolean> recommendTempMap = new HashMap<>();
        recommendTempMap.put(recommendedAppLockedName.get(position),false);
        recommendedAppsMap.put(recommendedAppPackageList.get(position), recommendTempMap);
        recommendedAppLocked.set(position,false);
    }

    void loadNotificationAppsMap(){
        appLockModel.loadAppPackages(AppLockModel.NOTIFICATION_CHECKED_APPS_PACKAGE);
        notificationAppsMap = appLockModel.getNotificationCheckedAppMap();
    }

    void updateNotificationAppsMap(){
        appLockModel.updateAppPackages(notificationAppsMap,AppLockModel.NOTIFICATION_CHECKED_APPS_PACKAGE);
    }

    void updateAppModel(){
        appLockModel.updateAppPackages(installedAppsMap,AppLockModel.INSTALLED_APPS_PACKAGE);
        appLockModel.updateAppPackages(checkedAppsMap,AppLockModel.CHECKED_APPS_PACKAGE);
        appLockModel.updateRecommendedAppPackages(recommendedAppsMap);
        appLockModel.updateAppPackages(notificationAppsMap,AppLockModel.NOTIFICATION_CHECKED_APPS_PACKAGE);
    }

    void closeAppLockRecyclerAdapter(){
        for(AppLockRecyclerViewItem holder : itemHolder){
            holder.setOnAppListItemClickListener(null);
        }
        LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(NotificationLockService.UPDATE_LOCK_PACKAGES));
        activity = null;
    }
}

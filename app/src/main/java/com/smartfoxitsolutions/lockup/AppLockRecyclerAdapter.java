package com.smartfoxitsolutions.lockup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
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


    private static final int ITEM_POSITION_RANGE_HEADER_ONE = 3;
    private static final int ITEM_POSITION_RANGE_RECOMMENDED_APPS = 4;
    private static final int ITEM_POSITION_RANGE_HEADER_TWO = 5;
    private static final int ITEM_POSITION_RANGE_CHECKED_APPS = 6;
    private static final int ITEM_POSITION_RANGE_HEADER_THREE = 7;
    private static final int ITEM_POSITION_RANGE_INSTALLED_APPS = 8;

    private ArrayList<String> installedAppsName,installedAppsPackage,checkedAppsName,checkedAppsPackage,
                        recommendedAppList;
    private ArrayList<Boolean> recommendedAppLocked;
    private ArrayList<AppLockRecyclerViewItem> itemHolder;
    private TreeMap<String,String> installedAppsMap,checkedAppsMap;
    private HashMap<String,Boolean> recommendedAppsMap;
    private PackageManager packageManager;
    private AppLockModel appLockModel;
    private AppLockActivity activity;


    public AppLockRecyclerAdapter(AppLockModel appModel, AppLockActivity activity) {
        this.installedAppsPackage =  appModel.getInstalledAppsPackage();
        this.installedAppsName = appModel.getInstalledAppsName();
        this.checkedAppsPackage = appModel.getCheckedAppsPackage();
        this.checkedAppsName= appModel.getCheckedAppsName();
        this.recommendedAppList = appModel.getRecommendedAppList();
        this.installedAppsMap = appModel.getInstalledAppsMap();
        this.checkedAppsMap = appModel.getCheckedAppsMap();
        this.recommendedAppsMap= appModel.getRecommendedAppsMap();
        this.recommendedAppLocked = appModel.getRecommendedAppLocked();
        this.itemHolder = new ArrayList<>();
        this.activity = activity;
        this.packageManager = activity.getPackageManager();
        this.appLockModel = appModel;
    }

    private int getHeaderOneSize(){
        return  1;
    }

    private int getRecommendedListSize(){
        return getHeaderOneSize() +recommendedAppList.size();
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
                    itemView.getAppImage().setImageDrawable(null);
                    itemView.getAppName().setText(recommendedAppList.get(listPosition));
                    itemView.getAppImage().setBackgroundResource(R.drawable.img_pin_normal);
                } else if (listPosition == 1) {
                    itemView.getAppImage().setImageDrawable(null);
                    itemView.getAppName().setText(recommendedAppList.get(listPosition));
                    itemView.getAppImage().setBackgroundResource(R.drawable.img_pin_normal);
                }
                if (recommendedAppsMap.get(recommendedAppList.get(listPosition))) {
                    changeLockButtonImage(itemView.getLockButton(), true);
                } else {
                    changeLockButtonImage(itemView.getLockButton(), false);
                }
            }else {
                AppLockRecyclerViewItem itemView = (AppLockRecyclerViewItem) holder;
                int listPosition = position - getHeaderOneSize();
                if (listPosition == 0) {
                    itemView.getAppImage().setImageDrawable(null);
                    itemView.getAppImage().setBackgroundResource(R.drawable.ic_app_lock_activity_hide_notif);
                    itemView.getAppName().setText(recommendedAppList.get(listPosition));
                } else if (listPosition == 1) {
                    itemView.getAppImage().setImageDrawable(null);
                    itemView.getAppName().setText(recommendedAppList.get(listPosition));
                    itemView.getAppImage().setBackgroundResource(R.drawable.img_pin_normal);
                } else if (listPosition == 2) {
                    itemView.getAppImage().setImageDrawable(null);
                    itemView.getAppName().setText(recommendedAppList.get(listPosition));
                    itemView.getAppImage().setBackgroundResource(R.drawable.img_pin_normal);
                }
                if(listPosition == 0){
                    itemView.getLockButton().setImageResource(0);
                    itemView.getLockButton().setImageResource(R.drawable.ic_app_lock_activity_notif_navigation);
                }else
                if (recommendedAppsMap.get(recommendedAppList.get(listPosition))) {
                    changeLockButtonImage(itemView.getLockButton(), true);
                } else {
                    changeLockButtonImage(itemView.getLockButton(), false);
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
        return 3+recommendedAppList.size()+installedAppsPackage.size()+checkedAppsPackage.size();
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


    @Override
    public void onAppListItemClicked(AppLockRecyclerViewItem itemView, int listItemPosition) {

        if(getItemPositionRange(listItemPosition)==ITEM_POSITION_RANGE_RECOMMENDED_APPS){
            int listPosition = listItemPosition-getHeaderOneSize();
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN_MR2){
                AppCompatImageButton lockButton = itemView.getLockButton();
                boolean val = recommendedAppsMap.get(recommendedAppList.get(listPosition));
                if (val) {
                    itemView.getLockButtonAnimator().start();
                    changeLockButtonImage(lockButton, false);
                    recommendedAppsMap.put(recommendedAppList.get(listPosition), false);
                } else {
                    itemView.getLockButtonAnimator().start();
                    changeLockButtonImage(lockButton, true);
                    recommendedAppsMap.put(recommendedAppList.get(listPosition), true);
                }
            }else {
                if(listPosition == 0){
                    activity.startActivity(new Intent(activity.getBaseContext(),NotificationLockActivity.class));
                }
                else {
                    AppCompatImageButton lockButton = itemView.getLockButton();
                    boolean val = recommendedAppsMap.get(recommendedAppList.get(listPosition));
                    if (val) {
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, false);
                        recommendedAppsMap.put(recommendedAppList.get(listPosition), false);
                    } else {
                        itemView.getLockButtonAnimator().start();
                        changeLockButtonImage(lockButton, true);
                        recommendedAppsMap.put(recommendedAppList.get(listPosition), true);
                    }
                }
            }
        }
        else if(!checkedAppsPackage.isEmpty() && getItemPositionRange(listItemPosition)==ITEM_POSITION_RANGE_CHECKED_APPS){
            int listPosition = listItemPosition-getHeaderTwoSize();
            AppCompatImageButton lockButton = itemView.getLockButton();
            if((Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) && !activity.getUsageAccessPermissionGranted()){
                activity.startUsagePermissionDialog();
                return;
            }
            if(checkedAppsMap.containsKey(checkedAppsPackage.get(listPosition))) {
                changeLockButtonImage(lockButton,false);
                itemView.getLockButtonAnimator().start();
                installedAppsMap.put(checkedAppsPackage.get(listPosition),checkedAppsName.get(listPosition));
                checkedAppsMap.remove(checkedAppsPackage.get(listPosition));
            }else{
                changeLockButtonImage(lockButton,true);
                itemView.getLockButtonAnimator().start();
                checkedAppsMap.put(checkedAppsPackage.get(listPosition),checkedAppsName.get(listPosition));
                installedAppsMap.remove(checkedAppsPackage.get(listPosition));
            }
        }
        else if (!installedAppsPackage.isEmpty() && getItemPositionRange(listItemPosition)==ITEM_POSITION_RANGE_INSTALLED_APPS){
            int listPosition = listItemPosition-getHeaderThreeSize();
            AppCompatImageButton lockButton = itemView.getLockButton();
            if((Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) && !activity.getUsageAccessPermissionGranted()){
                activity.startUsagePermissionDialog();
                return;
            }
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

    void closeAppLockRecyclerAdapter(){
        for(AppLockRecyclerViewItem holder : itemHolder){
            holder.setOnAppListItemClickListener(null);
        }
        activity = null;
    }

    void updateAppModel(){
        appLockModel.updateAppPackages(installedAppsMap,AppLockModel.INSTALLED_APPS_PACKAGE);
        appLockModel.updateAppPackages(checkedAppsMap,AppLockModel.CHECKED_APPS_PACKAGE);
        appLockModel.updateRecommendedAppPackages(recommendedAppsMap);

        appLockModel.loadAppPackages(AppLockModel.INSTALLED_APPS_PACKAGE);
        appLockModel.loadAppPackages(AppLockModel.CHECKED_APPS_PACKAGE);
        appLockModel.loadAppPackages(AppLockModel.RECOMMENDED_APPS_PACKAGE);
    }
}

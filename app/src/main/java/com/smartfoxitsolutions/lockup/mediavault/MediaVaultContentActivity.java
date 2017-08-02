package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.MediaDeleteDialog;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.MediaMoveOutDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by RAAJA on 04-11-2016.
 */

public class MediaVaultContentActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    static String SELECTED_MEDIA_FILE_KEY = "selected_media_file";
    static String MOVE_OUT_VAULT_MEDIA_TAG = "move_out_vault_media";
    static String DELETE_VAULT_MEDIA_TAG = "delete_vault_media";


    private RecyclerView mediaVaultContentRecycler;
    private int noOfColumns, itemSize;
    private String bucketId, mediaType;
    private Toolbar toolbar;
    private MediaVaultContentAdapter mediaContentAdapter;
    private AppCompatImageButton selectAllButton,unlockButton, deleteButton;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private ArrayList<String> selectedMediaId;
    private RelativeLayout bottomBar;
    private ValueAnimator bottomBarAnimator;
    private boolean isLockPressed,isDeletePressed;
    private DialogFragment moveOutDialog, deleteFilesDialog;
    boolean shouldTrackUserPresence, shouldCloseAffinity;
    private VaultContentScreenOffReceiver vaultContentScreenOffReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldTrackUserPresence = true;
        setContentView(R.layout.vault_media_content_activity);
        mediaVaultContentRecycler = (RecyclerView) findViewById(R.id.vault_media_content_recycler);
        bottomBar = (RelativeLayout) findViewById(R.id.vault_media_content_bottom_bar);
        selectAllButton = (AppCompatImageButton) findViewById(R.id.vault_media_content_select_all);
        unlockButton = (AppCompatImageButton) findViewById(R.id.vault_media_content_unlock);
        deleteButton = (AppCompatImageButton) findViewById(R.id.vault_media_content_delete_button);
        loadingProgress = (ProgressBar) findViewById(R.id.vault_media_content_activity_progress);
        loadingText = (TextView) findViewById(R.id.vault_media_content_activity_load_text);
        toolbar = (Toolbar) findViewById(R.id.vault_media_content_activity_tool_bar);
        setBucketId(getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY));
        String albumName = getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_NAME_KEY);
        setMediaType(getIntent().getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY));
        setLoadingText();
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(albumName);
        setBottomBarAnimation();
        setListeners();
        measureImageView();
        initLoaders();
    }

    void setLoadingText(){
        switch (getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                loadingText.setText(R.string.vault_album_picker_load_image_text);
                return;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                loadingText.setText(R.string.vault_album_picker_load_video_text);
                return;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                loadingText.setText(R.string.vault_album_picker_load_audio_text);
        }
    }

    void setBucketId(String buckId){
        this.bucketId = buckId;
    }

    void setItemSize(int size){
        this.itemSize = size;
    }

    int getItemSize(){
        return this.itemSize;
    }

    void setNoOfColumns(int noOfColumns){
        this.noOfColumns = noOfColumns;
    }

    int getNoOfColumns(){
        return this.noOfColumns;
    }

    void setMediaType(String type){
        this.mediaType = type;
    }

    String getBucketId(){
        return this.bucketId;
    }

    String getMediaType(){
        return this.mediaType;
    }

    void setListeners(){
        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaContentAdapter !=null){
                    mediaContentAdapter.selectedAllImages();
                }
            }
        });

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if(!mediaContentAdapter.getSelectedAll() && mediaContentAdapter.getSelectedMediaIds().isEmpty()){
                  isLockPressed=false;
                  return;
                }
                if (mediaContentAdapter !=null && !isLockPressed){
                    isLockPressed = true;
                    moveOutDialog = new MediaMoveOutDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack(MOVE_OUT_VAULT_MEDIA_TAG);
                    moveOutDialog.show(fragmentTransaction,MOVE_OUT_VAULT_MEDIA_TAG);
                }

            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mediaContentAdapter.getSelectedAll() && mediaContentAdapter.getSelectedMediaIds().isEmpty()){
                    isDeletePressed=false;
                    return;
                }
                if (mediaContentAdapter !=null && !isDeletePressed){
                    isDeletePressed = true;
                    deleteFilesDialog = new MediaDeleteDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack(DELETE_VAULT_MEDIA_TAG);
                    deleteFilesDialog.show(fragmentTransaction,DELETE_VAULT_MEDIA_TAG);
                }

            }
        });

    }

    public void moveMediaFiles(){
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(mediaContentAdapter.getSelectedMediaIds());
        MediaVaultContentAdapter.isLongPressed=false;
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,getBucketId())
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType())
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT));
        shouldTrackUserPresence = false;
    }

    public void deleteMediaFiles(){
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(mediaContentAdapter.getSelectedMediaIds());
        MediaVaultContentAdapter.isLongPressed=false;
        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,getBucketId())
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType())
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT));
        shouldTrackUserPresence = false;
    }

    public void moveMediaCancelled(){
        isLockPressed = false;
    }

    public void deleteMediaCancelled(){
        isDeletePressed = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            bottomBarAnimator.setIntValues(bottomBar.getBottom(), bottomBar.getTop());
        }
    }

    void setBottomBarAnimation(){
        bottomBarAnimator = new ValueAnimator();
        bottomBarAnimator.setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator());
        bottomBarAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                bottomBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        bottomBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bottomBar.setTop((int) animation.getAnimatedValue());
            }
        });
    }

    void measureImageView(){
        Context ctxt = getBaseContext();
        setItemSize(Math.round(getResources().getDimension(R.dimen.vault_album_content_item_size)));
        DisplayMetrics metrics = ctxt.getResources().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;
        setNoOfColumns(displayWidth/ itemSize);
    }

    void startBottomBarAnimation(){
        bottomBarAnimator.start();
    }

    void initLoaders(){
        getSupportLoaderManager().initLoader(42,null,this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id==42){
            String[] mediaProjection = {MediaVaultModel.ID_COLUMN_NAME,MediaVaultModel.VAULT_MEDIA_PATH
                    , MediaVaultModel.ORIGINAL_FILE_NAME,MediaVaultModel.VAULT_BUCKET_ID, MediaVaultModel.VAULT_FILE_NAME
                    ,MediaVaultModel.MEDIA_TYPE,MediaVaultModel.FILE_EXTENSION,MediaVaultModel.THUMBNAIL_PATH};
            String mediaSelection = MediaVaultModel.MEDIA_TYPE+"=?"+" AND "+MediaVaultModel.VAULT_BUCKET_ID+"=?";
            String[] mediaSelectionArgs = {getMediaCursorType(getMediaType()),getBucketId()};
            String mediaOrderBy = MediaVaultModel.VAULT_FILE_NAME+ " DESC";
            loadingStarted();
            return new VaultDbCursorLoader(getBaseContext(),1,mediaProjection,mediaSelection
                                        ,mediaSelectionArgs,mediaOrderBy);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(mediaContentAdapter ==null){
            mediaContentAdapter = new MediaVaultContentAdapter(data,this);
            mediaContentAdapter.setItemSize(getItemSize());
            int itemMargin = Math.round(getResources().getDimension(R.dimen.fiveDpDimension));
            mediaVaultContentRecycler.addItemDecoration(new MediaVaultAlbumDecoration(itemMargin));
            mediaVaultContentRecycler.setAdapter(mediaContentAdapter);
            mediaVaultContentRecycler.setLayoutManager(new GridLayoutManager(getBaseContext(),getNoOfColumns(), GridLayoutManager.VERTICAL,false));
        }else{
            mediaContentAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
            mediaContentAdapter.swapCursor(null);
    }

    public void loadingStarted() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        mediaVaultContentRecycler.setVisibility(View.INVISIBLE);
    }

    public void loadingComplete() {
        try {
            loadingProgress.setVisibility(View.INVISIBLE);
            loadingText.setVisibility(View.INVISIBLE);
            mediaVaultContentRecycler.setVisibility(View.VISIBLE);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    String getMediaCursorType(String media){
        switch(media){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return "image";

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return "video";

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return "audio";
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if(MediaVaultContentAdapter.isLongPressed && mediaContentAdapter !=null){
            mediaContentAdapter.clearAllSelections();
            MediaVaultContentAdapter.isLongPressed = false;
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shouldTrackUserPresence = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity = true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    private WeakReference<MediaVaultContentActivity> getWeakReference(){
        return new WeakReference<>(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        vaultContentScreenOffReceiver = new VaultContentScreenOffReceiver(getWeakReference());
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(vaultContentScreenOffReceiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaContentAdapter!=null){
            mediaContentAdapter.renameVisibleThumbnail();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            if(mediaContentAdapter !=null){
                mediaContentAdapter.closeAdapter();
            }
           finishActivityAffinity();
        }
        if(!shouldTrackUserPresence){
            unregisterReceiver(vaultContentScreenOffReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!shouldCloseAffinity && mediaContentAdapter !=null){
            mediaContentAdapter.closeAdapter();
        }
        if(shouldTrackUserPresence){
            unregisterReceiver(vaultContentScreenOffReceiver);
        }
    }

    private void finishActivityAffinity(){
        Glide.get(this).clearMemory();
        new Thread(new ClearVaultContentCacheTask(getWeakReference())).start();
        finishAffinity();
    }

    static class ClearVaultContentCacheTask implements Runnable
    {
        WeakReference<MediaVaultContentActivity> activityWeakReference;
        ClearVaultContentCacheTask(WeakReference<MediaVaultContentActivity> activityReference){
            this.activityWeakReference = activityReference;
        }

        @Override
        public void run() {
            Log.d("CacheVault","Clear Vault Content Cache Started "+ System.currentTimeMillis());
            Glide.get(activityWeakReference.get()).clearDiskCache();
            Log.d("CacheVault","Clear Vault Content Cache Complete "+ System.currentTimeMillis());
        }
    }

    static class VaultContentScreenOffReceiver extends BroadcastReceiver {

        WeakReference<MediaVaultContentActivity> activity;
        VaultContentScreenOffReceiver(WeakReference<MediaVaultContentActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishActivityAffinity();
            }
        }
    }
}

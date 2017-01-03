package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.MediaMoveInDialog;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 22-09-2016.
 */

public class MediaPickerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    static final String MOVE_IN_DIALOG_TAG ="media_move_in_dialog";

    private RecyclerView mediaPickerRecycler;
    private int noOfColumns, itemSize;
    private String bucketId, mediaType;
    private MediaPickerAdapter mediaPickerAdapter;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private AppCompatImageButton selectAllButton,lockButton;
    private RelativeLayout bottomBar;
    private ValueAnimator bottomBarAnimator;
    private boolean isLockPressed;
    private DialogFragment moveInDialog;
    private Toolbar toolbar;
    private boolean shouldTrackUserPresence, shouldCloseAffinity;
    private ContentPickerScreenOffReceiver contentPickerScreenOffReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldTrackUserPresence = true;
        setContentView(R.layout.vault_media_picker_activity);
        mediaPickerRecycler = (RecyclerView) findViewById(R.id.vault_media_picker_recycler);
        bottomBar = (RelativeLayout) findViewById(R.id.vault_media_picker_bottom_bar);
        selectAllButton = (AppCompatImageButton) findViewById(R.id.vault_media_picker_select_all);
        lockButton = (AppCompatImageButton) findViewById(R.id.vault_media_picker_lock);
        loadingText = (TextView) findViewById(R.id.vault_media_picker_activity_load_text);
        loadingProgress = (ProgressBar) findViewById(R.id.vault_media_picker_activity_progress);
        toolbar = (Toolbar) findViewById(R.id.vault_media_picker_activity_tool_bar);
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
        getSupportLoaderManager().initLoader(23,null,this);
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

    void setListeners(){
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPickerAdapter !=null){
                    mediaPickerAdapter.selectedAllImages();
                }
            }
        });

        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mediaPickerAdapter.getSelectedAll() && mediaPickerAdapter.getSelectedMediaIds().isEmpty()){
                    isLockPressed=false;
                    return;
                }
                if (mediaPickerAdapter !=null && !isLockPressed){
                    isLockPressed = true;
                    moveInDialog = new MediaMoveInDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack(MOVE_IN_DIALOG_TAG);
                    moveInDialog.show(fragmentTransaction,MOVE_IN_DIALOG_TAG);
                }

            }
        });

    }

    public void moveMediaCancelled(){
        isLockPressed = false;
    }

    public void moveMediaFiles(){
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        selectedMediaModel.setSelectedMediaIdList(mediaPickerAdapter.getSelectedMediaIds());

        startActivity(new Intent(this,MediaMoveActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,getBucketId())
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType())
                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT));
        shouldTrackUserPresence = false;
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            bottomBarAnimator.setIntValues(bottomBar.getBottom(),bottomBar.getTop());
        }
    }

    void startBottomBarAnimation(){
        bottomBarAnimator.start();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id == 23){
            loadingStarted();
          return getMediaCursorLoader(getMediaType());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(mediaPickerAdapter ==null){
            mediaPickerAdapter = new MediaPickerAdapter(data,this);
            mediaPickerAdapter.setItemSize(getItemSize());
            mediaPickerRecycler.setAdapter(mediaPickerAdapter);
            int itemMargin = Math.round(getResources().getDimension(R.dimen.fiveDpDimension));
            mediaPickerRecycler.addItemDecoration(new MediaVaultAlbumDecoration(itemMargin));
            mediaPickerRecycler.setLayoutManager(new GridLayoutManager(getBaseContext(),getNoOfColumns(), GridLayoutManager.VERTICAL,false));
        }else{
               mediaPickerAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mediaPickerAdapter.swapCursor(null);
    }

    public void loadingStarted() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        mediaPickerRecycler.setVisibility(View.INVISIBLE);
    }

    public void loadingComplete() {
        try {
            loadingProgress.setVisibility(View.INVISIBLE);
            loadingText.setVisibility(View.INVISIBLE);
            mediaPickerRecycler.setVisibility(View.VISIBLE);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    CursorLoader getMediaCursorLoader(String media){
        switch(media){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                String[] imageProjection = {MediaStore.Images.Media._ID,MediaStore.Images.Media.BUCKET_ID
                        , MediaStore.Images.Media.DATE_ADDED};
                String imageSelection = MediaStore.Images.Media.BUCKET_ID+"=?";
                String[] imageSelectionArgs = {getBucketId()};
                String imageOrderBy = MediaStore.Images.Media.DATE_ADDED+ " DESC";
                return   new CursorLoader(getBaseContext(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,imageProjection,imageSelection,imageSelectionArgs,imageOrderBy);

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                String[] videoProjection = {MediaStore.Video.Media._ID,MediaStore.Video.Media.BUCKET_ID
                        , MediaStore.Video.Media.DATE_ADDED};
                String videoSelection = MediaStore.Video.Media.BUCKET_ID+"=?";
                String[] videoSelectionArgs = {getBucketId()};
                String videoOrderBy = MediaStore.Video.Media.DATE_ADDED+ " DESC";
                return   new CursorLoader(getBaseContext(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,videoProjection,videoSelection,videoSelectionArgs,videoOrderBy);

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                String[] audioProjection = {MediaStore.Audio.Media._ID,MediaStore.Audio.Media.ALBUM_ID};
                String audioSelection = MediaStore.Audio.Media.ALBUM_ID+"=?";
                String[] audioSelectionArgs = {getBucketId()};
                String audioOrderBy = MediaStore.Audio.Media._ID;
                return   new CursorLoader(getBaseContext(),
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,audioProjection,audioSelection,audioSelectionArgs,audioOrderBy);
        }
        return null;
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

    @Override
    protected void onStart() {
        super.onStart();
        contentPickerScreenOffReceiver = new ContentPickerScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(contentPickerScreenOffReceiver,filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            if(mediaPickerAdapter !=null){
                mediaPickerAdapter.closeAdapter();
            }
            finishAffinity();
        }
        if(!shouldTrackUserPresence){
            unregisterReceiver(contentPickerScreenOffReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        if(mediaPickerAdapter !=null && !mediaPickerAdapter.getSelectedMediaIds().isEmpty()){
            mediaPickerAdapter.clearAllSelections();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!shouldCloseAffinity && mediaPickerAdapter !=null){
            mediaPickerAdapter.closeAdapter();
        }
        if(shouldTrackUserPresence){
            unregisterReceiver(contentPickerScreenOffReceiver);
        }
    }

    static class ContentPickerScreenOffReceiver extends BroadcastReceiver {

        WeakReference<MediaPickerActivity> activity;
        ContentPickerScreenOffReceiver(WeakReference<MediaPickerActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishAffinity();
            }
        }
    }
}

package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
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
import android.widget.RelativeLayout;

import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by RAAJA on 22-09-2016.
 */

public class MediaPickerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mediaPickerRecycler;
    private int noOfColumns, itemSize;
    private String bucketId, mediaType;
    private MediaPickerAdapter mediaPickerAdapter;
    private AppCompatImageButton selectAllButton,lockButton;
    private ArrayList<String> selectedMediaId;
    private RelativeLayout bottomBar;
    private ValueAnimator bottomBarAnimator;
    private boolean isLockPressed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_media_picker_activity);
        mediaPickerRecycler = (RecyclerView) findViewById(R.id.vault_media_picker_recycler);
        bottomBar = (RelativeLayout) findViewById(R.id.vault_media_picker_bottom_bar);
        bottomBar.setVisibility(View.GONE);
        selectAllButton = (AppCompatImageButton) findViewById(R.id.vault_media_picker_select_all);
        lockButton = (AppCompatImageButton) findViewById(R.id.vault_media_picker_lock);
        Toolbar toolbar = (Toolbar) findViewById(R.id.vault_media_picker_activity_tool_bar);
        setBucketId(getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY));
        String albumName = getIntent().getStringExtra(MediaAlbumPickerActivity.ALBUM_NAME_KEY);
        setMediaType(getIntent().getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY));
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

    void setListeners(){
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
                if(!mediaPickerAdapter.getSelectedAll() && !mediaPickerAdapter.getSelectedMediaIds().isEmpty()){
                    isLockPressed=false;
                }
                if (mediaPickerAdapter !=null && !isLockPressed){
                    isLockPressed = true;
                    if(mediaPickerAdapter.getSelectedAll()){
                        startActivity(new Intent(getBaseContext(),MediaMoveActivity.class)
                                .putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL)
                                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,getBucketId())
                                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType())
                                .putExtra(MediaAlbumPickerActivity.SELECTED_FILE_COUNT_KEY,mediaPickerAdapter.mediaCursor.getCount())
                                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT)
                                .putExtra(MediaMoveActivity.SERVICE_START_TYPE_KEY,MediaMoveActivity.SERVICE_START_TYPE_NEW)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }

                    if(!mediaPickerAdapter.getSelectedAll() && !mediaPickerAdapter.getSelectedMediaIds().isEmpty()){
                        selectedMediaId = mediaPickerAdapter.getSelectedMediaIds();
                        String[] mediaIdArray = new String[selectedMediaId.size()];
                        String[] mediaId =selectedMediaId.toArray(mediaIdArray);
                        startActivity(new Intent(getBaseContext(),MediaMoveActivity.class)
                                .putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE)
                                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,getBucketId())
                                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType())
                                .putExtra(MediaAlbumPickerActivity.SELECTED_FILE_COUNT_KEY,mediaId.length)
                                .putExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY,mediaId)
                                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT)
                                .putExtra(MediaMoveActivity.SERVICE_START_TYPE_KEY,MediaMoveActivity.SERVICE_START_TYPE_NEW)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }
                }

            }
        });

    }

    void setBottomBarAnimation(){
        bottomBarAnimator = ValueAnimator.ofInt(0,1);
        bottomBarAnimator.setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator());
        bottomBarAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                bottomBar.setScaleY(0);
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
                Log.d("Vault",animation.getAnimatedValue() + "");
                bottomBar.setScaleY((int) animation.getAnimatedValue());
            }
        });
    }

    void measureImageView(){
        Context ctxt = getBaseContext();
        setItemSize(Math.round(DimensionConverter.convertDpToPixel(113,ctxt)));
        DisplayMetrics metrics = ctxt.getResources().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;
        setNoOfColumns(displayWidth/ itemSize);
    }

    void startBottomBarAnimation(){
        bottomBarAnimator.start();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id == 23){
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
            mediaPickerRecycler.setLayoutManager(new GridLayoutManager(getBaseContext(),getNoOfColumns(), GridLayoutManager.VERTICAL,false));
        }else{
               mediaPickerAdapter.swapCursor(data);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mediaPickerAdapter.swapCursor(null);
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
                String[] audioProjection = {MediaStore.Audio.Media._ID,MediaStore.Audio.Media.ALBUM_KEY};
                String audioSelection = MediaStore.Audio.Media.ALBUM_KEY+"=?";
                String[] audioSelectionArgs = {getBucketId()};
                String audioOrderBy = MediaStore.Audio.Media._ID;
                return   new CursorLoader(getBaseContext(),
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,audioProjection,audioSelection,audioSelectionArgs,audioOrderBy);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPickerAdapter !=null){
            mediaPickerAdapter.closeAdapter();
        }
    }
}

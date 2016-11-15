package com.smartfoxitsolutions.lockup.mediavault;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;

/**
 * Created by RAAJA on 04-11-2016.
 */

public class MediaVaultContentActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    static String SELECTED_MEDIA_FILE_KEY = "selected_media_file";

    private RecyclerView mediaVaultContentRecycler;
    private int noOfColumns, itemSize;
    private String bucketId, mediaType;
    private MediaVaultContentAdapter mediaContentAdapter;
    private AppCompatImageButton selectAllButton,unlockButton;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private ArrayList<String> selectedMediaId;
    private RelativeLayout bottomBar;
    private ValueAnimator bottomBarAnimator;
    private boolean isLockPressed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_media_content_activity);
        mediaVaultContentRecycler = (RecyclerView) findViewById(R.id.vault_media_content_recycler);
        bottomBar = (RelativeLayout) findViewById(R.id.vault_media_content_bottom_bar);
        bottomBar.setVisibility(View.GONE);
        selectAllButton = (AppCompatImageButton) findViewById(R.id.vault_media_content_select_all);
        unlockButton = (AppCompatImageButton) findViewById(R.id.vault_media_content_unlock);
        loadingProgress = (ProgressBar) findViewById(R.id.vault_media_content_activity_progress);
        loadingText = (TextView) findViewById(R.id.vault_media_content_activity_load_text);
        Toolbar toolbar = (Toolbar) findViewById(R.id.vault_media_content_activity_tool_bar);
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

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            /*    if(!mediaContentAdapter.getSelectedAll() && !mediaContentAdapter.getSelectedMediaIds().isEmpty()){
                    isLockPressed=false;
                }
                if (mediaContentAdapter !=null && !isLockPressed){
                    isLockPressed = true;
                    if(mediaContentAdapter.getSelectedAll()){
                        startActivity(new Intent(getBaseContext(),MediaMoveActivity.class)
                                .putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL)
                                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,getBucketId())
                                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType())
                                .putExtra(MediaAlbumPickerActivity.SELECTED_FILE_COUNT_KEY,mediaContentAdapter.mediaCursor.getCount())
                                .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT)
                                .putExtra(MediaMoveActivity.SERVICE_START_TYPE_KEY,MediaMoveActivity.SERVICE_START_TYPE_NEW)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }

                    if(!mediaContentAdapter.getSelectedAll() && !mediaContentAdapter.getSelectedMediaIds().isEmpty()){
                        selectedMediaId = mediaContentAdapter.getSelectedMediaIds();
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
                } */

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
            return new VaultDbCursorLoader(getApplicationContext(),1,mediaProjection,mediaSelection
                                        ,mediaSelectionArgs,mediaOrderBy);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(mediaContentAdapter ==null){
            mediaContentAdapter = new MediaVaultContentAdapter(data,this);
            mediaContentAdapter.setItemSize(getItemSize());
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
        super.onBackPressed();
        if(MediaVaultContentAdapter.isLongPressed && mediaContentAdapter !=null){
            mediaContentAdapter.clearAllSelections();
            MediaVaultContentAdapter.isLongPressed = false;
            return;
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaContentAdapter!=null){
            mediaContentAdapter.renameVisibleThumbnail();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaContentAdapter !=null){
            mediaContentAdapter.closeAdapter();
        }
    }
}

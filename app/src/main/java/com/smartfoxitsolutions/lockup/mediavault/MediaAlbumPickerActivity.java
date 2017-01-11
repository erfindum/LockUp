package com.smartfoxitsolutions.lockup.mediavault;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.smartfoxitsolutions.lockup.R;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class MediaAlbumPickerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String MEDIA_TYPE_KEY = "media_type";
    public static final String TYPE_IMAGE_MEDIA = "type_image_media";
    public static final String TYPE_VIDEO_MEDIA = "type_video_media";
    public static final String TYPE_AUDIO_MEDIA = "type_audio_media";
    public static final String ALBUM_NAME_KEY = "album_name_key";
    public static final String ALBUM_BUCKET_ID_KEY = "album_bucket_id";
    public static final String SELECTED_MEDIA_FILES_KEY = "selected_media_files";
    public static final String SELECTED_FILE_COUNT_KEY = "selected_file_count";

    private RecyclerView mediaPickerBuckRecycler;
    private MediaAlbumPickerAdapter mediaAdapter;
    private TextView loadingText;
    private ProgressBar loadingProgress;
    int viewWidth, viewHeight,noOfColumns;
    private String mediaType;
    private boolean shouldTrackUserPresence, shouldCloseAffinity;
    private AlbumPickerScreenOffReceiver albumPickerScreenOffReceiver;
    private AppCompatImageView emptyPlaceholder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_album_picker_activity);
        shouldTrackUserPresence = true;
        Toolbar toolbar = (Toolbar) findViewById(R.id.vault_album_picker_activity_tool_bar);
        mediaPickerBuckRecycler = (RecyclerView)findViewById(R.id.vault_album_picker_activity_recycler);
        loadingProgress = (ProgressBar)findViewById(R.id.vault_album_picker_activity_progress);
        loadingText = (TextView)findViewById(R.id.vault_album_picker_activity_load_text);
        emptyPlaceholder = (AppCompatImageView) findViewById(R.id.vault_album_picker_activity_placeholder);
        loadingText.setText(R.string.vault_album_picker_load_text);
        setMediaType(getIntent().getStringExtra(MEDIA_TYPE_KEY));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.vault_album_picker_select_album_toolbar);
        measureItemView();
        getSupportLoaderManager().initLoader(20,null,this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    void setMediaType(String media){
        this.mediaType = media;
    }

    String getMediaType(){
        return this.mediaType;
    }

    void measureItemView(){
        Context ctxt = getBaseContext();
        viewWidth = Math.round(getResources().getDimension(R.dimen.vault_album_thumbnail_width));
        viewHeight =Math.round(getResources().getDimension(R.dimen.vault_album_thumbnail_height));
        int itemWidth = Math.round(getResources().getDimension(R.dimen.vault_album_item_size));
        DisplayMetrics metrics = ctxt.getResources().getDisplayMetrics();
        int  displayWidth = metrics.widthPixels;
        noOfColumns = displayWidth/itemWidth;
    }

    private WeakReference<MediaAlbumPickerActivity> getWeakReference(){
        return new WeakReference<>(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        albumPickerScreenOffReceiver = new AlbumPickerScreenOffReceiver(getWeakReference());
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(albumPickerScreenOffReceiver,filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().setTitle(R.string.vault_album_picker_select_album_toolbar);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id == 20){
            return getMediaCursorLoader(getMediaType());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(mediaAdapter == null){
            mediaAdapter = new MediaAlbumPickerAdapter(data,this
                    , viewWidth, viewHeight);
            mediaPickerBuckRecycler.setAdapter(mediaAdapter);
            int itemMargin = Math.round(getResources().getDimension(R.dimen.tenDpDimension));
            mediaPickerBuckRecycler.addItemDecoration(new MediaVaultAlbumDecoration(itemMargin));
            mediaPickerBuckRecycler.setLayoutManager(new GridLayoutManager(getBaseContext()
                    ,noOfColumns,GridLayoutManager.VERTICAL,false));
            if(data.getCount()<=0){
                loadEmptyPlaceholder();
            }
        }else{
            if(data.getCount()<=0){
                loadEmptyPlaceholder();
            }
            mediaAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mediaAdapter !=null){
            mediaAdapter.swapCursor(null);
        }
    }

    CursorLoader getMediaCursorLoader(String media){
        switch(media){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                String[] imageProjection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Images.Media.BUCKET_ID};
                String imageOrderBy = MediaStore.Images.Media.BUCKET_ID +" DESC";
                return   new CursorLoader(getBaseContext(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,imageProjection,null,null,imageOrderBy);

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                String[] videoProjection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Video.Media.BUCKET_ID};
                String videoOrderBy = MediaStore.Video.Media.BUCKET_ID + " DESC";
                    return   new CursorLoader(getBaseContext(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,videoProjection,null,null,videoOrderBy);


            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                String[] audioProjection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM
                        ,MediaStore.Audio.Media.ALBUM_ID};
                String audioOrderBy = MediaStore.Audio.Media.ALBUM_ID + " DESC";
                    return   new CursorLoader(getBaseContext(),
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,audioProjection,null,null,audioOrderBy);
        }
        return null;
    }

    public void loadingStarted() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        mediaPickerBuckRecycler.setVisibility(View.INVISIBLE);
        emptyPlaceholder.setVisibility(View.INVISIBLE);
    }

    public void loadingComplete() {
        loadingProgress.setVisibility(View.INVISIBLE);
        loadingText.setVisibility(View.INVISIBLE);
        mediaPickerBuckRecycler.setVisibility(View.VISIBLE);
    }

    void loadEmptyPlaceholder(){
        emptyPlaceholder.setImageResource(R.drawable.ic_vault_placeholder);
        emptyPlaceholder.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText(getResources().getString(R.string.vault_album_picker_load_failed_external_text));
    }

    public void albumClicked(String albumId, String albumName) {
        startActivity(new Intent(getBaseContext(),MediaPickerActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumId)
                .putExtra(MediaAlbumPickerActivity.ALBUM_NAME_KEY,albumName)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType()));
        shouldTrackUserPresence = false;
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
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            if(mediaAdapter!=null){
                mediaAdapter.closeResources();
                mediaAdapter = null;
            }
            finishActivityAffinity();
        }
        if(!shouldTrackUserPresence){
            unregisterReceiver(albumPickerScreenOffReceiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!shouldCloseAffinity && mediaAdapter !=null){
            mediaAdapter.closeResources();
            mediaAdapter = null;
        }
        if(shouldTrackUserPresence){
            unregisterReceiver(albumPickerScreenOffReceiver);
        }
    }

    private void finishActivityAffinity(){
        Glide.get(this).clearMemory();
        new Thread(new ClearAlbumPickerCacheTask(getWeakReference())).start();
        finishAffinity();
    }

    static class ClearAlbumPickerCacheTask implements Runnable
    {
        WeakReference<MediaAlbumPickerActivity> activityWeakReference;
        ClearAlbumPickerCacheTask(WeakReference<MediaAlbumPickerActivity> activityReference){
            this.activityWeakReference = activityReference;
        }

        @Override
        public void run() {
            Log.d("CacheVault","Clear Album Picker Cache Started "+ System.currentTimeMillis());
            Glide.get(activityWeakReference.get()).clearDiskCache();
            Log.d("CacheVault","Clear Album Picker Cache Complete "+ System.currentTimeMillis());
        }
    }

    static class AlbumPickerScreenOffReceiver extends BroadcastReceiver {

        WeakReference<MediaAlbumPickerActivity> activity;
        AlbumPickerScreenOffReceiver(WeakReference<MediaAlbumPickerActivity> activity){
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

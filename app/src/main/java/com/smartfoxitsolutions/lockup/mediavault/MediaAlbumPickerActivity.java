package com.smartfoxitsolutions.lockup.mediavault;

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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.R;

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

    private RecyclerView mediaPickerBuckRecycler;
    private MediaAlbumPickerAdapter mediaAdapter;
    private TextView loadingText;
    private ProgressBar loadingProgress;
    int viewWidth, viewHeight,noOfColumns;
    private int storageKind;
    private String mediaType;
    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_album_picker_activity);
        toolbar = (Toolbar) findViewById(R.id.vault_album_picker_activity_tool_bar);
        mediaPickerBuckRecycler = (RecyclerView)findViewById(R.id.vault_album_picker_activity_recycler);
        loadingProgress = (ProgressBar)findViewById(R.id.vault_album_picker_activity_progress);
        loadingText = (TextView)findViewById(R.id.vault_album_picker_activity_load_text);
        setMediaType(getIntent().getStringExtra(MEDIA_TYPE_KEY));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        measureItemView();
        getSupportLoaderManager().initLoader(20,null,this);
    }

    void setMediaType(String media){
        this.mediaType = media;
    }

    String getMediaType(){
        return this.mediaType;
    }

    void measureItemView(){
        Context ctxt = getBaseContext();
        viewWidth = Math.round(DimensionConverter.convertDpToPixel(155,ctxt));
        viewHeight = Math.round(DimensionConverter.convertDpToPixel(115,ctxt));
        int itemWidth = Math.round(DimensionConverter.convertDpToPixel(165,ctxt));
        DisplayMetrics metrics = ctxt.getResources().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;
        noOfColumns = displayWidth/itemWidth;
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
            mediaPickerBuckRecycler.setLayoutManager(new GridLayoutManager(getBaseContext()
                    ,noOfColumns,GridLayoutManager.VERTICAL,false));
        }else{
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
                String imageOrderBy = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " COLLATE NOCASE";
                return   new CursorLoader(getBaseContext(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,imageProjection,null,null,imageOrderBy);

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                String[] videoProjection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Video.Media.BUCKET_ID};
                String videoOrderBy = MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " COLLATE NOCASE";
                    return   new CursorLoader(getBaseContext(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,videoProjection,null,null,videoOrderBy);


            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                String[] audioProjection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM
                        ,MediaStore.Audio.Media.ALBUM_KEY};
                String audioOrderBy = MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE";
                    return   new CursorLoader(getBaseContext(),
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,audioProjection,null,null,audioOrderBy);
        }
        return null;
    }

    public void loadingStarted() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        mediaPickerBuckRecycler.setVisibility(View.INVISIBLE);
    }

    public void loadingComplete() {
        loadingProgress.setVisibility(View.INVISIBLE);
        loadingText.setVisibility(View.INVISIBLE);
        mediaPickerBuckRecycler.setVisibility(View.VISIBLE);
    }

    public void albumClicked(String albumId, String albumName) {
        startActivity(new Intent(getBaseContext(),MediaPickerActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumId)
                .putExtra(MediaAlbumPickerActivity.ALBUM_NAME_KEY,albumName)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaAdapter !=null){
            mediaAdapter.closeResources();
        }
    }

}

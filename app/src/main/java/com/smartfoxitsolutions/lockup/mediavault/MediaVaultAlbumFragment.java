package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 01-10-2016.
 */

public class MediaVaultAlbumFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mediaVaultBuckRecycler;
    private MediaVaultAlbumAdapter mediaAdapter;
    private TextView loadingText;
    private ProgressBar loadingProgress;
    private AppCompatImageView emptyPlaceholder;
    int viewWidth, viewHeight,noOfColumns;
    private String mediaType;
    private MediaVaultAlbumActivity activity;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.vault_album_fragment,container,false);
        mediaVaultBuckRecycler = (RecyclerView) parent.findViewById(R.id.vault_fragment_recycler);
        loadingProgress = (ProgressBar)parent.findViewById(R.id.vault_fragment_progress);
        loadingText = (TextView)parent.findViewById(R.id.vault_fragment_load_text);
        emptyPlaceholder = (AppCompatImageView) parent.findViewById(R.id.vault_fragment_placeholder);
        if(savedInstanceState!=null){
            setMediaType(savedInstanceState.getString(MediaAlbumPickerActivity.MEDIA_TYPE_KEY));
        }
        loadingText.setText(R.string.vault_album_picker_load_text);
        return parent;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MediaVaultAlbumActivity) getActivity();
        measureItemView();
        initLoader(getMediaType());
    }

    void initLoader(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                activity.getSupportLoaderManager().initLoader(38,null,this);
                return;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                activity.getSupportLoaderManager().initLoader(39,null,this);
                return;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                activity.getSupportLoaderManager().initLoader(40,null,this);
        }
    }

    void setMediaType(String media){
        this.mediaType = media;
    }

    String getMediaType(){
        return this.mediaType;
    }

    void measureItemView(){
        Context ctxt = activity.getBaseContext();
        viewWidth = Math.round(DimensionConverter.convertDpToPixel(155,ctxt));
        viewHeight = Math.round(DimensionConverter.convertDpToPixel(115,ctxt));
        int itemWidth = Math.round(DimensionConverter.convertDpToPixel(165,ctxt));
        DisplayMetrics metrics = ctxt.getResources().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;
        noOfColumns = displayWidth/itemWidth;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id == 38){
            String[] mediaProjection = {MediaVaultModel.THUMBNAIL_PATH, MediaVaultModel.VAULT_BUCKET_NAME
                    ,MediaVaultModel.VAULT_BUCKET_ID};
            String selection = MediaVaultModel.MEDIA_TYPE+"=?";
            String[] selectionArgs = {getMediaCursorType(getMediaType())};
            String mediaOrderBy = MediaVaultModel.VAULT_BUCKET_NAME + " COLLATE NOCASE";
            return   new VaultDbCursorLoader(getContext(),1,mediaProjection
                    ,selection,selectionArgs,mediaOrderBy);
        }
        if(id == 39){
            String[] mediaProjection = {MediaVaultModel.THUMBNAIL_PATH, MediaVaultModel.VAULT_BUCKET_NAME
                    ,MediaVaultModel.VAULT_BUCKET_ID};
            String selection = MediaVaultModel.MEDIA_TYPE+"=?";
            String[] selectionArgs = {getMediaCursorType(getMediaType())};
            String mediaOrderBy = MediaVaultModel.VAULT_BUCKET_NAME + " COLLATE NOCASE";
            return   new VaultDbCursorLoader(getContext(),1,mediaProjection
                    ,selection,selectionArgs,mediaOrderBy);
        }
        if(id == 40){
            String[] mediaProjection = {MediaVaultModel.THUMBNAIL_PATH, MediaVaultModel.VAULT_BUCKET_NAME
                    ,MediaVaultModel.VAULT_BUCKET_ID};
            String selection = MediaVaultModel.MEDIA_TYPE+"=?";
            String[] selectionArgs = {getMediaCursorType(getMediaType())};
            String mediaOrderBy = MediaVaultModel.VAULT_BUCKET_NAME + " COLLATE NOCASE";
            return   new VaultDbCursorLoader(getContext(),1,mediaProjection
                    ,selection,selectionArgs,mediaOrderBy);
        }
        loadingStarted();
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(mediaAdapter == null){
            mediaAdapter = new MediaVaultAlbumAdapter(data,this
                    , viewWidth, viewHeight);
            mediaVaultBuckRecycler.setAdapter(mediaAdapter);
            mediaVaultBuckRecycler.setLayoutManager(new GridLayoutManager(activity.getBaseContext()
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

    void loadEmptyPlaceholder(){
        switch(getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                emptyPlaceholder.setImageResource(R.drawable.ic_vault_image_placeholder_empty);
                loadingText.setVisibility(View.VISIBLE);
                loadingText.setText(getResources().getString(R.string.vault_album_picker_load_failed_image_text));


            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                emptyPlaceholder.setImageResource(R.drawable.ic_vault_video_placeholder_empty);
                loadingText.setVisibility(View.VISIBLE);
                loadingText.setText(getResources().getString(R.string.vault_album_picker_load_failed_video_text));


            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                emptyPlaceholder.setImageResource(R.drawable.ic_vault_audio_placeholder_empty);
                loadingText.setVisibility(View.VISIBLE);
                loadingText.setText(getResources().getString(R.string.vault_album_picker_load_failed_audio_text));

        }

    }

    public void loadingStarted() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText(getResources().getString(R.string.vault_album_picker_load_text));
        mediaVaultBuckRecycler.setVisibility(View.INVISIBLE);
    }

    public void loadingComplete() {
        loadingProgress.setVisibility(View.INVISIBLE);
        loadingText.setVisibility(View.INVISIBLE);
        mediaVaultBuckRecycler.setVisibility(View.VISIBLE);
    }

    public void albumClicked(String albumId, String albumName) {
        startActivity(new Intent(getActivity().getBaseContext(),MediaVaultContentActivity.class)
                .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumId)
                .putExtra(MediaAlbumPickerActivity.ALBUM_NAME_KEY,albumName)
                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,getMediaType()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mediaAdapter!=null){
            mediaAdapter.renameVisibleThumbnail();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeFragment();
    }

    void closeFragment(){
        if(mediaAdapter !=null){
            mediaAdapter.swapCursor(null);
            mediaAdapter.closeResources();
            mediaAdapter = null;
        }
        if(activity!=null){
            activity =  null;
        }
    }
}

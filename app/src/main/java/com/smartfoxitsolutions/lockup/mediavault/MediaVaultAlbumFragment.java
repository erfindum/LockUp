package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mopub.nativeads.MoPubRecyclerAdapter;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.ViewBinder;
import com.smartfoxitsolutions.lockup.AppLoaderActivity;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.R;

import static android.content.Context.MODE_PRIVATE;

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
    private MoPubRecyclerAdapter moPubAdapter;


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
        viewWidth = Math.round(getResources().getDimension(R.dimen.vault_album_thumbnail_width));
        viewHeight = Math.round(getResources().getDimension(R.dimen.vault_album_thumbnail_height));
        int itemWidth = Math.round(getResources().getDimension(R.dimen.vault_album_item_size));
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
            String mediaOrderBy = MediaVaultModel.VAULT_BUCKET_ID + " COLLATE NOCASE";
            return   new VaultDbCursorLoader(activity.getBaseContext(),1,mediaProjection
                    ,selection,selectionArgs,mediaOrderBy);
        }
        if(id == 39){
            String[] mediaProjection = {MediaVaultModel.THUMBNAIL_PATH, MediaVaultModel.VAULT_BUCKET_NAME
                    ,MediaVaultModel.VAULT_BUCKET_ID};
            String selection = MediaVaultModel.MEDIA_TYPE+"=?";
            String[] selectionArgs = {getMediaCursorType(getMediaType())};
            String mediaOrderBy = MediaVaultModel.VAULT_BUCKET_ID + " COLLATE NOCASE";
            return   new VaultDbCursorLoader(activity.getBaseContext(),1,mediaProjection
                    ,selection,selectionArgs,mediaOrderBy);
        }
        if(id == 40){
            String[] mediaProjection = {MediaVaultModel.THUMBNAIL_PATH, MediaVaultModel.VAULT_BUCKET_NAME
                    ,MediaVaultModel.VAULT_BUCKET_ID};
            String selection = MediaVaultModel.MEDIA_TYPE+"=?";
            String[] selectionArgs = {getMediaCursorType(getMediaType())};
            String mediaOrderBy = MediaVaultModel.VAULT_BUCKET_ID + " COLLATE NOCASE";
            return   new VaultDbCursorLoader(activity.getBaseContext(),1,mediaProjection
                    ,selection,selectionArgs,mediaOrderBy);
        }
        loadingStarted();
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(mediaAdapter == null){
            int itemMargin = Math.round(getResources().getDimension(R.dimen.tenDpDimension));
            mediaVaultBuckRecycler.addItemDecoration(new MediaVaultAlbumDecoration(itemMargin));
            mediaVaultBuckRecycler.setLayoutManager(new GridLayoutManager(activity.getBaseContext()
                                ,noOfColumns,GridLayoutManager.VERTICAL,false));
            ViewBinder viewBinder = new ViewBinder.Builder(R.layout.vault_ad_view)
                    .titleId(R.id.vault_ad_view_title)
                    .textId(R.id.vault_ad_view_text)
                    .mainImageId(R.id.vault_ad_view_image)
                    .callToActionId(R.id.vault_ad_view_call_to_action)
                    .build();
            MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);
            mediaAdapter = new MediaVaultAlbumAdapter(data,this
                    , viewWidth, viewHeight);
            moPubAdapter = new MoPubRecyclerAdapter(activity,mediaAdapter);
            mediaAdapter.setMoPubAdapter(moPubAdapter);
            moPubAdapter.registerAdRenderer(adRenderer);

            mediaVaultBuckRecycler.setAdapter(moPubAdapter);

            moPubAdapter.loadAds("ea1ea6ae342a4dd295aec2cdd8da905b");
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
        emptyPlaceholder.setImageResource(R.drawable.ic_vault_placeholder);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText(getResources().getString(R.string.vault_album_picker_load_failed_general_text));
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
        activity.shouldTrackUserPresence = false;
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
    public void onStop() {
        super.onStop();
        if(activity.shouldCloseAffinity) {
            closeFragment();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("VaultMain",String.valueOf(activity!=null));
        if(activity!=null && !activity.shouldCloseAffinity){
            closeFragment();
        }
    }

    void closeFragment(){
        if(mediaAdapter !=null){
            mediaAdapter.closeResources();
            mediaAdapter = null;
        }
        if(moPubAdapter!=null){
            moPubAdapter.destroy();
        }
        if(activity!=null){
            activity =  null;
        }
    }
}

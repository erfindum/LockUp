package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Created by RAAJA on 22-09-2016.
 */

public class MediaAlbumPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements OnGridItemSelectedListener{


    private Cursor resultCursor;
    private LinkedList<String> bucketIdList;
    private LinkedList<Integer> bucketCountList;
    private LinkedList<String> bucketThumbnailId;
    private ArrayList<MediaAlbumPickerHolder> holders;
    private LinkedList<String> bucketNameList;
    private int viewWidth, viewHeight;
    private Drawable placeHolder;
    private MediaAlbumPickerActivity mediaAlbumActivity;
    private String mediaType;

    public MediaAlbumPickerAdapter(Cursor cursor,MediaAlbumPickerActivity activity, int viewWidth, int viewHeight) {
        bucketIdList = new LinkedList<>();
        bucketCountList = new LinkedList<>();
        bucketThumbnailId = new LinkedList<>();
        bucketNameList = new LinkedList<>();
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.resultCursor = cursor;
        holders = new ArrayList<>();
        this.mediaAlbumActivity = activity;
        setMediaType(mediaAlbumActivity.getMediaType());
        loadPlaceHolderImages();
        setupAlbumBuckets();
    }

    private void setupAlbumBuckets(){
        mediaAlbumActivity.loadingStarted();
        bucketCountList.clear();
        bucketIdList.clear();
        bucketNameList.clear();
        bucketThumbnailId.clear();
        if(resultCursor!=null && resultCursor.getCount()>=1){
            Log.d("Vault",resultCursor.getCount() + "");
            loadAlbumData(resultCursor);
        }
        mediaAlbumActivity.loadingComplete();
        notifyDataSetChanged();
    }

    private void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    private String getMediaType(){
        return this.mediaType;
    }

    private void loadAlbumData(Cursor cursor){
        cursor.moveToFirst();
        Log.d("VaultAlbum",cursor.getCount() + " Cursor COunt ------");
        String bucketId = "";
        int bucketCount = 0;
        do{
            int bucketIndex = cursor.getColumnIndex(getBucketIndex());
            String bucketIdString = cursor.getString(bucketIndex);
            if(!bucketId.equals(bucketIdString)){
                bucketId = bucketIdString;
                int bucketNameIndex = cursor.getColumnIndex(getBucketNameIndex());
                int firstImageIndex = cursor.getColumnIndex(getIdIndex());
                bucketIdList.add(bucketId);
                bucketNameList.add(cursor.getString(bucketNameIndex));
                bucketThumbnailId.add(cursor.getString(firstImageIndex));
                if(!cursor.isFirst()){
                    bucketCountList.add(bucketCount);
                    Log.d("VaultAlbum",bucketCount+" count");
                    bucketCount = 0;
                }
                Log.d("VaultAlbum",bucketId);
            }
            bucketCount += 1;
        }while (cursor.moveToNext());
        if(bucketIdList.size()==1){
            bucketCountList.add(bucketCount);
            bucketCount = 0;
        }
        if(cursor.isAfterLast()){
            bucketCountList.add(bucketCount);
            bucketCount = 0;
        }
    }

    private String getBucketIndex(){
        switch(getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_ID;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_ID;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM_ID;
        }
        return null;
    }

    private String getBucketNameIndex(){
        switch(getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_DISPLAY_NAME;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_DISPLAY_NAME;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM;
        }
        return null;
    }

    private String getIdIndex(){
        switch(getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media._ID;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media._ID;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media._ID;
        }
        return null;
    }

    private Uri getExternalMediaUri(){
        switch(getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    void swapCursor(Cursor cursor){
        if(cursor!=null){
        resultCursor = cursor;
        setupAlbumBuckets();
        }else{
            resultCursor = null;
            setupAlbumBuckets();
        }
    }

    private void loadPlaceHolderImages(){
        switch(getMediaType()) {
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                placeHolder = ContextCompat.getDrawable(mediaAlbumActivity, R.drawable.ic_vault_image_placeholder);
                return;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                placeHolder = ContextCompat.getDrawable(mediaAlbumActivity, R.drawable.ic_vault_video_placeholder);
                return;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                placeHolder = ContextCompat.getDrawable(mediaAlbumActivity, R.drawable.ic_vault_audio_placeholder);
        }
    }

    private Drawable getPlaceHolderImages(){
        return placeHolder;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctxt = parent.getContext();
        LayoutInflater inflater =  LayoutInflater.from(ctxt);
        View itemView = inflater.inflate(R.layout.vault_album_recycler_item,parent,false);
        MediaAlbumPickerHolder holder = new MediaAlbumPickerHolder(itemView);
        holder.setOnGridItemSelectedListener(this);
        holders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MediaAlbumPickerHolder imageHolder = (MediaAlbumPickerHolder) holder;
            Uri uri;
            uri = Uri.parse(getExternalMediaUri()+"/"+bucketThumbnailId.get(position));
            Log.d("VaultMedia",uri.getPath() + " " + uri.getAuthority() + " " +uri.getScheme());
            switch (getMediaType()){
                case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                    Glide.with(mediaAlbumActivity).load(uri).placeholder(getPlaceHolderImages())
                            .error(getPlaceHolderImages()).override(viewWidth, viewHeight)
                            .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).crossFade()
                            .into(imageHolder.getThumbnailView());
                    break;

                case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                    Glide.with(mediaAlbumActivity).load(uri).asBitmap()
                            .placeholder(getPlaceHolderImages())
                            .error(getPlaceHolderImages()).override(viewWidth, viewHeight)
                            .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .format(DecodeFormat.PREFER_ARGB_8888)
                            .into(imageHolder.getThumbnailView());
                    break;

                case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                    Glide.with(mediaAlbumActivity).load(new AlbumArtModel(uri,mediaAlbumActivity))
                            .placeholder(getPlaceHolderImages())
                            .error(getPlaceHolderImages()).override(viewWidth, viewHeight)
                            .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).crossFade()
                            .into(imageHolder.getThumbnailView());
            }

        imageHolder.getInfoText().setText(bucketNameList.get(position));
        imageHolder.getCountText().setText("(" + bucketCountList.get(position)+")");
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        MediaAlbumPickerHolder mediaHolder= (MediaAlbumPickerHolder) holder;
        Glide.clear(mediaHolder.getThumbnailView());
    }

    @Override
    public void onGridItemSelected(int gridPosition) {
        mediaAlbumActivity.albumClicked(bucketIdList.get(gridPosition),bucketNameList.get(gridPosition));
    }

    @Override
    public int getItemCount() {
        return bucketIdList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    void closeResources(){
        if(mediaAlbumActivity !=null) {
            mediaAlbumActivity = null;
        }
        for (MediaAlbumPickerHolder holder : holders){
            holder.setOnGridItemSelectedListener(null);
        }
    }

}

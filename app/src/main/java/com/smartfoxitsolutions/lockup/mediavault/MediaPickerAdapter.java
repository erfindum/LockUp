package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;

/**
 * Created by RAAJA on 24-09-2016.
 */

public class
MediaPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MediaPickerHolder.OnMediaPickedListener {

    private ArrayList<MediaPickerHolder> holder;
    private ArrayList<String> selectedMediaIds, mediaIdList;
    private MediaPickerActivity activity;
     Cursor mediaCursor;
    private boolean selectedAll,isSelectionStarted;
    private int itemSize;
    private Drawable placeHolder;

     MediaPickerAdapter(Cursor cursor, MediaPickerActivity activity) {
        this.activity = activity;
         this.mediaCursor = cursor;
        holder = new ArrayList<>();
        selectedMediaIds = new ArrayList<>();
         mediaIdList = new ArrayList<>();
        loadPlaceHolderImages();
         loadCursorData();
    }

    void swapCursor(Cursor cursor){
        if(cursor!=null){
            this.mediaCursor = cursor;
            loadCursorData();
        }
        else{
            this.mediaCursor = null;
            notifyDataSetChanged();
        }
    }

    void loadCursorData(){
        mediaCursor.moveToFirst();
        if(mediaCursor.getCount()>0) {
            do {
                int idIndex = mediaCursor.getColumnIndex(getIdIndex());
                mediaIdList.add(mediaCursor.getString(idIndex));

            } while (mediaCursor.moveToNext());
        }
        notifyDataSetChanged();
        activity.loadingComplete();
    }

    void selectedAllImages(){
        if(getSelectedAll()){
            setSelectedAll(false);
            selectDeselectAllImages();
        }else{
            setSelectedAll(true);
            selectDeselectAllImages();
        }
    }

    void setItemSize(int size){
        this.itemSize = size;
    }

    private void setSelectedAll(boolean selected){
        this.selectedAll = selected;
    }

    boolean getSelectedAll(){
        return this.selectedAll;
    }

   private int getItemSize(){
        return this.itemSize;
    }

    ArrayList<String> getSelectedMediaIds(){
        return this.selectedMediaIds;
    }

     String getIdIndex(){
        switch(activity.getMediaType()){
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
        switch(activity.getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctxt = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctxt);
        View itemView  = inflater.inflate(R.layout.vault_media_picker_item,parent,false);
        MediaPickerHolder mediaHolder = new MediaPickerHolder(itemView);
        mediaHolder.setOnMediaPickedListener(this);
        holder.add(mediaHolder);
        return mediaHolder;
    }

    @Override
    public int getItemViewType(int position) {
        super.getItemViewType(position);
        return 0;
    }

    private void loadPlaceHolderImages(){
        switch(activity.getMediaType()) {
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                placeHolder = ContextCompat.getDrawable(activity, R.drawable.ic_vault_image_placeholder);
                return;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                placeHolder = ContextCompat.getDrawable(activity, R.drawable.ic_vault_video_placeholder);
                return;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                placeHolder = ContextCompat.getDrawable(activity, R.drawable.ic_vault_audio_placeholder);
        }
    }

    private Drawable getPlaceHolderImages(){
        return placeHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MediaPickerHolder mediaHolder = (MediaPickerHolder) holder;
        Uri uri;
        uri = Uri.parse(getExternalMediaUri()+"/"+mediaIdList.get(position) );

        switch (activity.getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                Glide.with(activity).load(uri).placeholder(getPlaceHolderImages())
                        .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                        .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).crossFade()
                        .into(mediaHolder.getThumbnailView());
                break;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                Glide.with(activity).load(uri).placeholder(getPlaceHolderImages())
                        .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                        .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).crossFade()
                        .into(mediaHolder.getThumbnailView());
                break;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                Glide.with(activity).load(new AlbumArtModel(uri,activity)).placeholder(getPlaceHolderImages())
                        .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                        .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).crossFade()
                        .into(mediaHolder.getThumbnailView());
        }

        if (!selectedMediaIds.contains(mediaIdList.get(position))) {
            mediaHolder.setItemDeselected();
        } else if (selectedMediaIds.contains(mediaIdList.get(position))) {
            mediaHolder.setItemSelected();
        }

      /*  if(!selectedAll) {

        }else{
            mediaHolder.setItemSelected();
        } */
    }

    @Override
    public int getItemCount() {
        return mediaCursor.getCount();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        MediaPickerHolder currentHolder = (MediaPickerHolder) holder;
        Glide.clear(currentHolder.getThumbnailView());
    }

    private void selectDeselectAllImages(){
        if(selectedAll){
            for(MediaPickerHolder imageHolder:holder){
                imageHolder.setItemSelected();
            }
            selectedMediaIds.clear();
            selectedMediaIds.addAll(mediaIdList);
        }else{
            for(MediaPickerHolder imageHolder:holder){
                imageHolder.setItemDeselected();
            }
            selectedMediaIds.clear();
        }
    }

    void clearAllSelections(){
        if(getSelectedAll()){
            setSelectedAll(false);
        }
        for(MediaPickerHolder mediaHolder:holder){
            mediaHolder.setItemDeselected();
        }
        selectedMediaIds.clear();
    }

    @Override
    public void onMediaPicked(int mediaPosition, MediaPickerHolder holder) {
        if(!isSelectionStarted){
            isSelectionStarted = true;
            activity.startBottomBarAnimation();
        }
        if(!selectedMediaIds.contains(mediaIdList.get(mediaPosition))){
            selectedMediaIds.add(mediaIdList.get(mediaPosition));
            holder.getItemAnimator().start();
        }else{
            selectedMediaIds.remove(mediaIdList.get(mediaPosition));
            holder.setItemDeselected();
        }
    }

    void closeAdapter(){
        for (MediaPickerHolder mediaHolder: holder){
            mediaHolder.setOnMediaPickedListener(null);
        }
        if(activity!=null){
            activity=null;
        }
    }
}

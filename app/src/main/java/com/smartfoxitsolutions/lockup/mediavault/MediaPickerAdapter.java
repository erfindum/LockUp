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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;

/**
 * Created by RAAJA on 24-09-2016.
 */

public class MediaPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MediaPickerHolder.OnMediaPickedListener {

    private ArrayList<MediaPickerHolder> holder;
    private ArrayList<String> selectedMediaIds;
    private MediaPickerActivity activity;
    private Cursor mediaCursor;
    private boolean selectedAll,isSelectionStarted;
    private StringBuilder mediaIdString;
    private int itemSize;
    private Drawable placeHolder;

     MediaPickerAdapter(Cursor cursor, MediaPickerActivity activity) {
        this.activity = activity;
         this.mediaCursor = cursor;
        holder = new ArrayList<>();
        selectedMediaIds = new ArrayList<>();
        mediaIdString = new StringBuilder();
        loadPlaceHolderImages();
    }

    void swapCursor(Cursor cursor){
        if(cursor!=null){
            this.mediaCursor = cursor;
            notifyDataSetChanged();
        }
        else{
            this.mediaCursor = null;
            notifyDataSetChanged();
        }
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
        mediaIdString.delete(0, mediaIdString.length());
        MediaPickerHolder mediaHolder = (MediaPickerHolder) holder;
        mediaCursor.moveToPosition(position);
        int mediaIdIndex = mediaCursor.getColumnIndex(getIdIndex());
        mediaIdString.append(mediaCursor.getString(mediaIdIndex));
        Uri uri;
        uri = Uri.parse(getExternalMediaUri()+"/"+ mediaIdString.toString());

        switch (activity.getMediaType()){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                Glide.with(activity).load(uri).placeholder(getPlaceHolderImages())
                        .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                        .centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).crossFade()
                        .into(mediaHolder.getThumbnailView());
                break;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                Glide.with(activity).load(uri).placeholder(getPlaceHolderImages())
                        .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                        .centerCrop().diskCacheStrategy(DiskCacheStrategy.RESULT).crossFade()
                        .into(mediaHolder.getThumbnailView());
                break;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                Glide.with(activity).load(uri).placeholder(getPlaceHolderImages())
                        .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                        .centerCrop().diskCacheStrategy(DiskCacheStrategy.RESULT).crossFade()
                        .into(mediaHolder.getThumbnailView());
        }
        if(!selectedAll) {
            switch (activity.getMediaType()) {
                case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                    if (!selectedMediaIds.contains(mediaIdString.toString())) {
                        mediaHolder.setItemDeselected();
                    } else if (selectedMediaIds.contains(mediaIdString.toString())) {
                        mediaHolder.setItemSelected();
                    }
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mediaCursor.getCount();
    }

    private void selectDeselectAllImages(){
        if(selectedAll){
            for(MediaPickerHolder imageHolder:holder){
                imageHolder.setItemSelected();
            }
        }else{
            for(MediaPickerHolder imageHolder:holder){
                imageHolder.setItemDeselected();
                selectedMediaIds.clear();
            }
        }
    }

    @Override
    public void onMediaPicked(int mediaPosition, MediaPickerHolder holder) {
        if(!isSelectionStarted){
            isSelectionStarted = true;
            activity.startBottomBarAnimation();
        }
        mediaCursor.moveToPosition(mediaPosition);
        int mediaIdIndex = mediaCursor.getColumnIndex(getIdIndex());
        String mediaId = mediaCursor.getString(mediaIdIndex);
        if(!selectedMediaIds.contains(mediaId)){
            selectedMediaIds.add(mediaId);
            holder.getItemAnimator().start();
        }else{
            selectedMediaIds.remove(mediaId);
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

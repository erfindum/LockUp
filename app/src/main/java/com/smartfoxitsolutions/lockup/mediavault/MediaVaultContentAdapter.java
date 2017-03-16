package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartfoxitsolutions.lockup.R;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by RAAJA on 04-11-2016.
 */

public class MediaVaultContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MediaVaultContentHolder.OnMediaClickedListener {

    Cursor mediaContentCursor;
    private int itemSize;
    private ArrayList<MediaVaultContentHolder> holders;
    private ArrayList<String> selectedMediaIds;
    private MediaVaultContentActivity activity;
    private LinkedList<String> vaultMediaPath, originalFileName, fileExtension, thumbnailPath, vaultIdList;
    private boolean selectedAll,isSelectionStarted;
    private Drawable placeHolder;
    static boolean isLongPressed;

    MediaVaultContentAdapter(Cursor cursor,MediaVaultContentActivity activity){
        this.activity = activity;
        this.mediaContentCursor = cursor;
        holders = new ArrayList<>();
        selectedMediaIds = new ArrayList<>();
        vaultMediaPath = new LinkedList<>();
        originalFileName = new LinkedList<>();
        fileExtension = new LinkedList<>();
        thumbnailPath = new LinkedList<>();
        vaultIdList = new LinkedList<>();
        loadPlaceHolderImages();
        setupContent(mediaContentCursor);
    }

    void swapCursor(Cursor cursor){
        if(cursor!=null){
            this.mediaContentCursor = cursor;
            setupContent(cursor);
        }
        else{
            this.mediaContentCursor = null;
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

    private void setupContent(Cursor mediaCursor){
        activity.loadingStarted();
        mediaCursor.moveToFirst();
        if(mediaCursor.getCount()>0) {
            do {
                int vaultMediaPathIndex = mediaCursor.getColumnIndex(MediaVaultModel.VAULT_MEDIA_PATH);
                int originalFileNameIndex = mediaCursor.getColumnIndex(MediaVaultModel.ORIGINAL_FILE_NAME);
                int fileExtensionIndex = mediaCursor.getColumnIndex(MediaVaultModel.FILE_EXTENSION);
                int thumbnailPathIndex = mediaCursor.getColumnIndex(MediaVaultModel.THUMBNAIL_PATH);
                int vaultIdIndex = mediaCursor.getColumnIndex(MediaVaultModel.ID_COLUMN_NAME);

                vaultIdList.add(mediaCursor.getString(vaultIdIndex));
                vaultMediaPath.add(mediaCursor.getString(vaultMediaPathIndex));
                originalFileName.add(mediaCursor.getString(originalFileNameIndex));
                fileExtension.add(mediaCursor.getString(fileExtensionIndex));
                thumbnailPath.add(mediaCursor.getString(thumbnailPathIndex));
            }
            while (mediaCursor.moveToNext());
        }
        activity.loadingComplete();
    }

    private void selectDeselectAllImages(){
        if(selectedAll){
            for(MediaVaultContentHolder mediaHolder:holders){
                mediaHolder.setItemSelected();
            }
            selectedMediaIds.clear();
            selectedMediaIds.addAll(vaultIdList);
            MediaVaultContentAdapter.isLongPressed=true;
        }else{
            for(MediaVaultContentHolder mediaHolder:holders){
                mediaHolder.setItemDeselected();
            }
            selectedMediaIds.clear();
            MediaVaultContentAdapter.isLongPressed=false;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctxt = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctxt);
        View itemView  = inflater.inflate(R.layout.vault_media_content_item,parent,false);
        MediaVaultContentHolder mediaHolder = new MediaVaultContentHolder(itemView);
        mediaHolder.setOnMediaClickedListener(this);
        holders.add(mediaHolder);
        return mediaHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        mediaContentCursor.moveToPosition(position);
        int vaultFileNameIndex = mediaContentCursor.getColumnIndex(MediaVaultModel.VAULT_FILE_NAME);
        File currentFile = new File(thumbnailPath.get(position));
        File currentRenameFile = new File(thumbnailPath.get(position)+".jpg");
        MediaVaultContentHolder mediaHolder = (MediaVaultContentHolder) holder;
        if(currentFile.exists()) {
            currentFile.renameTo(currentRenameFile);
            Glide.with(activity).load(currentRenameFile).placeholder(getPlaceHolderImages())
                    .error(getPlaceHolderImages()).override(getItemSize(), getItemSize())
                    .centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).crossFade()
                    .into(mediaHolder.getThumbnailView());
        }
        if(isLongPressed){
            if (!selectedMediaIds.contains(mediaContentCursor.getString(vaultFileNameIndex))) {
                mediaHolder.setItemDeselected();
            } else if (selectedMediaIds.contains(mediaContentCursor.getString(vaultFileNameIndex))) {
                mediaHolder.setItemSelected();
            }
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        MediaVaultContentHolder contentHolder= (MediaVaultContentHolder) holder;
        int holderPosition = contentHolder.getLayoutPosition();
            File oldFile = new File(thumbnailPath.get(holderPosition)+".jpg");
            if(oldFile.exists()){oldFile.renameTo(new File(thumbnailPath.get(holderPosition)));}
        Glide.clear(contentHolder.getThumbnailView());

    }

    @Override
    public int getItemCount() {
        return mediaContentCursor.getCount();
    }

    @Override
    public void onMediaClicked(boolean isLongPressed, int mediaPosition, MediaVaultContentHolder holder) {
        if(!isLongPressed){
            if(activity.getMediaType().equals(MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA)){
                VaultImageViewActivity.setOriginalFileNameList(originalFileName);
                VaultImageViewActivity.setVaultFileList(vaultMediaPath);
                VaultImageViewActivity.setVaultIdList(vaultIdList);
                VaultImageViewActivity.setFileExtensionList(fileExtension);
                activity.startActivity(new Intent(activity.getBaseContext(),VaultImageViewActivity.class)
                            .putExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,mediaPosition)
                            .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,activity.getBucketId()));
                activity.shouldTrackUserPresence = false;
            }
            if(activity.getMediaType().equals(MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA)){
                VaultVideoPlayerActivity.setOriginalFileNameList(originalFileName);
                VaultVideoPlayerActivity.setVaultFileList(vaultMediaPath);
                VaultVideoPlayerActivity.setVaultIdList(vaultIdList);
                VaultVideoPlayerActivity.setFileExtensionList(fileExtension);
                activity.startActivity(new Intent(activity.getBaseContext(),VaultVideoPlayerActivity.class)
                            .putExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,mediaPosition)
                            .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,activity.getBucketId()));
                activity.shouldTrackUserPresence = false;
            }
            if(activity.getMediaType().equals(MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA)){
                VaultAudioPlayerActivity.setOriginalFileNameList(originalFileName);
                VaultAudioPlayerActivity.setVaultFileList(vaultMediaPath);
                VaultAudioPlayerActivity.setVaultIdList(vaultIdList);
                VaultAudioPlayerActivity.setFileExtensionList(fileExtension);
                activity.startActivity(new Intent(activity.getBaseContext(),VaultAudioPlayerActivity.class)
                        .putExtra(MediaVaultContentActivity.SELECTED_MEDIA_FILE_KEY,mediaPosition)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,activity.getBucketId()));
                activity.shouldTrackUserPresence = false;
            }

        }
        if(isLongPressed){
            if(!MediaVaultContentAdapter.isLongPressed){
                MediaVaultContentAdapter.isLongPressed = true;
            }
            if(!isSelectionStarted){
                isSelectionStarted = true;
                activity.startBottomBarAnimation();
            }
            String vaultIdString = vaultIdList.get(mediaPosition);
            if(!selectedMediaIds.contains(vaultIdString)){
                selectedMediaIds.add(vaultIdString);
                holder.getItemAnimator().start();
               // Log.d("VaultRename",mediaPosition + " position ...... "+ originalFileName.get(mediaPosition)+ "   filename Added");
            }else{
                selectedMediaIds.remove(vaultIdString);
                if(selectedMediaIds.isEmpty()) {
                    MediaVaultContentAdapter.isLongPressed = false;
                }
                holder.setItemDeselected();
               // Log.d("VaultRename",mediaPosition + " position ...... "+ originalFileName.get(mediaPosition)+ "   filename Removed");
            }
        }
    }

    void clearAllSelections(){
        if(getSelectedAll()){
           setSelectedAll(false);
        }
        for(MediaVaultContentHolder mediaHolder:holders){
            mediaHolder.setItemDeselected();
        }
        selectedMediaIds.clear();
        isLongPressed = false;
    }

    void renameVisibleThumbnail(){
        Log.d("VaultRename", System.currentTimeMillis()+" rename started " + holders.size() + " holder size");
        for (MediaVaultContentHolder holder : holders){
            int holderPosition = holder.getLayoutPosition();
            if(holderPosition!=-1) {
                File oldFile = new File(thumbnailPath.get(holderPosition) + ".jpg");
                if (oldFile.exists()) {
                    oldFile.renameTo(new File(thumbnailPath.get(holderPosition)));
                }
            }
        }
        Log.d("VaultRename", System.currentTimeMillis()+" rename End " + holders.size() + " holder size");
    }

    void closeAdapter(){
        if(activity !=null) {
            activity = null;
        }
        for (MediaVaultContentHolder holder : holders){
            holder.setOnMediaClickedListener(null);
        }
    }
}

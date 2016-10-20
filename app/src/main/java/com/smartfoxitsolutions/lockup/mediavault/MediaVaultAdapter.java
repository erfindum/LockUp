package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.smartfoxitsolutions.lockup.R;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by RAAJA on 15-10-2016.
 */

public class MediaVaultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
                            implements OnGridItemSelectedListener{

    private static final int ITEM_TYPE_ALBUM = 3;
    private static final int ITEM_TYPE_ADS = 4;

    private Cursor resultCursor;
    private LinkedList<String> bucketIdList;
    private LinkedList<Integer> bucketCountList;
    private LinkedList<String> bucketThumbnailId;
    private ArrayList<MediaAlbumPickerHolder> holders;
    private ArrayList<MediaAlbumAdHolder> adHolders;
    private LinkedList<String> bucketNameList;
    private int viewWidth, viewHeight;
    private Drawable placeHolder;
    private MediaVaultFragment mediaAlbumFragment;
    private String mediaType;

    MediaVaultAdapter(Cursor cursor, MediaVaultFragment frag, int viewWidth, int viewHeight){
        bucketIdList = new LinkedList<>();
        bucketCountList = new LinkedList<>();
        bucketThumbnailId = new LinkedList<>();
        bucketNameList = new LinkedList<>();
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.resultCursor = cursor;
        holders = new ArrayList<>();
        adHolders = new ArrayList<>();
        this.mediaAlbumFragment = frag;
        setMediaType(mediaAlbumFragment.getMediaType());
        loadPlaceHolderImages();
        setupAlbumBuckets();
    }

    private void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    private String getMediaType(){
        return this.mediaType;
    }

    private void loadPlaceHolderImages(){
        switch(getMediaType()) {
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                placeHolder = ContextCompat.getDrawable(mediaAlbumFragment.getActivity(), R.drawable.ic_vault_image_placeholder);
                return;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                placeHolder = ContextCompat.getDrawable(mediaAlbumFragment.getActivity(), R.drawable.ic_vault_video_placeholder);
                return;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                placeHolder = ContextCompat.getDrawable(mediaAlbumFragment.getActivity(), R.drawable.ic_vault_audio_placeholder);
        }
    }

    private Drawable getPlaceHolderImages(){
        return placeHolder;
    }

    private void setupAlbumBuckets(){
        mediaAlbumFragment.loadingStarted();
        bucketCountList.clear();
        bucketIdList.clear();
        bucketNameList.clear();
        bucketThumbnailId.clear();
        if(resultCursor!=null && resultCursor.getCount()>=1){
            Log.d("Vault",resultCursor.getCount() + "");
            loadAlbumData(resultCursor);
        }
        mediaAlbumFragment.loadingComplete();
        notifyDataSetChanged();
    }

    private void loadAlbumData(Cursor cursor){
        cursor.moveToFirst();
        String bucketId = "";
        int bucketCount = 0;
        do{
            int bucketIndex = cursor.getColumnIndex(MediaVaultModel.VAULT_BUCKET_ID);
            String bucketIdString = cursor.getString(bucketIndex);
            if(!bucketId.equals(bucketIdString)){
                bucketId = bucketIdString;
                int bucketNameIndex = cursor.getColumnIndex(MediaVaultModel.VAULT_BUCKET_NAME);
                int firstImageIndex = cursor.getColumnIndex(MediaVaultModel.THUMBNAIL_PATH);
                bucketIdList.add(bucketId);
                bucketNameList.add(cursor.getString(bucketNameIndex));
                bucketThumbnailId.add(cursor.getString(firstImageIndex));
                if(!cursor.isFirst()){
                    bucketCountList.add(bucketCount);
                    bucketCount = 0;
                }
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
        if(bucketIdList.size()>4){
            bucketIdList.add(3,"Ads");
            bucketNameList.add(3,"Ads");
            bucketCountList.add(3,0);
            bucketThumbnailId.add(3,"Ads");
        }
        if(bucketIdList.size()>8){
            bucketIdList.add(bucketIdList.size()-1,"Ads");
            bucketNameList.add(bucketNameList.size()-1,"Ads");
            bucketCountList.add(bucketCountList.size()-1,0);
            bucketThumbnailId.add(bucketThumbnailId.size()-1,"Ads");
        }
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctxt = parent.getContext();
        LayoutInflater inflater =  LayoutInflater.from(ctxt);
        if(viewType == ITEM_TYPE_ALBUM) {
            View itemView = inflater.inflate(R.layout.vault_album_recycler_item, parent, false);
            MediaAlbumPickerHolder holder = new MediaAlbumPickerHolder(itemView);
            holder.setOnGridItemSelectedListener(this);
            holders.add(holder);
            return holder;
        }
        if(viewType == ITEM_TYPE_ADS){
            View itemView = inflater.inflate(R.layout.vault_fragment_ads_recycler_item, parent, false);
            MediaAlbumAdHolder holder = new MediaAlbumAdHolder(itemView);
            holder.setOnGridItemSelectedListener(this);
            adHolders.add(holder);
            return holder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(getItemCount()>4 && position==3){
            return;
        }
        if(getItemCount()>8 && position==7){
            return;
        }
        String uriString = bucketThumbnailId.get(position);
        Uri uri = Uri.parse(uriString);
        Log.d("VaultMedia",uri.getPath() + " " + uri.getAuthority() + " " +uri.getScheme());
    }

    @Override
    public int getItemCount() {
        return bucketIdList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(getItemCount()>4 && position==3){
            return ITEM_TYPE_ADS;
        }
        if(getItemCount()>8 && position==7){
            return ITEM_TYPE_ADS;
        }
        return ITEM_TYPE_ALBUM;
    }



    @Override
    public void onGridItemSelected(int gridPosition) {
        if(getItemViewType(gridPosition) == ITEM_TYPE_ALBUM) {
            mediaAlbumFragment.albumClicked(bucketIdList.get(gridPosition), bucketNameList.get(gridPosition));
        }
        if(getItemViewType(gridPosition)== ITEM_TYPE_ADS){
            Toast.makeText(mediaAlbumFragment.getActivity(),"Ads Will be displayed",Toast.LENGTH_SHORT).show();
        }
    }

    void closeResources(){
        if(mediaAlbumFragment !=null) {
            mediaAlbumFragment = null;
        }
        for (MediaAlbumPickerHolder holder : holders){
            holder.setOnGridItemSelectedListener(null);
        }
        for(MediaAlbumAdHolder holder: adHolders){
            holder.setOnGridItemSelectedListener(null);
        }
    }
}

package com.smartfoxitsolutions.lockup.mediavault.services;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.smartfoxitsolutions.lockup.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultModel;
import com.smartfoxitsolutions.lockup.mediavault.SelectedMediaModel;
import com.smartfoxitsolutions.lockup.mediavault.VaultDbHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by RAAJA on 18-11-2016.
 */

public class MediaDeleteTask implements Runnable {

    private Context context;
    private Handler uiHandler;
    private String albumBucketId, mediaType;
    private ArrayList<String> selectedMediaId;
    private VaultDbHelper vaultDatabaseHelper;
    private SQLiteDatabase vaultDb;
    private Message mssg;

    MediaDeleteTask(Context ctxt, Handler.Callback callback){
        this.context = ctxt;
        this.uiHandler = new Handler(Looper.getMainLooper(),callback);
    }

    void setTaskRequirements(String bucketId, String media){
        this.albumBucketId = bucketId;
        this.mediaType = media;
        this.selectedMediaId = SelectedMediaModel.getInstance().getSelectedMediaIdList();
        String databasePath = Environment.getExternalStorageDirectory()+ File.separator
                +".lockup"+File.separator+"vault_db";
        vaultDatabaseHelper = new VaultDbHelper(context.getApplicationContext(),databasePath,null,1);
        vaultDb = vaultDatabaseHelper.getWritableDatabase();
    }

    private String[] getProjection(){
        return new String[]{MediaVaultModel.ID_COLUMN_NAME,MediaVaultModel.VAULT_MEDIA_PATH
                ,MediaVaultModel.VAULT_BUCKET_ID,MediaVaultModel.MEDIA_TYPE,MediaVaultModel.THUMBNAIL_PATH
                ,MediaVaultModel.FILE_EXTENSION};
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

    private String getSelection(){
        return MediaVaultModel.ID_COLUMN_NAME+ " IN (" +
                TextUtils.join(",", Collections.nCopies(selectedMediaId.size(),"?"))+")"
                + " AND " + MediaVaultModel.MEDIA_TYPE+"=?"+" AND "
                + MediaVaultModel.VAULT_BUCKET_ID+"=?";
    }

    private String[] getSelectionArgs(){
        String[] selectionArgs = new String[selectedMediaId.size()+2];
        for(int i=0;i<selectedMediaId.size();i++){
            selectionArgs[i] = selectedMediaId.get(i);
        }
        selectionArgs[selectionArgs.length-2] = getMediaCursorType(mediaType);
        selectionArgs[selectionArgs.length-1] = albumBucketId;
        return selectionArgs;
    }

    @Override
    public void run() {
        Cursor mediaCursor;
        String[] projection = getProjection();
        mediaCursor = vaultDb.query(MediaVaultModel.TABLE_NAME,projection,getSelection()
                ,getSelectionArgs(),null,null,null);
        if(mediaCursor !=null && mediaCursor.getCount()>0){
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
            mssg.arg1 = 0;
            mssg.arg2 = mediaCursor.getCount();
            mssg.sendToTarget();
            try {
                deleteFromVault(mediaCursor);
            }catch (IOException e){
                e.printStackTrace();
                SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
                mssg.sendToTarget();
            }

        }
    }

    private void deleteFromVault(Cursor cursor) throws IOException{
        int deleteCount = 0;
        cursor.moveToFirst();
        try {
            do {
                int vaultIdIndex = cursor.getColumnIndex(MediaVaultModel.ID_COLUMN_NAME);
                int vaultMediaIndex = cursor.getColumnIndex(MediaVaultModel.VAULT_MEDIA_PATH);
                int thumbnailPathIndex = cursor.getColumnIndex(MediaVaultModel.THUMBNAIL_PATH);
                int fileExtensionIndex = cursor.getColumnIndex(MediaVaultModel.FILE_EXTENSION);

                String vaultMediaPath = cursor.getString(vaultMediaIndex);
                String thumbnailPath = cursor.getString(thumbnailPathIndex);
                String vaultId = cursor.getString(vaultIdIndex);
                String extension = cursor.getString(fileExtensionIndex);

                boolean mediaDeleted = false;
                File mediaFile = new File(vaultMediaPath);
                if(mediaFile.exists()){
                    mediaDeleted = mediaFile.delete();
                }else{
                    File alternateMediaFile = new File(vaultMediaPath+"."+extension);
                    if(alternateMediaFile.exists()){
                        mediaDeleted = alternateMediaFile.delete();
                    }
                }
                Log.d("VaultMedia", String.valueOf(mediaDeleted) + " mediacopied");
                boolean thumbnailDeleted = false;
                if(mediaDeleted){
                    File thumbnailFile = new File(thumbnailPath);
                    if(thumbnailFile.exists()){
                        thumbnailDeleted = thumbnailFile.delete();
                    }else{
                        File alternateThumbnailFile = new File(thumbnailPath+".jpg");
                        if(alternateThumbnailFile.exists()){
                            thumbnailDeleted = alternateThumbnailFile.delete();
                        }
                    }
                }
                if (thumbnailDeleted) {
                    String selection = MediaVaultModel.ID_COLUMN_NAME+ " IN (" +
                            "?)"
                            + " AND " + MediaVaultModel.MEDIA_TYPE+"=?"+" AND "
                            + MediaVaultModel.VAULT_BUCKET_ID+"=?";
                        String[] selectionArgs = new String[]{vaultId,getMediaCursorType(mediaType),albumBucketId};
                        int deletedFromDb = vaultDb.delete(MediaVaultModel.TABLE_NAME,selection,selectionArgs);
                        deleteCount += deletedFromDb;
                    mssg = uiHandler.obtainMessage();
                    mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                    mssg.arg1 = cursor.getPosition() + 1;
                    mssg.arg2 = cursor.getCount();
                    mssg.sendToTarget();
                }
                Log.d("VaultMedia"," Inserted and deleted " + deleteCount);
            } while (cursor.moveToNext());
            SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
            mssg.sendToTarget();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void closeTask(){
        if(context!=null){
            context = null;
            vaultDb = null;
            vaultDatabaseHelper.close();
            uiHandler = null;
        }
    }
}

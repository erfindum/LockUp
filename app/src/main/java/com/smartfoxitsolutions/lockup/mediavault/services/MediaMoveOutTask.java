package com.smartfoxitsolutions.lockup.mediavault.services;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by RAAJA on 18-11-2016.
 */

public class MediaMoveOutTask implements Runnable {

    private Context context;
    private Handler uiHandler;
    private String albumBucketId, mediaType;
    private ArrayList<String> selectedMediaId;
    private VaultDbHelper vaultDatabaseHelper;
    private SQLiteDatabase vaultDb;
    private Message mssg;
    private LinkedList<String> scanner_file_path;

    MediaMoveOutTask(Context ctxt, Handler.Callback callback){
        this.context = ctxt;
        this.uiHandler = new Handler(Looper.getMainLooper(),callback);
        scanner_file_path = new LinkedList<>();
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

        return new String[]{MediaVaultModel.ID_COLUMN_NAME,MediaVaultModel.ORIGINAL_MEDIA_PATH
                        ,MediaVaultModel.VAULT_MEDIA_PATH,MediaVaultModel.VAULT_BUCKET_ID
                        ,MediaVaultModel.MEDIA_TYPE,MediaVaultModel.THUMBNAIL_PATH,MediaVaultModel.FILE_EXTENSION};
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
                TextUtils.join(",",Collections.nCopies(selectedMediaId.size(),"?"))+")"
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
                moveMediaToStorage(mediaCursor);
            }catch (IOException e){
                e.printStackTrace();
                SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
                mssg.sendToTarget();
            }

        }
    }

    private void moveMediaToStorage(Cursor cursor) throws IOException{
        cursor.moveToFirst();
        try {
            do {
                int vaultIdIndex = cursor.getColumnIndex(MediaVaultModel.ID_COLUMN_NAME);
                int vaultMediaIndex = cursor.getColumnIndex(MediaVaultModel.VAULT_MEDIA_PATH);
                int originalFilePathIndex = cursor.getColumnIndex(MediaVaultModel.ORIGINAL_MEDIA_PATH);
                int thumbnailPathIndex = cursor.getColumnIndex(MediaVaultModel.THUMBNAIL_PATH);
                int extensionIndex = cursor.getColumnIndex(MediaVaultModel.FILE_EXTENSION);

                String vaultMediaPath = cursor.getString(vaultMediaIndex);
                String originalFilePath = cursor.getString(originalFilePathIndex);
                String thumbnailPath = cursor.getString(thumbnailPathIndex);
                String vaultId = cursor.getString(vaultIdIndex);
                String extension = cursor.getString(extensionIndex);

                boolean mediaCopied = false;
                mediaCopied = copyMediaFile(vaultMediaPath, originalFilePath,extension);
                Log.d("VaultMedia", String.valueOf(mediaCopied) + " mediacopied");
                if (mediaCopied) {
                    File vaultFile = new File(vaultMediaPath);
                    boolean deleteVaultFile = false;
                    if(vaultFile.exists()) {
                        deleteVaultFile = vaultFile.delete();
                    }else{
                        File alternateMediaFile = new File(vaultMediaPath+"."+extension);
                        if(alternateMediaFile.exists()){
                            deleteVaultFile = alternateMediaFile.delete();
                        }
                    }
                    boolean deleteThumbnailFile = false;
                    if(deleteVaultFile) {
                        File thumbnailFile = new File(thumbnailPath);
                        if(thumbnailFile.exists()) {
                            deleteThumbnailFile = thumbnailFile.delete();
                        }else{
                            File alternateThumbnailFile = new File(thumbnailPath+".jpg");
                            if(alternateThumbnailFile.exists()){
                                deleteThumbnailFile = alternateThumbnailFile.delete();
                            }
                        }
                        Log.d("VaultMedia",String.valueOf(deleteThumbnailFile) + " thumbnail renamed");
                    }
                        if (deleteThumbnailFile) {
                            String selection = MediaVaultModel.ID_COLUMN_NAME+ " IN (" +
                                    "?)"+ " AND " + MediaVaultModel.MEDIA_TYPE+"=?"+" AND "
                                    + MediaVaultModel.VAULT_BUCKET_ID+"=?";
                            String[] selectionArgs = new String[]{vaultId, getMediaCursorType(mediaType), albumBucketId};
                            int deletedFromDb = vaultDb.delete(MediaVaultModel.TABLE_NAME, selection, selectionArgs);
                            Log.d("VaultMedia"," Deleted from database " + vaultId);
                           if(!scanner_file_path.isEmpty()) {
                                MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{scanner_file_path.get(0)}, null,
                                        new MediaScannerConnection.OnScanCompletedListener() {
                                            @Override
                                            public void onScanCompleted(String path, Uri uri) {
                                                Log.d("VaultMedia", " Scanned " + path);
                                            }
                                        });
                                scanner_file_path.clear();
                            }
                        }
                }
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                mssg.arg1 = cursor.getPosition() + 1;
                mssg.arg2 = cursor.getCount();
                mssg.sendToTarget();
            } while (cursor.moveToNext());
            SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
            mssg.sendToTarget();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean copyMediaFile(String vaultMediaPath, String originalFilePath, String extension) throws IOException{
        BufferedInputStream buffInput = null;
        BufferedOutputStream buffOutput = null;
        boolean mediaCopied = false;
        try {
            File vaultMediaFile = new File(vaultMediaPath);
            if(vaultMediaFile.exists()){
                buffInput = new BufferedInputStream(new FileInputStream(vaultMediaFile));
            }
            else{
                File alternateVaultMediaFile = new File(vaultMediaPath+"."+extension);
                if(alternateVaultMediaFile.exists()){
                    buffInput = new BufferedInputStream(new FileInputStream(alternateVaultMediaFile));
                }else{
                    return false;
                }
            }
            File originalMediaFile = new File(originalFilePath);
            boolean destCreated = false;
            if(!originalMediaFile.getParentFile().exists()) {
                destCreated = originalMediaFile.getParentFile().mkdirs();
            }else{
                destCreated = true;
            }
            if(destCreated && originalMediaFile.exists()){
                String alternateFilePath = originalFilePath.substring(0,originalFilePath.lastIndexOf(File.separator));
                String alternateFileName = originalFilePath.substring(originalFilePath.lastIndexOf(File.separator)+1
                                                                ,originalFilePath.lastIndexOf("."));
                File alternateFile = new File(alternateFilePath+File.separator
                                            +alternateFileName+"_"+System.currentTimeMillis()+"."+extension);
               boolean alternateFileCreated = alternateFile.createNewFile();
                if(alternateFileCreated) {
                    buffOutput = new BufferedOutputStream(new FileOutputStream(alternateFile));
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = buffInput.read(buffer)) > 0) {
                        buffOutput.write(buffer, 0, length);
                    }
                    scanner_file_path.add(alternateFile.getAbsolutePath());
                    return true;
                }else{
                    return false;
                }
            }
            if(destCreated && !originalMediaFile.exists()){
                boolean created = originalMediaFile.createNewFile();
                if(created) {
                    buffOutput = new BufferedOutputStream(new FileOutputStream(originalMediaFile));
                    byte[] buffer = new byte[1024];
                    int length;
                    while((length = buffInput.read(buffer))>0){
                        buffOutput.write(buffer,0,length);
                    }
                    scanner_file_path.add(originalFilePath);
                    return true;
                }
                else{
                    return false;
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            if(buffInput!=null){
                buffInput.close();
            }
            if (buffOutput!=null){
                buffOutput.close();
            }
        }
        return false;
    }

    private boolean sendScanBroadcast(){
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,Uri.parse("file://"+Environment.getExternalStorageDirectory())));
        return true;
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


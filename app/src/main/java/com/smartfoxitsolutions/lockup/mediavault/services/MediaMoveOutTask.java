package com.smartfoxitsolutions.lockup.mediavault.services;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.smartfoxitsolutions.lockup.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.lockup.mediavault.MediaVaultModel;
import com.smartfoxitsolutions.lockup.mediavault.VaultDbHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by RAAJA on 18-11-2016.
 */

public class MediaMoveOutTask implements Runnable {

    private Context context;
    private Handler uiHandler;
    private int mediaSelectionType;
    private String albumBucketId, mediaType;
    private String[] selectedMediaId;
    private VaultDbHelper vaultDatabaseHelper;
    private SQLiteDatabase vaultDb;
    private Message mssg;
    private LinkedList<String> scanner_file_path,scanner_alternate_file_path,vault_media_path, vault_thumbnail_path
                                ,vault_id_list;

    MediaMoveOutTask(Context ctxt, Handler.Callback callback){
        this.context = ctxt;
        this.uiHandler = new Handler(Looper.getMainLooper(),callback);
        scanner_file_path = new LinkedList<>();
        scanner_alternate_file_path = new LinkedList<>();
        vault_media_path = new LinkedList<>();
        vault_thumbnail_path = new LinkedList<>();
        vault_id_list = new LinkedList<>();
    }

    void setTaskRequirements(int selectionType, String bucketId, String media,String[] selectedMediaId){
        this.mediaSelectionType = selectionType;
        this.albumBucketId = bucketId;
        this.mediaType = media;
        this.selectedMediaId = selectedMediaId;
        String databasePath = Environment.getExternalStorageDirectory()+ File.separator
                +".lockup"+File.separator+"vault_db";
        vaultDatabaseHelper = new VaultDbHelper(context.getApplicationContext(),databasePath,null,1);
        vaultDb = vaultDatabaseHelper.getWritableDatabase();
    }

    private String[] getProjection(){

        return new String[]{MediaVaultModel.ID_COLUMN_NAME,MediaVaultModel.ORIGINAL_MEDIA_PATH
                        ,MediaVaultModel.VAULT_MEDIA_PATH,MediaVaultModel.VAULT_BUCKET_ID
                        ,MediaVaultModel.MEDIA_TYPE,MediaVaultModel.THUMBNAIL_PATH};
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
        return MediaVaultModel.MEDIA_TYPE+"=?"+" AND "+MediaVaultModel.VAULT_BUCKET_ID+"=?";
    }

    private String[] getSelectionArgs(String mediaType){
        return new String[]{getMediaCursorType(mediaType),albumBucketId};
    }

    private String getUniqueSelection(){
        return MediaVaultModel.ID_COLUMN_NAME+ " IN (" +
                TextUtils.join(",",Collections.nCopies(selectedMediaId.length,"?"))+")"
                + " AND " + MediaVaultModel.MEDIA_TYPE+"=?"+" AND "
                + MediaVaultModel.VAULT_BUCKET_ID+"=?";
    }

    private String[] getUniqueSelectionArgs(){
        String[] selectionArgs = new String[selectedMediaId.length+2];
        for(int i=0;i<selectedMediaId.length;i++){
            selectionArgs[i] = selectedMediaId[i];
        }
        selectionArgs[selectionArgs.length-2] = getMediaCursorType(mediaType);
        selectionArgs[selectionArgs.length-1] = albumBucketId;
        return selectionArgs;
    }

    @Override
    public void run() {
        Cursor mediaCursor;
        String[] projection = getProjection();
        if(mediaSelectionType == MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL){
            mediaCursor = vaultDb.query(MediaVaultModel.TABLE_NAME,projection,getSelection()
                    ,getSelectionArgs(mediaType),null,null,null);
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
                }
            }
        }
        if(mediaSelectionType == MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE){
            mediaCursor = vaultDb.query(MediaVaultModel.TABLE_NAME,projection,getUniqueSelection()
                    ,getUniqueSelectionArgs(),null,null,null);
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
                }

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

                String vaultMediaPath = cursor.getString(vaultMediaIndex);
                String originalFilePath = cursor.getString(originalFilePathIndex);
                String thumbnailPath = cursor.getString(thumbnailPathIndex);
                String vaultId = cursor.getString(vaultIdIndex);

                boolean mediaCopied = false;
                mediaCopied = copyMediaFile(vaultMediaPath, originalFilePath);
                Log.d("VaultMedia", String.valueOf(mediaCopied) + " mediacopied");
                if (mediaCopied) {
                    scanner_file_path.add(originalFilePath);
                    vault_media_path.add(vaultMediaPath);
                    vault_thumbnail_path.add(thumbnailPath);
                    vault_id_list.add(vaultId);
                    Log.d("VaultMedia", " DB Data Set");
                    mssg = uiHandler.obtainMessage();
                    mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                    mssg.arg1 = cursor.getPosition() + 1;
                    mssg.arg2 = cursor.getCount();
                    mssg.sendToTarget();
                }
            } while (cursor.moveToNext());
            boolean removeDone = removeFromDb();
            Log.d("VaultMedia", "Inserted to DB " + String.valueOf(removeDone));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Log.d("VaultMedia", "Media Scanner Started");
                scanner_file_path.addAll(scanner_alternate_file_path);
                runMediaScanner(scanner_file_path);
            } else {
                Log.d("VaultMedia", "Media Scanner Started below 19");
                sendScanBroadcast();
            }


            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
            mssg.sendToTarget();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean copyMediaFile(String vaultMediaPath, String originalFilePath) throws IOException{
        BufferedInputStream buffInput = null;
        BufferedOutputStream buffOutput = null;
        boolean mediaCopied = false;
        try {
            File vaultMediaFile = new File(vaultMediaPath);
            if(vaultMediaFile.exists()){
                buffInput = new BufferedInputStream(new FileInputStream(vaultMediaFile));
            }
            else{
                return false;
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
                String alternateFileName = originalFilePath.substring(originalFilePath.lastIndexOf(File.separator)+1);
                File alternateFile = new File(alternateFilePath+File.separator
                                            +System.currentTimeMillis()+"_"+alternateFileName);
                alternateFile.createNewFile();
                buffOutput = new BufferedOutputStream(new FileOutputStream(alternateFile));
                byte[] buffer = new byte[1024];
                int length;
                while((length = buffInput.read(buffer))>0){
                    buffOutput.write(buffer,0,length);
                }
                scanner_alternate_file_path.add(alternateFilePath+File.separator
                        +System.currentTimeMillis()+"_"+alternateFileName);
                return true;
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
        return mediaCopied;
    }

    private boolean removeFromDb(){
        int deleteCount = 0;
        for (int i = 0; i < vault_media_path.size(); i++) {
            File vaultFile = new File(vault_media_path.get(i));
            boolean deleteVaultFile = false;
            if(vaultFile.exists()) {
                deleteVaultFile = vaultFile.delete();
            }
            boolean deleteThumbnailFile = false;
            if(deleteVaultFile) {
                File thumbnailFile = new File(vault_thumbnail_path.get(i));
                if(thumbnailFile.exists()) {
                    deleteThumbnailFile = thumbnailFile.delete();
                }
                Log.d("VaultMedia",String.valueOf(deleteThumbnailFile) + " thumbnail renamed");
            }
            try {
                if (deleteThumbnailFile) {
                    String selection = MediaVaultModel.ID_COLUMN_NAME+ " IN (" +
                            "?)"+ " AND " + MediaVaultModel.MEDIA_TYPE+"=?"+" AND "
                            + MediaVaultModel.VAULT_BUCKET_ID+"=?";
                    String[] selectionArgs = new String[]{vault_id_list.get(i), getMediaCursorType(mediaType), albumBucketId};
                    int deletedFromDb = vaultDb.delete(MediaVaultModel.TABLE_NAME, selection, selectionArgs);
                    Log.d("VaultMedia"," Deleted from database " + vault_id_list.get(i));
                    deleteCount += deletedFromDb;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.d("VaultMedia"," Inserted and deleted " + deleteCount);
        return true;
    }

    private boolean runMediaScanner(LinkedList<String> originalFilePathList){
        Log.d("VaultMedia",originalFilePathList.size()+" Path size");
        String[] pathDummyArray = new String[originalFilePathList.size()];
        String[] pathArray = originalFilePathList.toArray(pathDummyArray);
        MediaScannerConnection.scanFile(context.getApplicationContext(), pathArray, null
                , new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d("VaultMedia",path);
                    }
                });
        return true;
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


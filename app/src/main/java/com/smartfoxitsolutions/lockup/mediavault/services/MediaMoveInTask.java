package com.smartfoxitsolutions.lockup.mediavault.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.smartfoxitsolutions.lockup.AppLockModel;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveInTask implements Runnable {

   private Context context;
   private ContentResolver contentResolver;
   private int mediaSelectionType;
   private String albumBucketId, mediaType;
   private String[] selectedMediaId, fileNames;
   private int viewWidth, viewHeight;
   private AtomicLong timestamp;
   private VaultDbHelper vaultDatabaseHelper;
   private SQLiteDatabase vaultDb;
   private Handler uiHandler;
   private Message mssg;
   private LinkedList<String> original_media_path,vault_media_path,original_file_name,vault_file_name,vault_bucket_id,vault_bucket_name
           ,file_extension, vault_thumbnail_path,scanner_file_path, vault_thumbnail_path_dummy, vault_media_path_dummy
           ,vault_media_failed_list,vault_thumbnail_failed_list;

    public MediaMoveInTask(Context ctxt, Handler.Callback callback) {
        this.context = ctxt;
        this.uiHandler = new Handler(Looper.getMainLooper(),callback);
        original_file_name = new LinkedList<>();
        original_media_path = new LinkedList<>();
        vault_media_path = new LinkedList<>();
        vault_file_name = new LinkedList<>();
        vault_bucket_id = new LinkedList<>();
        vault_bucket_name = new LinkedList<>();
        file_extension = new LinkedList<>();
        vault_thumbnail_path = new LinkedList<>();
        scanner_file_path = new LinkedList<>();
        vault_thumbnail_path_dummy = new LinkedList<>();
        vault_media_path_dummy = new LinkedList<>();
        vault_media_failed_list = new LinkedList<>();
        vault_thumbnail_failed_list = new LinkedList<>();
    }

    void setTaskRequirements(int selectionType, String bucketId, String media,String[] selectedMediaId, String[] fileNames){
        this.mediaSelectionType = selectionType;
        this.albumBucketId=bucketId;
        this.mediaType = media;
        this.selectedMediaId = selectedMediaId;
        this.fileNames = fileNames;
        this.contentResolver = context.getContentResolver();
        SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        this.viewWidth = prefs.getInt(MediaAlbumPickerActivity.THUMBNAIL_WIDTH_KEY,0);
        this.viewHeight = prefs.getInt(MediaAlbumPickerActivity.THUMBNAIL_HEIGHT_KEY,0);
        timestamp = new AtomicLong(System.currentTimeMillis());
        String databasePath = Environment.getExternalStorageDirectory()+ File.separator
                +".lockup"+File.separator+"vault_db";
        vaultDatabaseHelper = new VaultDbHelper(context.getApplicationContext(),databasePath,null,1);
        vaultDb = vaultDatabaseHelper.getWritableDatabase();
    }

    private Uri getExternalUri(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    private String[] getProjection(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return new String[]{MediaStore.Images.Media._ID,MediaStore.Images.Media.BUCKET_ID,MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                            ,MediaStore.Images.Media.DATA};

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return new String[]{MediaStore.Video.Media._ID,MediaStore.Video.Media.BUCKET_ID,MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Video.Media.DATA};

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.ALBUM_ID,MediaStore.Audio.Media.ALBUM
                        ,MediaStore.Audio.Media.DATA};
        }
        return null;
    }

    private String getSelection(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_ID+"=?";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_ID+"=?";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM_ID+"=?";
        }
        return null;

    }

    private String getUniqueSelection(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media._ID+" IN ("
                        + TextUtils.join(",", Collections.nCopies(selectedMediaId.length,"?"))+")"
                        + " AND "+ MediaStore.Images.Media.BUCKET_ID+"=?";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media._ID+" IN ("
                        + TextUtils.join(",", Collections.nCopies(selectedMediaId.length,"?"))+")"
                        + " AND "+ MediaStore.Video.Media.BUCKET_ID+"=?";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media._ID+" IN ("
                        + TextUtils.join(",", Collections.nCopies(selectedMediaId.length,"?"))+")"
                        + " AND "+ MediaStore.Audio.Media.ALBUM_ID+"=?";
        }
        return null;
    }

    private String[] getUniqueSelectionArgs(){
        String[] selectionArgs = new String[selectedMediaId.length+1];
        for(int i=0;i<selectedMediaId.length;i++){
            selectionArgs[i] = selectedMediaId[i];
        }
        selectionArgs[selectionArgs.length-1] = albumBucketId;
        return selectionArgs;
    }

    private String getBucketIndex(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_ID;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_ID;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM_ID;
        }
        return null;
    }

    private String getBucketNameIndex(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_DISPLAY_NAME;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM;
        }
        return null;
    }

    private String getDataIndex(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.DATA;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.DATA;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.DATA;
        }
        return null;
    }

    private String getVaultMediaType(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return "image";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return "video";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return "audio";
        }
        return null;
    }

    private String getOrderBy(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media._ID +" ASC";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media._ID +" ASC";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media._ID +" ASC";
        }
        return null;
    }

    @Override
    public void run() {
        Cursor mediaCursor;
        String[] projection = getProjection(mediaType);
        if(mediaSelectionType == MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL){
            String[] selectionArgs = {albumBucketId};
            mediaCursor = contentResolver.query(getExternalUri(mediaType),projection,getSelection(mediaType)
                                ,selectionArgs,getOrderBy(mediaType));
            if(mediaCursor !=null && mediaCursor.getCount()>0){
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                mssg.arg1 = 0;
                mssg.arg2 = mediaCursor.getCount();
                mssg.sendToTarget();
                try {
                    moveMediaToVault(mediaCursor);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        if(mediaSelectionType == MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE){
            mediaCursor = contentResolver.query(getExternalUri(mediaType),projection,getUniqueSelection(mediaType)
                                                ,getUniqueSelectionArgs(),getOrderBy(mediaType));
            if(mediaCursor !=null && mediaCursor.getCount()>0){
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                mssg.arg1 = 0;
                mssg.arg2 = mediaCursor.getCount();
                mssg.sendToTarget();
               try {
                   moveMediaToVault(mediaCursor);
               }catch (IOException e){
                   e.printStackTrace();
               }

            }
        }
    }

   private void moveMediaToVault(Cursor cursor) throws IOException{
        cursor.moveToFirst();
       try {
           do {
               int dataIndex = cursor.getColumnIndex(getDataIndex(mediaType));
               int bucketIdIndex = cursor.getColumnIndex(getBucketIndex(mediaType));
               int bucketNameIndex = cursor.getColumnIndex(getBucketNameIndex(mediaType));
               String dataPath = cursor.getString(dataIndex);
               String uniqueBucketId = cursor.getString(bucketIdIndex);
               String uniqueBucketName = cursor.getString(bucketNameIndex);
               String originalFileName = dataPath.substring(dataPath.lastIndexOf("/") + 1, dataPath.lastIndexOf("."));
               String extension = dataPath.substring(dataPath.lastIndexOf(".") + 1);
               String destPathDummy = "";
               String destPath = "";
               String thumbnailPathDummy = "";
               String thumbnailPath = "";
               if(mediaType.equals(MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA)){
                 //  uniqueBucketId = uniqueBucketId.substring(uniqueBucketId.lastIndexOf("-"));
                   destPathDummy = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + originalFileName + "." + extension;
                   destPath = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + fileNames[cursor.getPosition()];
                   thumbnailPathDummy = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                           + File.separator + originalFileName + "." + "jpg";
                   thumbnailPath = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                           + File.separator + fileNames[cursor.getPosition()];
               }else{
                   destPathDummy = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + originalFileName + "." + extension;
                   destPath = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + fileNames[cursor.getPosition()];
                   thumbnailPathDummy = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                           + File.separator + originalFileName + "." + "jpg";
                   thumbnailPath = Environment.getExternalStorageDirectory() + File.separator
                           + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                           + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                           + File.separator + fileNames[cursor.getPosition()];
               }
               boolean mediaCopied = false;
               mediaCopied = copyMediaFile(dataPath, destPathDummy);
               boolean thumbnailCopied = false;
               Log.d("VaultMedia", String.valueOf(mediaCopied) + " mediacopied");
               if (mediaCopied) {
                   Bitmap thumbnail = getThumbnail(dataPath);
                   Log.d("VaultMedia", String.valueOf(thumbnail == null));
                   File thumbnailFile = new File(thumbnailPathDummy);
                   if (!thumbnailFile.getParentFile().exists()) {
                       thumbnailFile.getParentFile().mkdirs();
                   }
                   boolean thumbFileCreated = false;
                   if (thumbnailFile.getParentFile().exists() && !thumbnailFile.exists()) {
                       thumbFileCreated = thumbnailFile.createNewFile();
                   } else {
                       thumbFileCreated = true;
                   }
                   if (thumbFileCreated && thumbnail != null) {
                       thumbnailCopied = thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(thumbnailPathDummy));
                       thumbnail.recycle();
                       Log.d("VaultMedia", String.valueOf(thumbnailCopied) + " thumbnail copied");
                   }
                   if (thumbFileCreated && thumbnail == null) {
                       thumbnailCopied = true;
                       Log.d("VaultMedia", String.valueOf(thumbnailCopied) + " thumbnail copied");
                   }
                   Log.d("VaultMedia", " ThumbnailCreated");
               } else {
                   scanner_file_path.add(dataPath);
                   continue;
               }
               if (thumbnailCopied) {
                   original_media_path.add(dataPath);
                   scanner_file_path.add(dataPath);
                   vault_media_path.add(destPath);
                   vault_media_path_dummy.add(destPathDummy);
                   original_file_name.add(originalFileName);
                   vault_file_name.add(fileNames[cursor.getPosition()]);
                   vault_bucket_id.add(uniqueBucketId);
                   vault_bucket_name.add(uniqueBucketName);
                   file_extension.add(extension);
                   vault_thumbnail_path.add(thumbnailPath);
                   vault_thumbnail_path_dummy.add(thumbnailPathDummy);
                   Log.d("VaultMedia", " DB Data Set");
                   mssg = uiHandler.obtainMessage();
                   mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                   mssg.arg1 = cursor.getPosition() + 1;
                   mssg.arg2 = cursor.getCount();
                   mssg.sendToTarget();
               }

               Log.d("VaultMedia", String.valueOf(uiHandler == null) + " uiHandler null?");
           } while (cursor.moveToNext());
           boolean insertDone = insertIntoDb();
           Log.d("VaultMedia", "Inserted to DB " + String.valueOf(insertDone));

           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
               Log.d("VaultMedia", "Media Scanner Started");
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

    private boolean copyMediaFile(String mediaPath, String destPath) throws IOException{
        BufferedInputStream buffInput = null;
        BufferedOutputStream buffOutput = null;
        boolean mediaCopied = false;
        try {
            File originalFile = new File(mediaPath);
            if(originalFile.exists()){
            buffInput = new BufferedInputStream(new FileInputStream(originalFile));
            }
            else{
                return false;
            }
            File file = new File(destPath);
            boolean destCreated = false;
            if(!file.getParentFile().exists()) {
                destCreated = file.getParentFile().mkdirs();
            }else{
                destCreated = true;
            }
            if(destCreated && file.exists()){
                buffOutput = new BufferedOutputStream(new FileOutputStream(destPath));
                byte[] buffer = new byte[1024];
                int length;
                while((length = buffInput.read(buffer))>0){
                    buffOutput.write(buffer,0,length);
                }
                return true;
            }
             if(destCreated && !file.exists()){
                boolean created = file.createNewFile();
                if(created) {
                    buffOutput = new BufferedOutputStream(new FileOutputStream(file));
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

    private Bitmap getThumbnail(String path){
        switch (mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return ThumbnailUtils.extractThumbnail(getImageBitmap(path),viewWidth,viewHeight);

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return getVideoBitmap(path);

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return getAudioBitmap(path);
        }
        return null;
    }

    private Bitmap getImageBitmap(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = calculateImageSampleSize(options,viewWidth,viewHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path,options);
    }

    private int calculateImageSampleSize(BitmapFactory.Options options,int reqWidth, int reqHeight){
        int optionWidth = options.outWidth;
        int optionHeight = options.outHeight;
        int inSample = 1;
        if(optionWidth>reqWidth || optionHeight>reqHeight){
            int heightRatio= Math.round((float)optionHeight/ (float)reqHeight);
            int widthRatio= Math.round((float)optionWidth/ (float)reqWidth);
            inSample = widthRatio<heightRatio ? widthRatio : heightRatio;
        }
        return inSample;
    }

    private Bitmap getVideoBitmap(String path){
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Bitmap bitmap;
        try {
            metadataRetriever.setDataSource(path);
            bitmap = metadataRetriever.getFrameAtTime(5000000);
        }finally {
            metadataRetriever.release();
        }
        return bitmap;
    }

    private Bitmap getAudioBitmap(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        byte[] bitmapByteData;
        try{
            retriever.setDataSource(path);
            bitmapByteData = retriever.getEmbeddedPicture();
        }finally {
            retriever.release();
        }
        if(bitmapByteData!=null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bitmapByteData,0,bitmapByteData.length,options);
            options.inSampleSize = calculateImageSampleSize(options, viewWidth, viewHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(bitmapByteData,0,bitmapByteData.length,options);
        }
        return null;
    }

    private String getMediaFolder(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return ".image";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return ".video";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return ".audio";
        }
        return null;
    }

    private boolean insertIntoDb(){
           ContentValues insertValues = new ContentValues();
           for (int i = 0; i < original_media_path.size(); i++) {
               boolean renamedFile = renameVaultMedia(vault_media_path_dummy.get(i),vault_media_path.get(i));
               boolean renamedThumbnail = false;
               if(renamedFile) {
                  renamedThumbnail = renameVaultMedia(vault_thumbnail_path_dummy.get(i), vault_thumbnail_path.get(i));
                   Log.d("VaultMedia",String.valueOf(renamedThumbnail) + " thumbnail renamed");
               }else{
                   vault_media_failed_list.add(vault_media_path_dummy.get(i));
               }
               if(renamedThumbnail) {
                   insertValues.put(MediaVaultModel.ORIGINAL_MEDIA_PATH, original_media_path.get(i));
                   insertValues.put(MediaVaultModel.VAULT_MEDIA_PATH, vault_media_path.get(i));
                   insertValues.put(MediaVaultModel.ORIGINAL_FILE_NAME, original_file_name.get(i));
                   insertValues.put(MediaVaultModel.VAULT_FILE_NAME, vault_file_name.get(i));
                   insertValues.put(MediaVaultModel.VAULT_BUCKET_ID, vault_bucket_id.get(i));
                   insertValues.put(MediaVaultModel.VAULT_BUCKET_NAME, vault_bucket_name.get(i));
                   insertValues.put(MediaVaultModel.FILE_EXTENSION, file_extension.get(i));
                   insertValues.put(MediaVaultModel.MEDIA_TYPE, getVaultMediaType(mediaType));
                   insertValues.put(MediaVaultModel.TIME_STAMP, vault_file_name.get(i));
                   insertValues.put(MediaVaultModel.THUMBNAIL_PATH, vault_thumbnail_path.get(i));
                   long insertedRow = 0;
                   if (vaultDb != null) {
                       insertedRow = vaultDb.insert(MediaVaultModel.TABLE_NAME, "NULL", insertValues);
                   }
                   deleteOriginalMedia(original_media_path.get(i));
                   Log.d("VaultMedia"," Inserted and deleted");
               }
               else{
                   vault_thumbnail_failed_list.add(vault_thumbnail_path_dummy.get(i));
               }

           }
            deleteFailedFiles(vault_media_failed_list);
        deleteFailedFiles(vault_thumbnail_failed_list);
        return true;
    }

    private boolean deleteOriginalMedia(String path){
        File deletePath = new File(path);
        if(deletePath.exists()){
            return deletePath.delete();
        }
        return false;
    }

    private void deleteFailedFiles(LinkedList<String> failedList){
        for(String dummy : failedList){
            File deleteDummy = new File(dummy);
            if(deleteDummy.exists()){
                deleteDummy.delete();
            }
        }
    }

    private boolean renameVaultMedia(String path, String to){
        File renamePath = new File(path);
        File toPath = new File(to);
        Log.d("VaultMedia",renamePath.getAbsolutePath());
        Log.d("VaultMedia",toPath.getAbsolutePath());
        if(renamePath.exists() && toPath.getParentFile().exists()){
            return renamePath.renameTo(toPath);
        }
        return false;
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
            contentResolver = null;
            vaultDb = null;
            vaultDatabaseHelper.close();
            uiHandler = null;
        }
    }
}

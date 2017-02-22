package com.smartfoxitsolutions.lockup.mediavault.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.smartfoxitsolutions.lockup.mediavault.MediaVaultModel;
import com.smartfoxitsolutions.lockup.mediavault.VaultDbHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by RAAJA on 02-12-2016.
 */

public class ShareMoveTask implements Runnable {

    private Context context;
    private ContentResolver resolver;
    private Handler uiHandler;
    private int viewWidth, viewHeight;
    private AtomicLong timestamp;
    private VaultDbHelper vaultDatabaseHelper;
    private SQLiteDatabase vaultDb;
    private ContentValues insertValues;
    private Message mssg;
    private ArrayList<Uri> fileUriList;
    private MimeTypeMap mimeTypeMap;
    private String currentVaultMediaFile, currentThumbnailMediaFile;
    private int currentPosition;
    private boolean shouldPostInsufficientSpace;


    public ShareMoveTask(Context context, Handler.Callback callback) {
        this.context = context;
        resolver = context.getContentResolver();
        uiHandler = new Handler(Looper.getMainLooper(),callback);
        mimeTypeMap = MimeTypeMap.getSingleton();
    }

    void setTaskRequirements(ArrayList<Uri> uriList, int viewWidth, int viewHeight){
        this.fileUriList = uriList;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        currentPosition=0;
        timestamp = new AtomicLong(System.currentTimeMillis());
        String databasePath = Environment.getExternalStorageDirectory()+ File.separator
                +".lockup"+File.separator+"vault_db";
        vaultDatabaseHelper = new VaultDbHelper(context.getApplicationContext(),databasePath,null,1);
        vaultDb = vaultDatabaseHelper.getWritableDatabase();
    }

    private String[] getProjection(String mediaType){
        switch(mediaType){
            case ShareMoveService.TYPE_IMAGE_MEDIA:
                return new String[]{MediaStore.Images.Media._ID,MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Images.Media.DATA};

            case ShareMoveService.TYPE_VIDEO_MEDIA:
                return new String[]{MediaStore.Video.Media._ID,MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Video.Media.DATA};

            case ShareMoveService.TYPE_AUDIO_MEDIA:
                return new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.ALBUM
                        ,MediaStore.Audio.Media.DATA};
        }
        return null;
    }

    private String getDataIndex(String mediaType){
        switch(mediaType){
            case ShareMoveService.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.DATA;
            case ShareMoveService.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.DATA;
            case ShareMoveService.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.DATA;
        }
        return null;
    }

    private String getBucketNameIndex(String mediaType){
        switch(mediaType){
            case ShareMoveService.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
            case ShareMoveService.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_DISPLAY_NAME;
            case ShareMoveService.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM;
        }
        return null;
    }

    private String getBucketId(String path){
        StringBuffer pathBucketId = new StringBuffer();
        try {
            byte[] pathBucketByte = path.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] pathDigest = md.digest(pathBucketByte);
            pathBucketId = new StringBuffer();
            for (int i = 0; i < pathDigest.length; ++i) {
                pathBucketId.append(Integer.toHexString((pathDigest[i] & 0xFF) | 0x100).substring(1,3));
            }
        }catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return pathBucketId.toString();
    }

    private String getBucketName(String path){
        File mediaFile = new File(path);
        return  mediaFile.getParentFile().getName();
    }

    private String getAudioBucketName(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
        }catch (Exception e){
            e.printStackTrace();
        }
        String albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        retriever.release();
        if(albumName==null){
            return "UnknownAlbum";
        }
        return albumName;
    }

    private String getFileExtension(String path){
        return path.substring(path.lastIndexOf(".")+1);
    }

    private String getOriginalFileName(String path){
        return path.substring(path.lastIndexOf(File.separator)+1,path.lastIndexOf("."));
    }

    private long getFileTimeStamp(){
        long currentTime = System.currentTimeMillis();
        if(timestamp.get()>=currentTime){
            currentTime = timestamp.get()+1;
        }
        if(timestamp.compareAndSet(timestamp.get(),currentTime)){
            return currentTime;
        }
        return 0;
    }

    @Override
    public void run() {
        for(Uri uri:fileUriList){
            moveFileToVault(uri,uri.getScheme());
        }
        if(!shouldPostInsufficientSpace) {
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
            mssg.sendToTarget();
        }
    }

    private void moveFileToVault(Uri uri, String scheme){
        try {
            if (scheme.equals("content")) {
                moveContentScheme(uri);
            }
            if (scheme.equals("file")) {
                moveFileScheme(uri);
            }
        }catch (IOException e){
            File currentVaultFile = new File(currentVaultMediaFile);
            File currentThumbnailFile = new File(currentThumbnailMediaFile);
            if(currentVaultFile.exists()){
                currentVaultFile.delete();
            }
            if(currentThumbnailFile.exists()){
                currentThumbnailFile.delete();
            }
            e.printStackTrace();
        }
    }

    private void moveContentScheme(Uri uri) throws IOException{
        String type = resolver.getType(uri);
        if(type == null || type.isEmpty()){
            return;
        }
        String mediaType = type.substring(0,type.indexOf("/"));
        if(!checkValidMimeType(mediaType)){
            return;
        }
        Cursor mediaCursor = resolver.query(uri,getProjection(mediaType),null,null,null);
        if(mediaCursor!=null && mediaCursor.getCount()>0) {
            mediaCursor.moveToFirst();
            int dataIndex = mediaCursor.getColumnIndex(getDataIndex(mediaType));
            String dataPath = mediaCursor.getString(dataIndex);
            if(dataPath==null){
                return;
            }
            String uniqueBucketName = "";
            String uniqueBucketId = "";
            if (mediaType.equals(ShareMoveService.TYPE_AUDIO_MEDIA)) {
                uniqueBucketName = getAudioBucketName(dataPath);
                uniqueBucketId = getBucketId(uniqueBucketName);
            } else {
                uniqueBucketName = getBucketName(dataPath);
                uniqueBucketId = getBucketId(dataPath.substring(0, dataPath.lastIndexOf(File.separator)));
            }
            String originalFileName = dataPath.substring(dataPath.lastIndexOf(File.separator) + 1, dataPath.lastIndexOf("."));
            String extension = dataPath.substring(dataPath.lastIndexOf(".") + 1);
            String vaultFileName = String.valueOf(getFileTimeStamp());
            String destPathDummy = Environment.getExternalStorageDirectory() + File.separator
                    + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                    + uniqueBucketId + File.separator + originalFileName + "." + extension;
            String destPath = Environment.getExternalStorageDirectory() + File.separator
                    + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                    + uniqueBucketId + File.separator + vaultFileName;
            String thumbnailPathDummy = Environment.getExternalStorageDirectory() + File.separator
                    + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                    + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                    + File.separator + originalFileName + "." + "jpg";
            String thumbnailPath = Environment.getExternalStorageDirectory() + File.separator
                    + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                    + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                    + File.separator + vaultFileName;
            currentVaultMediaFile = destPathDummy;
            currentThumbnailMediaFile = thumbnailPathDummy;
            boolean isFileSpaceAvailable = getFileSpaceAvailability(dataPath);
            boolean mediaCopied = false;
            if(isFileSpaceAvailable) {
                mediaCopied = copyMediaFile(dataPath, destPathDummy);
            }else{
                shouldPostInsufficientSpace = true;
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MOVE_INSUFFICIENT_SPACE;
                mssg.sendToTarget();
                return;
            }
            boolean thumbnailCopied = false;
            Log.d("VaultMedia", String.valueOf(mediaCopied) + " mediacopied");
            if (mediaCopied) {
                Bitmap thumbnail = getThumbnail(dataPath,mediaType);
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
            }
            if (thumbnailCopied) {
                boolean renamedFile = renameVaultMedia(destPathDummy, destPath);
                boolean renamedThumbnail = false;
                if (renamedFile) {
                    renamedThumbnail = renameVaultMedia(thumbnailPathDummy, thumbnailPath);
                    Log.d("VaultMedia", String.valueOf(renamedThumbnail) + " thumbnail renamed");
                } else {
                    deleteFailedFiles(destPathDummy);
                }
                if (renamedThumbnail) {
                    insertValues = new ContentValues();
                    insertValues.put(MediaVaultModel.ORIGINAL_MEDIA_PATH, dataPath);
                    insertValues.put(MediaVaultModel.VAULT_MEDIA_PATH, destPath);
                    insertValues.put(MediaVaultModel.ORIGINAL_FILE_NAME, originalFileName);
                    insertValues.put(MediaVaultModel.VAULT_FILE_NAME, vaultFileName);
                    insertValues.put(MediaVaultModel.VAULT_BUCKET_ID, uniqueBucketId);
                    insertValues.put(MediaVaultModel.VAULT_BUCKET_NAME, uniqueBucketName);
                    insertValues.put(MediaVaultModel.FILE_EXTENSION, extension);
                    insertValues.put(MediaVaultModel.MEDIA_TYPE, mediaType);
                    insertValues.put(MediaVaultModel.TIME_STAMP, vaultFileName);
                    insertValues.put(MediaVaultModel.THUMBNAIL_PATH, thumbnailPath);
                    long insertedRow = 0;
                    if (vaultDb != null) {
                        insertedRow = vaultDb.insert(MediaVaultModel.TABLE_NAME, "NULL", insertValues);
                    }
                    deleteOriginalMedia(dataPath);
                    MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{dataPath}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.d("VaultMedia", " Scanned " + path);
                                }
                            });
                    Log.d("VaultMedia", " Inserted and deleted");
                } else {
                    deleteFailedFiles(thumbnailPathDummy);
                }
            }
            currentPosition+=1;
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
            mssg.arg1 = currentPosition;
            mssg.arg2 = fileUriList.size();
            mssg.sendToTarget();
            mediaCursor.close();
        }
    }

    private void moveFileScheme(Uri uri) throws IOException{
        String extType = MimeTypeMap.getFileExtensionFromUrl(uri.toString()).toLowerCase();
        String mimeType = mimeTypeMap.getMimeTypeFromExtension(extType);
        if(mimeType == null || mimeType.isEmpty()){
            return;
        }
        String mediaType = mimeType.substring(0,mimeType.indexOf("/"));
        if(!checkValidMimeType(mediaType)){
            return;
        }
        String dataPath = uri.getSchemeSpecificPart().substring(2);
        String uniqueBucketName = "";
        String uniqueBucketId = "";
        if(mediaType.equals(ShareMoveService.TYPE_AUDIO_MEDIA)){
            uniqueBucketName = getAudioBucketName(dataPath);
            uniqueBucketId = getBucketId(uniqueBucketName);
        }else{
            uniqueBucketName = getBucketName(dataPath);
            uniqueBucketId = getBucketId(dataPath.substring(0, dataPath.lastIndexOf(File.separator)));
        }
        String originalFileName = getOriginalFileName(dataPath);
        String extension = getFileExtension(dataPath);
        String vaultFileName = String.valueOf(getFileTimeStamp());

        String destPathDummy = Environment.getExternalStorageDirectory() + File.separator
                + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                + uniqueBucketId + File.separator + originalFileName + "." + extension;
        String destPath = Environment.getExternalStorageDirectory() + File.separator
                + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                + uniqueBucketId + File.separator + vaultFileName;
        String thumbnailPathDummy = Environment.getExternalStorageDirectory() + File.separator
                + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                + File.separator + originalFileName + "." + "jpg";
        String thumbnailPath = Environment.getExternalStorageDirectory() + File.separator
                + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                + File.separator + vaultFileName;

        currentVaultMediaFile = destPathDummy;
        currentThumbnailMediaFile = thumbnailPathDummy;

        boolean isFileSpaceAvailable = getFileSpaceAvailability(dataPath);
        boolean mediaCopied = false;
        if(isFileSpaceAvailable) {
            mediaCopied = copyMediaFile(dataPath, destPathDummy);
        }else{
            shouldPostInsufficientSpace = true;
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MOVE_INSUFFICIENT_SPACE;
            mssg.sendToTarget();
            return;
        }
        boolean thumbnailCopied = false;
        Log.d("VaultMedia", String.valueOf(mediaCopied) + " mediacopied");
        if (mediaCopied) {
            Bitmap thumbnail = getThumbnail(dataPath,mediaType);
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
        }
        if (thumbnailCopied) {
            boolean renamedFile = renameVaultMedia(destPathDummy, destPath);
            boolean renamedThumbnail = false;
            if (renamedFile) {
                renamedThumbnail = renameVaultMedia(thumbnailPathDummy, thumbnailPath);
                Log.d("VaultMedia", String.valueOf(renamedThumbnail) + " thumbnail renamed");
            } else {
                deleteFailedFiles(destPathDummy);
            }
            if (renamedThumbnail) {
                insertValues = new ContentValues();
                insertValues.put(MediaVaultModel.ORIGINAL_MEDIA_PATH, dataPath);
                insertValues.put(MediaVaultModel.VAULT_MEDIA_PATH, destPath);
                insertValues.put(MediaVaultModel.ORIGINAL_FILE_NAME, originalFileName);
                insertValues.put(MediaVaultModel.VAULT_FILE_NAME, vaultFileName);
                insertValues.put(MediaVaultModel.VAULT_BUCKET_ID, uniqueBucketId);
                insertValues.put(MediaVaultModel.VAULT_BUCKET_NAME, uniqueBucketName);
                insertValues.put(MediaVaultModel.FILE_EXTENSION, extension);
                insertValues.put(MediaVaultModel.MEDIA_TYPE, mediaType);
                insertValues.put(MediaVaultModel.TIME_STAMP, vaultFileName);
                insertValues.put(MediaVaultModel.THUMBNAIL_PATH, thumbnailPath);
                long insertedRow = 0;
                if (vaultDb != null) {
                    insertedRow = vaultDb.insert(MediaVaultModel.TABLE_NAME, "NULL", insertValues);
                }
                deleteOriginalMedia(dataPath);
                MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{dataPath}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.d("VaultMedia", " Scanned " + path);
                            }
                        });
                Log.d("VaultMedia", " Inserted and deleted");
            } else {
                deleteFailedFiles(thumbnailPathDummy);
            }
        }
        currentPosition+=1;
        mssg = uiHandler.obtainMessage();
        mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
        mssg.arg1 = currentPosition;
        mssg.arg2 = fileUriList.size();
        mssg.sendToTarget();

    }

    boolean getFileSpaceAvailability(String path){
        StatFs fileStats = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2){
            File originalFile = new File(path);
            if(originalFile.exists()) {
                if (fileStats.getAvailableBytes() > (originalFile.length() + 10_24_000)) {
                    return true;
                } else {
                    return false;
                }
            }else{
                return true;
            }
        }else{
            File originalFile = new File(path);
            if(originalFile.exists()) {
                long availableBytes = fileStats.getAvailableBlocks() * fileStats.getBlockSize();
                if (availableBytes > (originalFile.length() + 10_24_000)) {
                    return true;
                } else {
                    return false;
                }
            }else{
                return true;
            }
        }
    }

    private boolean copyMediaFile(String mediaPath, String destPath) throws IOException {
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

    private String getMediaFolder(String mediaType){
        switch(mediaType){
            case ShareMoveService.TYPE_IMAGE_MEDIA:
                return ".image";
            case ShareMoveService.TYPE_VIDEO_MEDIA:
                return ".video";
            case ShareMoveService.TYPE_AUDIO_MEDIA:
                return ".audio";
        }
        return null;
    }

    private boolean checkValidMimeType(String mediaType){
        switch (mediaType){
            case ShareMoveService.TYPE_IMAGE_MEDIA:
            case ShareMoveService.TYPE_VIDEO_MEDIA:
            case ShareMoveService.TYPE_AUDIO_MEDIA:
                return true;
        }
        return false;
    }

    private Bitmap getThumbnail(String path, String mediaType){
        switch (mediaType){
            case ShareMoveService.TYPE_IMAGE_MEDIA:
                return ThumbnailUtils.extractThumbnail(getImageBitmap(path),viewWidth,viewHeight);

            case ShareMoveService.TYPE_VIDEO_MEDIA:
                return getVideoBitmap(path);

            case ShareMoveService.TYPE_AUDIO_MEDIA:
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

    private boolean deleteOriginalMedia(String path){
        File deletePath = new File(path);
        if(deletePath.exists()){
            return deletePath.delete();
        }
        return false;
    }

    private void deleteFailedFiles(String deletePath){
        File deleteDummy = new File(deletePath);
        if(deleteDummy.exists()){
            deleteDummy.delete();
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

    void closeTask(){
        if(context!=null){
            context = null;
            resolver = null;
            vaultDb = null;
            vaultDatabaseHelper.close();
            uiHandler = null;
        }
    }
}

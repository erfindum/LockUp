package com.smartfoxitsolutions.lockup.mediavault;

import java.util.LinkedList;

/**
 * Created by RAAJA on 03-11-2016.
 */

public class HiddenFileContentModel {

private static LinkedList<String> mediaVaultFile, mediaOriginalName, mediaExtension, mediaId;

static {
    mediaVaultFile = new LinkedList<>();
    mediaOriginalName = new LinkedList<>();
    mediaExtension = new LinkedList<>();
    mediaId = new LinkedList<>();
}

    public static LinkedList<String> getMediaVaultFile(){
         return mediaVaultFile;
    }

    public static LinkedList<String> getMediaOriginalName(){
        return mediaOriginalName;
    }

    public static LinkedList<String> getMediaExtension(){
        return mediaExtension;
    }

    public static LinkedList<String> getMediaId(){
        return mediaId;
    }


    public static void setMediaVaultFile(LinkedList<String> mediaVaultFileName){
        mediaVaultFile.clear();
        mediaVaultFile.addAll(mediaVaultFileName);
    }

    public static void setMediaOriginalName(LinkedList<String> mediaOriginalFileName){
        mediaOriginalName.clear();
        mediaOriginalName.addAll(mediaOriginalFileName);
    }

    public static void setMediaExtension(LinkedList<String> mediaExtensionName){
        mediaExtension.clear();
        mediaExtension.addAll(mediaExtensionName);
    }

    public static void setMediaId(LinkedList<String> mediaIdList){
        mediaId.clear();
        mediaId.addAll(mediaIdList);
    }
}

package com.smartfoxitsolutions.lockup.mediavault;

import java.util.LinkedList;

/**
 * Created by RAAJA on 03-11-2016.
 */

public class HiddenFileContentModel {

private static LinkedList<String> mediaVaultFile, mediaOriginalName, mediaExtension;
private static boolean isAudioAlbumChanged;

static {
    mediaVaultFile = new LinkedList<>();
    mediaOriginalName = new LinkedList<>();
    mediaExtension = new LinkedList<>();
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

    public static boolean getIsAudioAlbumChanged(){
        return isAudioAlbumChanged;
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

    public static void setIsAudioAlbumChanged(boolean isAudiChanged){
        isAudioAlbumChanged = isAudiChanged;
    }
}

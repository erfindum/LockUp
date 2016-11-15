package com.smartfoxitsolutions.lockup.mediavault;

import java.util.LinkedList;

/**
 * Created by RAAJA on 03-11-2016.
 */

public class HiddenFileContentModel {

private static LinkedList<String> mediaVaultFile, mediaOriginalName, mediaExtension;

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

    public static void setMediaVaultFile(LinkedList<String> mediaVaultFileName){
        mediaVaultFile = mediaVaultFileName;
    }

    public static void setMediaOriginalName(LinkedList<String> mediaOriginalFileName){
        mediaOriginalName = mediaOriginalFileName;
    }

    public static void setMediaExtension(LinkedList<String> mediaExtensionName){
        mediaExtension = mediaExtensionName;
    }


}

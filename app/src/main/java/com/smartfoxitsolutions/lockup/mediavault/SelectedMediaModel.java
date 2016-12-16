package com.smartfoxitsolutions.lockup.mediavault;

import java.util.ArrayList;

/**
 * Created by RAAJA on 12-12-2016.
 */

public class SelectedMediaModel {

   private static SelectedMediaModel selectedMediaModelInstance;
    private ArrayList<String> selectedMediaIdList, selectedMediaFileNameList;

    public static SelectedMediaModel getInstance(){
        if(selectedMediaModelInstance==null){
            selectedMediaModelInstance = new SelectedMediaModel();
            return selectedMediaModelInstance;
        }
            return selectedMediaModelInstance;
    }

    void setSelectedMediaIdList(ArrayList<String> seletedMediaId){
        if(selectedMediaIdList == null) {
            this.selectedMediaIdList = new ArrayList<>();
            this.selectedMediaIdList.addAll(seletedMediaId);
        }else {
            this.selectedMediaIdList.clear();
            this.selectedMediaIdList.addAll(seletedMediaId);
        }
    }

    void setSelectedMediaFileNameList(ArrayList<String> selectedMediaFileName){
        if(selectedMediaFileNameList == null) {
            this.selectedMediaFileNameList = new ArrayList<>();
            this.selectedMediaFileNameList.addAll(selectedMediaFileName);
        }else{
            this.selectedMediaFileNameList.clear();
            this.selectedMediaFileNameList.addAll(selectedMediaFileName);
        }
    }

    public ArrayList<String> getSelectedMediaIdList(){
        return this.selectedMediaIdList;
    }

    public  ArrayList<String> getSelectedMediaFileNameList(){
        return this.selectedMediaFileNameList;
    }
}

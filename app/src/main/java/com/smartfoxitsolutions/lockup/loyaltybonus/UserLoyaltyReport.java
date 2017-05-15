package com.smartfoxitsolutions.lockup.loyaltybonus;

import com.google.gson.annotations.SerializedName;

/**
 * Created by RAAJA on 06-02-2017.
 */

public class UserLoyaltyReport {
    @SerializedName("reportDate")
    private String reportDate;

    @SerializedName("Totalimpression")
    private String Totalimpression;

    @SerializedName("TotalClicked")
    private String TotalClicked;

    @SerializedName("Totalimpression2")
    private String Totalimpression2;

    @SerializedName("TotalClicked2")
    private String TotalClicked2;

    public UserLoyaltyReport(String reportDate,int impression, int click, int impression2, int click2){
        this.reportDate = reportDate;
        setTotalImpression(impression);
        setTotalImpression2(impression2);
        setTotalClicked(click);
        setTotalClicked2(click2);
    }

    public void setTotalImpression(int impression){
        this.Totalimpression = String.valueOf(impression);
    }

    public void setTotalImpression2(int impression2){
        this.Totalimpression2 = String.valueOf(impression2);
    }

    public void setTotalClicked(int clicks){
        this.TotalClicked = String.valueOf(clicks);
    }

    public void setTotalClicked2(int clicks2){
        this.TotalClicked2 = String.valueOf(clicks2);
    }

    public String getTotalImpression(){
        return this.Totalimpression;
    }

    public String getTotalImpression2(){
        return this.Totalimpression2;
    }

    public String getTotalClicked(){
        return this.TotalClicked;
    }

    public String getTotalClicked2(){
        return this.TotalClicked2;
    }

    public String getReportDate(){
        return this.reportDate;
    }
}

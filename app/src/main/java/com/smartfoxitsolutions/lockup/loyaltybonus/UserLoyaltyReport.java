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

    public UserLoyaltyReport(String reportDate){
        this.reportDate = reportDate;
    }

    public void setTotalImpression(int impression){
        this.Totalimpression = String.valueOf(impression);
    }

    public void setTotalClicked(int clicks){
        this.TotalClicked = String.valueOf(clicks);
    }

    public String getTotalImpression(){
        return this.Totalimpression;
    }

    public String getTotalClicked(){
        return this.TotalClicked;
    }

    public String getReportDate(){
        return this.reportDate;
    }
}

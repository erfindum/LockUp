package com.smartfoxitsolutions.lockup.loyaltybonus;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by RAAJA on 18-01-2017.
 */

public interface LoyaltyBonusRequest {

    @GET("service.php")
    Call<LoyaltyBonusSignUpResponse> requestSignUp(@Query("action")String action,@Query("fullname") String fullname
                    ,@Query("password") String password,@Query("dob") String dob,@Query("country") String country
                    , @Query("gender") String gender, @Query("emailId") String emailId,@Query("deviceId") String deviceId);

    @GET("service.php")
    Call<LoyaltyBonusInitialPointResponse> requestInitialPoints(@Query("action") String action, @Query("emailId") String emailId
                    ,@Query("auth_code") String auth_code);

   @GET("service.php")
    Call<LoyaltyBonusRecoveryResponse> requestRecoverCode(@Query("action")String action, @Query("emailId")String emailId);

    @GET("service.php")
    Call<LoyaltyBonusResetResponse> requestResetPassword(@Query("action")String action,@Query("emailId")String emailId,
                                                            @Query("password")String password,@Query("forgetcode") String forgetcode);

    @GET("service.php")
    Call<LoyaltyBonusLoginResponse> requestLogin(@Query("action")String action,@Query("password") String password
                                                ,@Query("emailId")String emailId);

    @GET("service.php")
    Call<UserLoyaltyReportResponse> sendUserLoyaltyReport(@Query("action") String action, @Query("emailId")String emailId
            , @Query("auth_code")String auth_code, @Query("reportData") String reportData);

    @GET("service.php")
    Call<LoyaltyUserRedeemResponse> requestBonusRedeem(@Query("action") String action, @Query("Redeem_Point") String redeemPoints
            ,@Query("emailId") String emailId, @Query("auth_code") String auth_code,@Query("Redeem_Type") String redeemType
            ,@Query("Reddemd_price") String redeemedPrice, @Query("date") String redeemDate, @Query("ReddemedAcc") String redeemedAcc);

}

package com.smartfoxitsolutions.lockup;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by RAAJA on 14-12-2016.
 */

public interface ResetPasswordRequest {

    @GET("Service.php")
    Call<ResetPasswordResponse> requestRecovery(@Query("action") String action,@Query("email") String email);
}

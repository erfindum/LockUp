package com.smartfoxitsolutions.lockup.loyaltybonus;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by RAAJA on 18-01-2017.
 */

public class LoyaltyServiceGenerator {
    public static String BASE_API_URL = "http://www.lockup-applock.com/lockup/";

    private static OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();

    private static Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
            .baseUrl(BASE_API_URL)
            .addConverterFactory(GsonConverterFactory.create());

    public static <L> L createService(Class<L> serviceClass){
        Retrofit retro = retrofitBuilder.client(okHttpBuilder.build()).build();
        return retro.create(serviceClass);
    }

}

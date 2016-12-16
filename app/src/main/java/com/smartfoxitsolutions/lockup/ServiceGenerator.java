package com.smartfoxitsolutions.lockup;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by RAAJA on 14-12-2016.
 */

public class ServiceGenerator {

public static String BASE_API_URL = "http://www.lockup-applock.com/Api/";

private static OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();

private static Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                                        .baseUrl(BASE_API_URL)
                                        .addConverterFactory(GsonConverterFactory.create());

    public static <L> L createService(Class<L> serviceClass){
        Retrofit retro = retrofitBuilder.client(okHttpBuilder.build()).build();
        return retro.create(serviceClass);
    }
}

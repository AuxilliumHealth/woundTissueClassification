package com.auxilliumhealth.woundtissueclassification.Network;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static final String DEFAULT_BASE_URL = "https://api.woundtele.com";
    private static final String CALIBRATION_BASE_URL = "https://calibration.woundtele.com/";

    public static synchronized Retrofit getInstance(boolean isCalibration, String token) {
        // HTTP Logging Interceptor
        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Authentication Interceptor
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .method(original.method(), original.body());
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .protocols(Arrays.asList(Protocol.HTTP_1_1))
                .readTimeout(220, TimeUnit.SECONDS)
                .connectTimeout(180, TimeUnit.SECONDS)
                .build();

        // Select the appropriate base URL based on isCalibration
        String baseUrl = isCalibration ? CALIBRATION_BASE_URL : DEFAULT_BASE_URL;

        // Rebuild Retrofit instance if it doesn't exist or if the base URL has changed
        if (retrofit == null || !retrofit.baseUrl().toString().equals(baseUrl)) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
        }

        return retrofit;
    }
}
package com.radio.chat.rest;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class APIExecutor {

    private ApiService apiService;

    public static ApiService getApiService() {
        String credentials = "Retrofit_demo_user";
        final String basic = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Content-Type", "application/json")
                                /*  .header("apiKey", "74d1192b64cbacd81a4667xftk84c4715da03f3")
                                  .header("Content-Type", "application/json")
                                  .header("Authorization", basic)*/
                                .method(original.method(), original.body());
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }


                })
                .connectTimeout(100000, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build();
       /* Gson gson = new GsonBuilder()
                .setLenient()
                .create();*/

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WebConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(client).build();


        return retrofit.create(ApiService.class);

    }
}


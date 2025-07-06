package com.dao.rjobhunt.others;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)  // 🔥 30s to connect
                .readTimeout(60, TimeUnit.SECONDS)     // 🔥 60s to read response
                .writeTimeout(60, TimeUnit.SECONDS)    // 🔥 60s to send request
                .retryOnConnectionFailure(true)        // 🔄 auto retry on failures
                .build();
    }
}

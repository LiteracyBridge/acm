package org.literacybridge.talkingbookapp.api_services

import aws.smithy.kotlin.runtime.util.asyncLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.literacybridge.talkingbookapp.helpers.API_URL
import org.literacybridge.talkingbookapp.helpers.dataStoreManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


//val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
//    val original = chain.request()
//    val requestBuilder = original.newBuilder()
//        .header("Accept", "application/json")
//        .header("Content-Type", "application/json")
//        .header("Authorization", "Bearer ${dataStoreManager.getAccessToken()}")
//        .method(original.method, original.body)
//
//    val request = requestBuilder.build()
//    chain.proceed(request)
//}.build()

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${dataStoreManager.accessToken}")
                .method(original.method, original.body)

            val request = requestBuilder.build()
            chain.proceed(request)
        }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    fun instance(): ApiService {
        return provideApiService(provideRetrofit(provideOkHttpClient()))
    }
}
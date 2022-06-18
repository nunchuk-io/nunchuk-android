package com.nunchuk.android.core.network.di

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nunchuk.android.core.network.ApiConstant.BASE_URL
import com.nunchuk.android.core.network.ApiConstant.BASE_URL_MATRIX
import com.nunchuk.android.core.network.ApiConstant.HTTP_CONNECT_TIMEOUT
import com.nunchuk.android.core.network.ApiConstant.HTTP_READ_TIMEOUT
import com.nunchuk.android.core.network.ApiConstant.HTTP_WRITE_TIMEOUT
import com.nunchuk.android.core.network.BuildConfig
import com.nunchuk.android.core.network.HeaderInterceptor
import com.nunchuk.android.network.util.MATRIX_HTTP_CLIENT
import com.nunchuk.android.network.util.MATRIX_LOGGING_INTERCEPTOR
import com.nunchuk.android.network.util.MATRIX_RETROFIT
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule @Inject constructor() {

    @Singleton
    @Provides
    fun provideGson(): Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Singleton
    @Provides
    fun provideConnectionSpecs(): List<ConnectionSpec> = Collections.singletonList(ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS).build())

    @Singleton
    @Provides
    fun provideNunchukRetrofit(gson: Gson, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .baseUrl(BASE_URL)
        .client(client)
        .build()

    @Singleton
    @Provides
    @Named(MATRIX_RETROFIT)
    fun provideMatrixRetrofit(
        gson: Gson,
        @Named(MATRIX_HTTP_CLIENT)
        client: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl(BASE_URL_MATRIX)
        .client(client)
        .build()

    @Singleton
    @Provides
    fun provideNunchukOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        headerInterceptor: HeaderInterceptor,
        connectionSpecs: List<ConnectionSpec>
    ): OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(HTTP_WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(headerInterceptor)
        .connectionSpecs(connectionSpecs)
        .build()

    @Singleton
    @Provides
    @Named(MATRIX_HTTP_CLIENT)
    fun provideMatrixOkHttpClient(
        @Named(MATRIX_LOGGING_INTERCEPTOR)
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(HTTP_WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    @Singleton
    @Provides
    fun provideLoggingInterceptor() = HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) {
            setLevel(Level.BODY)
        } else {
            setLevel(Level.NONE)
        }
    }

    // Due to large sync file, DON'T log response body
    @Singleton
    @Provides
    @Named(MATRIX_LOGGING_INTERCEPTOR)
    fun provideMatrixLoggingInterceptor() = HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) {
            setLevel(Level.BASIC)
        } else {
            setLevel(Level.NONE)
        }
    }

}
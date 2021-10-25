package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.api.AuthApi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object AuthNetworkModule {

    @Singleton
    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Singleton
    @Provides
    @Named("AuthClientV1_1")
    fun provideAuthApiV1_1(@Named("RetrofitClientV1_1") retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

}
package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.api.AuthApi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
internal object AuthNetworkModule {

    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

}
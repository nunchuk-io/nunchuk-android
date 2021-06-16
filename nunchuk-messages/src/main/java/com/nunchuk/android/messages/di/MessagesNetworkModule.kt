package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.api.UserApi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
internal object MessagesNetworkModule {

    @Provides
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

}
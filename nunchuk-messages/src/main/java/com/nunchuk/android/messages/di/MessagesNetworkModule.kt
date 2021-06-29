package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.api.ContactApi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
internal object MessagesNetworkModule {

    @Provides
    fun provideUserApi(retrofit: Retrofit): ContactApi = retrofit.create(ContactApi::class.java)

}
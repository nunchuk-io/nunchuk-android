package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.api.ContactApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ContactNetworkModule {

    @Singleton
    @Provides
    fun provideUserApi(retrofit: Retrofit): ContactApi = retrofit.create(ContactApi::class.java)

}
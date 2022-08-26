package com.nunchuk.android.signer.software.components.data.di

import com.nunchuk.android.signer.software.components.data.api.SignerSoftwareApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SignerSoftwareNetworkModule {

    @Singleton
    @Provides
    fun provideSignerSoftwareApi(retrofit: Retrofit): SignerSoftwareApi =
        retrofit.create(SignerSoftwareApi::class.java)

}
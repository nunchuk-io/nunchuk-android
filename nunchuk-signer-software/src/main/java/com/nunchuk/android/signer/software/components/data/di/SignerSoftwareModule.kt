package com.nunchuk.android.signer.software.components.data.di

import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.signer.software.components.data.repository.SignerSoftwareRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SignerSoftwareModule {

    @Binds
    @Singleton
    fun bindSignerSoftwareRepository(repository: SignerSoftwareRepositoryImpl): SignerSoftwareRepository

}
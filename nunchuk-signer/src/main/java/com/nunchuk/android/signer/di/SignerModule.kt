package com.nunchuk.android.signer.di

import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.signer.repository.KeyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SignerModule {

    @Binds
    @Singleton
    abstract fun provideKeyRepository(implementation: KeyRepositoryImpl) : KeyRepository
}
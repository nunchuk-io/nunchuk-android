package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.auth.data.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthDataModule {

    @Binds
    @Singleton
    fun bindAuthRepository(repository: AuthRepositoryImpl): AuthRepository

}
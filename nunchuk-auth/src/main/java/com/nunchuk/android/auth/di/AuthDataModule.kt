package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.auth.data.AuthRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal abstract class AuthDataModule {

    @Binds
    abstract fun bindAuthRepository(repository: AuthRepositoryImpl): AuthRepository

}
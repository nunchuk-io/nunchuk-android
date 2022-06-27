package com.nunchuk.android.main.di

import com.nunchuk.android.core.matrix.SyncStateHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SyncStateModule {

    @Singleton
    @Provides
    fun provideGlobalSyncLockState() = SyncStateHolder()
}
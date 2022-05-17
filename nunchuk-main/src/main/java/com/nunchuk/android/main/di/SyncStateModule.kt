package com.nunchuk.android.main.di

import com.nunchuk.android.core.matrix.SyncStateHolder
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SyncStateModule {

    @Singleton
    @Provides
    fun provideGlobalSyncLockState() = SyncStateHolder()
}
package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.SyncFileRepository
import com.nunchuk.android.core.repository.SyncFileRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal interface SyncFileDataModule {
    @Binds
    fun bindSyncFileRepository(repository: SyncFileRepositoryImpl): SyncFileRepository
}
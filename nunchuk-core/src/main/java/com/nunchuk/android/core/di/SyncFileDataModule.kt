package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.SyncFileRepository
import com.nunchuk.android.core.repository.SyncFileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncFileDataModule {
    @Binds
    fun bindSyncFileRepository(repository: SyncFileRepositoryImpl): SyncFileRepository
}
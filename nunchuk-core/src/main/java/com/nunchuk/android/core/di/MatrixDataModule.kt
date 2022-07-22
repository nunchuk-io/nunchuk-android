package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.MatrixAPIRepository
import com.nunchuk.android.core.repository.MatrixAPIRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MatrixDataModule {
    @Binds
    @Singleton
    fun bindMatrixAPIRepository(repository: MatrixAPIRepositoryImpl): MatrixAPIRepository
}
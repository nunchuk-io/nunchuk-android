package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.MatrixAPIRepository
import com.nunchuk.android.core.repository.MatrixAPIRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MatrixDataModule {
    @Binds
    fun bindMatrixAPIRepository(repository: MatrixAPIRepositoryImpl): MatrixAPIRepository
}
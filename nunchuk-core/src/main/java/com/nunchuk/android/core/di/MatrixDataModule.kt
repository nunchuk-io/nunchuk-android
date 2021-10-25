package com.nunchuk.android.core.di

import com.nunchuk.android.core.data.MatrixAPIRepository
import com.nunchuk.android.core.data.MatrixAPIRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal interface MatrixDataModule {
    @Binds
    fun bindMatrixAPIRepository(repository: MatrixAPIRepositoryImpl): MatrixAPIRepository
}
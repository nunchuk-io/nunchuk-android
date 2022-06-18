package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.PriceConverterAPIRepository
import com.nunchuk.android.core.repository.PriceConverterAPIRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindPriceConverterAPIRepository(repository: PriceConverterAPIRepositoryImpl): PriceConverterAPIRepository
}
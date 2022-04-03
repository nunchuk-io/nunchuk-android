package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.PriceConverterAPIRepository
import com.nunchuk.android.core.repository.PriceConverterAPIRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal interface DataModule {
    @Binds
    fun bindPriceConverterAPIRepository(repository: PriceConverterAPIRepositoryImpl): PriceConverterAPIRepository
}
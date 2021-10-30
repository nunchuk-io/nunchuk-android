package com.nunchuk.android.core.di

import com.nunchuk.android.core.data.PriceConverterAPIRepository
import com.nunchuk.android.core.data.PriceConverterAPIRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal interface DataModule {
    @Binds
    fun bindPriceConverterAPIRepository(repository: PriceConverterAPIRepositoryImpl): PriceConverterAPIRepository
}
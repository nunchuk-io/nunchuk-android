package com.nunchuk.android.core.di

import com.nunchuk.android.core.repository.BtcPriceRepository
import com.nunchuk.android.core.repository.BtcPriceRepositoryImpl
import com.nunchuk.android.core.repository.TransactionRepositoryImpl
import com.nunchuk.android.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    @Singleton
    fun bindPriceConverterAPIRepository(repository: BtcPriceRepositoryImpl): BtcPriceRepository

    @Binds
    @Singleton
    fun bindTransactionRepository(repository: TransactionRepositoryImpl): TransactionRepository
}
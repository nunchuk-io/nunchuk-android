package com.nunchuk.android.transaction.di

import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TransactionDomainModule {

    @Binds
    fun bindGetBlockchainExplorerUrlUseCase(useCase: GetBlockchainExplorerUrlUseCaseImpl): GetBlockchainExplorerUrlUseCase

}
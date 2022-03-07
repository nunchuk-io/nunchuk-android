package com.nunchuk.android.transaction.di

import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCaseImpl
import dagger.Binds
import dagger.Module

@Module(
    includes = [
        TransactionActivityModule::class,
        TransactionDomainModule::class,
    ]
)
interface TransactionProxyModule

@Module
internal interface TransactionDomainModule {

    @Binds
    fun bindGetBlockchainExplorerUrlUseCase(useCase: GetBlockchainExplorerUrlUseCaseImpl): GetBlockchainExplorerUrlUseCase

}
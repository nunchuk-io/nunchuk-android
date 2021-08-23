package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module

@Module
internal interface SharedWalletDomainModule {

    @Binds
    fun bindInitWalletUseCase(useCase: InitWalletUseCaseImpl): InitWalletUseCase

}

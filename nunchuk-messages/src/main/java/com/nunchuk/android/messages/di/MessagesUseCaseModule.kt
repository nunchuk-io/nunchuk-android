package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.usecase.AddContactUseCase
import com.nunchuk.android.messages.usecase.AddContactUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal interface MessagesUseCaseModule {

    @Binds
    fun bindAddContactUseCase(useCase: AddContactUseCaseImpl): AddContactUseCase

}
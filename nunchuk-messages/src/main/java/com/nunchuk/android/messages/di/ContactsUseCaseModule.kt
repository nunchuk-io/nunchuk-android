package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.usecase.contact.*
import dagger.Binds
import dagger.Module

@Module
internal interface ContactsUseCaseModule {

    @Binds
    fun bindSearchContactUseCase(useCase: SearchContactUseCaseImpl): SearchContactUseCase

    @Binds
    fun bindAddContactUseCase(useCase: AddContactUseCaseImpl): AddContactUseCase

    @Binds
    fun bindAutoCompleteSearchUseCase(useCase: AutoCompleteSearchUseCaseImpl): AutoCompleteSearchUseCase

    @Binds
    fun bindGetReceivedContactsUseCase(useCase: GetReceivedContactsUseCaseImpl): GetReceivedContactsUseCase

    @Binds
    fun bindGetSentContactsUseCase(useCase: GetSentContactsUseCaseImpl): GetSentContactsUseCase

}
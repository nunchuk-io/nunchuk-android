package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.usecase.*
import com.nunchuk.android.share.GetContactsUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ContactUseCaseModule {

    @Binds
    fun bindSearchContactUseCase(useCase: SearchContactUseCaseImpl): SearchContactUseCase

    @Binds
    fun bindAddContactUseCase(useCase: AddContactUseCaseImpl): AddContactUseCase

    @Binds
    fun bindInviteFriendUseCase(useCase: InviteFriendUseCaseImpl): InviteFriendUseCase

    @Binds
    fun bindAutoCompleteSearchUseCase(useCase: AutoCompleteSearchUseCaseImpl): AutoCompleteSearchUseCase

    @Binds
    fun bindGetReceivedContactsUseCase(useCase: GetReceivedContactsUseCaseImpl): GetReceivedContactsUseCase

    @Binds
    fun bindGetSentContactsUseCase(useCase: GetSentContactsUseCaseImpl): GetSentContactsUseCase

    @Binds
    fun bindGetContactsUseCase(useCase: GetContactsUseCaseImpl): GetContactsUseCase

    @Binds
    fun bindUpdateAvatarUseCase(useCase: UpdateAvatarUseCaseImpl): UpdateAvatarUseCase

}
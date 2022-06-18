package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.usecase.message.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MessagesUseCaseModule {

    @Binds
    fun bindCreateRoomUseCase(useCase: CreateRoomUseCaseImpl): CreateRoomUseCase

    @Binds
    fun bindCreateRoomWithTagUseCase(useCase: CreateRoomWithTagUseCaseImpl): CreateRoomWithTagUseCase

    @Binds
    fun bindCreateDirectRoomUseCase(useCase: CreateDirectRoomUseCaseImpl): CreateDirectRoomUseCase

    @Binds
    fun bindAddTagRoomUseCase(useCase: AddTagRoomUseCaseImpl): AddTagRoomUseCase

    @Binds
    fun bindJoinRoomUseCase(useCase: JoinRoomUseCaseImpl): JoinRoomUseCase

    @Binds
    fun bindCheckShowBannerNewChatUseCase(useCase: CheckShowBannerNewChatUseCaseImpl): CheckShowBannerNewChatUseCase

}
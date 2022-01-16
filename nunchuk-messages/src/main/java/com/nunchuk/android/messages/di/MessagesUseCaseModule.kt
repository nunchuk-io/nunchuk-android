package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.usecase.message.*
import dagger.Binds
import dagger.Module

@Module
internal interface MessagesUseCaseModule {

    @Binds
    fun bindGetRoomSummaryListUseCase(useCase: GetRoomSummaryListUseCaseImpl): GetRoomSummaryListUseCase

    @Binds
    fun bindCreateRoomUseCase(useCase: CreateRoomUseCaseImpl): CreateRoomUseCase

    @Binds
    fun bindCreateRoomWithTagUseCase(useCase: CreateRoomWithTagUseCaseImpl): CreateRoomWithTagUseCase

    @Binds
    fun bindCreateDirectRoomUseCase(useCase: CreateDirectRoomUseCaseImpl): CreateDirectRoomUseCase

    @Binds
    fun bindAddTagRoomUseCase(useCase: AddTagRoomUseCaseImpl): AddTagRoomUseCase

    @Binds
    fun bindLeaveRoomUseCase(useCase: LeaveRoomUseCaseImpl): LeaveRoomUseCase

    @Binds
    fun bindJoinRoomUseCase(useCase: JoinRoomUseCaseImpl): JoinRoomUseCase

}
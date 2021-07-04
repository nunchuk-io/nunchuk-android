package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.usecase.message.*
import dagger.Binds
import dagger.Module

@Module
internal interface MessagesUseCaseModule {

    @Binds
    fun bindGetRoomsUseCase(useCase: GetRoomSummaryListUseCaseImpl): GetRoomSummaryListUseCase

    @Binds
    fun bindCreateRoomUseCase(useCase: CreateRoomUseCaseImpl): CreateRoomUseCase

    @Binds
    fun bindCreateDirectRoomUseCase(useCase: CreateDirectRoomUseCaseImpl): CreateDirectRoomUseCase

    @Binds
    fun bindLeaveRoomUseCase(useCase: LeaveRoomUseCaseImpl): LeaveRoomUseCase

    @Binds
    fun bindJoinRoomUseCase(useCase: JoinRoomUseCaseImpl): JoinRoomUseCase

}
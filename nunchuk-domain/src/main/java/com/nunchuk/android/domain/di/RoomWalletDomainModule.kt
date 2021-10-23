package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module

@Module
internal interface RoomWalletDomainModule {

    @Binds
    fun bindInitWalletUseCase(useCase: InitWalletUseCaseImpl): InitWalletUseCase

    @Binds
    fun bindJoinWalletUseCase(useCase: JoinWalletUseCaseImpl): JoinWalletUseCase

    @Binds
    fun bindCancelWalletUseCase(useCase: CancelWalletUseCaseImpl): CancelWalletUseCase

    @Binds
    fun bindLeaveWalletUseCase(useCase: LeaveWalletUseCaseImpl): LeaveWalletUseCase

    @Binds
    fun bindGetRoomWalletUseCase(useCase: GetRoomWalletUseCaseImpl): GetRoomWalletUseCase

    @Binds
    fun bindGetAllRoomWalletsUseCase(useCase: GetAllRoomWalletsUseCaseImpl): GetAllRoomWalletsUseCase

    @Binds
    fun bindConsumeEventUseCase(useCase: ConsumeEventUseCaseImpl): ConsumeEventUseCase

    @Binds
    fun bindConsumerSyncEventUseCase(useCase: ConsumerSyncEventUseCaseImpl): ConsumerSyncEventUseCase

    @Binds
    fun bindEnableAutoBackupUseCase(useCase: EnableAutoBackupUseCaseImpl): EnableAutoBackupUseCase

    @Binds
    fun bindCreateSharedWalletUseCase(useCase: CreateSharedWalletUseCaseImpl): CreateSharedWalletUseCase

}

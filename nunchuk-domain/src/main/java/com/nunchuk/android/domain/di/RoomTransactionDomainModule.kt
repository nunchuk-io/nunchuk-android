package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.room.transaction.*
import dagger.Binds
import dagger.Module

@Module
internal interface RoomTransactionDomainModule {

    @Binds
    fun bindInitRoomTransactionUseCase(useCase: InitRoomTransactionUseCaseImpl): InitRoomTransactionUseCase

    @Binds
    fun bindSignRoomTransactionUseCase(useCase: SignRoomTransactionUseCaseImpl): SignRoomTransactionUseCase

    @Binds
    fun bindRejectRoomTransactionUseCase(useCase: RejectRoomTransactionUseCaseImpl): RejectRoomTransactionUseCase

    @Binds
    fun bindCancelRoomTransactionUseCase(useCase: CancelRoomTransactionUseCaseImpl): CancelRoomTransactionUseCase

    @Binds
    fun bindBroadcastRoomTransactionUseCase(useCase: BroadcastRoomTransactionUseCaseImpl): BroadcastRoomTransactionUseCase

    @Binds
    fun bindGetPendingTransactionsUseCase(useCase: GetPendingTransactionsUseCaseImpl): GetPendingTransactionsUseCase

    @Binds
    fun bindGetRoomTransactionUseCase(useCase: GetRoomTransactionUseCaseImpl): GetRoomTransactionUseCase

}

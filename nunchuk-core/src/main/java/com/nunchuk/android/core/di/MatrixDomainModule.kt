package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.domain.SendErrorEventUseCaseImpl
import com.nunchuk.android.core.matrix.*
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MatrixDomainModule {
    @Binds
    fun bindUploadFileUseCase(useCase: UploadFileUseCaseImpl): UploadFileUseCase

    @Binds
    fun bindRegisterDownloadBackUpFileUseCase(useCase: RegisterDownloadBackUpFileUseCaseImpl): RegisterDownloadBackUpFileUseCase

    @Binds
    fun bindRegisterConsumeSyncFileUseCase(useCase: ConsumeSyncFileUseCaseImpl): ConsumeSyncFileUseCase

    @Binds
    fun bindRegisterBackupFileUseCase(useCase: BackupFileUseCaseImpl): BackupFileUseCase

    @Binds
    fun bindRegisterConsumerSyncEventUseCase(useCase: ConsumerSyncEventUseCaseImpl): ConsumerSyncEventUseCase

    @Binds
    fun bindSyncStateMatrixUseCase(useCase: SyncStateMatrixUseCaseImpl): SyncStateMatrixUseCase

    @Binds
    fun bindLeaveRoomUseCase(useCase: LeaveRoomUseCaseImpl): LeaveRoomUseCase

    @Binds
    fun bindSendErrorEventUseCase(useCase: SendErrorEventUseCaseImpl): SendErrorEventUseCase
}
package com.nunchuk.android.core.di

import com.nunchuk.android.core.matrix.*
import com.nunchuk.android.core.matrix.DownloadFileUseCaseImpl
import com.nunchuk.android.core.matrix.UploadFileUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal interface MatrixDomainModule {
    @Binds
    fun bindUploadFileUseCase(useCase: UploadFileUseCaseImpl): UploadFileUseCase

    @Binds
    fun bindDownloadFileUseCase(useCase: DownloadFileUseCaseImpl): DownloadFileUseCase

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
}
package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.repository.SyncFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CreateOrUpdateSyncFileUseCase {
    fun execute(syncFileModel: SyncFileModel): Flow<Unit>
}

internal class CreateOrUpdateSyncFileUseCaseImpl @Inject constructor(
    private val syncFileRepository: SyncFileRepository
) : CreateOrUpdateSyncFileUseCase {

    override fun execute(syncFileModel: SyncFileModel) = flow {
        val existedSyncFiles = syncFileRepository.getSyncFiles(syncFileModel.userId)

        emit(
            if (existedSyncFiles.any { file -> file.fileJsonInfo == syncFileModel.fileJsonInfo }) {
                syncFileRepository.updateSyncFile(syncFileModel)
            } else {
                syncFileRepository.createSyncFile(syncFileModel)
            }
        )
    }

}

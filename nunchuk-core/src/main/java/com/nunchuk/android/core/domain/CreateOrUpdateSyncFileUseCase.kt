package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.repository.SyncFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface CreateOrUpdateSyncFileUseCase {
    fun execute(syncFileModel: SyncFileModel): Flow<Unit>
}

internal class CreateOrUpdateSyncFileUseCaseImpl @Inject constructor(
    private val syncFileRepository: SyncFileRepository
) : CreateOrUpdateSyncFileUseCase {

    override fun execute(syncFileModel: SyncFileModel) = syncFileRepository.getSyncFiles(syncFileModel.userId)
        .map { existedSyncFiles->
            if (existedSyncFiles.any { file -> file.fileJsonInfo == syncFileModel.fileJsonInfo }) {
                syncFileRepository.updateSyncFile(syncFileModel)
            } else {
                syncFileRepository.createSyncFile(syncFileModel)
            }
        }

}

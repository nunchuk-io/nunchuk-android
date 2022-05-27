package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.repository.SyncFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface DeleteSyncFileUseCase {
    fun execute(syncFileModel: SyncFileModel): Flow<Unit>
}

internal class DeleteSyncFileUseCaseImpl @Inject constructor(
    private val syncFileRepository: SyncFileRepository
) : DeleteSyncFileUseCase {

    override fun execute(syncFileModel: SyncFileModel) = flow {
        emit(
            syncFileRepository.deleteSyncFile(syncFileModel)
        )
    }

}

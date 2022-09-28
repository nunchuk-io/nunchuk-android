package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.repository.SyncFileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetSyncFileUseCase {
    fun execute(userId: String): Flow<List<SyncFileModel>>
}

internal class GetSyncFileUseCaseImpl @Inject constructor(
    private val syncFileRepository: SyncFileRepository
) : GetSyncFileUseCase {

    override fun execute(userId: String) = syncFileRepository.getSyncFiles(userId)
}

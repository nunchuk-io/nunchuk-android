package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.data.model.toEntity
import com.nunchuk.android.core.data.model.toModel
import com.nunchuk.android.persistence.dao.SyncFileDao
import com.nunchuk.android.persistence.updateOrInsert
import javax.inject.Inject

interface SyncFileRepository {

    fun getSyncFiles(userId: String): List<SyncFileModel>
    fun createOrUpdateSyncFile(model: SyncFileModel)
    fun deleteSyncFile(model: SyncFileModel)
    fun createSyncFile(model: SyncFileModel)
    fun updateSyncFile(model: SyncFileModel)
}

internal class SyncFileRepositoryImpl @Inject constructor(
    private val syncFileDao: SyncFileDao
) : SyncFileRepository {

    override fun getSyncFiles(userId: String) = syncFileDao.getSyncFiles(userId).blockingFirst().map { entity -> entity.toModel()}

    override fun createOrUpdateSyncFile(model: SyncFileModel) {
        syncFileDao.updateOrInsert(model.toEntity())
    }

    override fun deleteSyncFile(model: SyncFileModel) {
        syncFileDao.deleteSyncFilesByInfo(listOf(model.fileJsonInfo))
    }

    override fun createSyncFile(model: SyncFileModel) {
        syncFileDao.insert(model.toEntity())
    }

    override fun updateSyncFile(model: SyncFileModel) {
        syncFileDao.update(model.toEntity())
    }
}


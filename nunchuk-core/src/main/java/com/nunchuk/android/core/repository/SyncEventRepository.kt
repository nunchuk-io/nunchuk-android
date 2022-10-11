package com.nunchuk.android.core.repository

import com.nunchuk.android.persistence.dao.SyncEventDao
import com.nunchuk.android.persistence.entity.SyncEventEntity
import com.nunchuk.android.persistence.updateOrInsert
import javax.inject.Inject

interface SyncEventRepository {
    suspend fun save(eventId: String)
    suspend fun delete(eventId: String)
    suspend fun isSync(eventId: String) : Boolean
}

class SyncEventRepositoryImpl @Inject constructor(
    private val syncEventDao: SyncEventDao
) : SyncEventRepository {
    override suspend fun isSync(eventId: String) : Boolean {
        return syncEventDao.getEvent(eventId) != null
    }

    override suspend fun save(eventId: String) {
        syncEventDao.updateOrInsert(SyncEventEntity(eventId))
    }

    override suspend fun delete(eventId: String) {
        syncEventDao.delete(SyncEventEntity(eventId))
    }
}
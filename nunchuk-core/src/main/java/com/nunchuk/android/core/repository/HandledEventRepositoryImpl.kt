package com.nunchuk.android.core.repository

import com.nunchuk.android.persistence.dao.HandledEventDao
import com.nunchuk.android.persistence.entity.HandledEventEntity
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.HandledEventRepository
import javax.inject.Inject

class HandledEventRepositoryImpl @Inject constructor(
    private val handledEventDao: HandledEventDao
) : HandledEventRepository {
    override suspend fun isHandled(eventId: String): Boolean {
        return handledEventDao.getEvent(eventId) != null
    }

    override suspend fun save(eventId: String) {
        handledEventDao.updateOrInsert(HandledEventEntity(eventId))
    }
}
package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.entity.SyncEventEntity

@Dao
interface SyncEventDao : BaseDao<SyncEventEntity> {
    @Query("SELECT * FROM sync_event WHERE event_id = :eventId LIMIT 1")
    fun getEvent(eventId: String): SyncEventEntity?
}
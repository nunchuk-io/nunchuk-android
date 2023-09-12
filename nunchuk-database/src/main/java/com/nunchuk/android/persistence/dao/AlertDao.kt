package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ALERT
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.type.Chain

@Dao
interface AlertDao : BaseDao<AlertEntity> {
    @Query("SELECT * FROM $TABLE_ALERT WHERE group_id = :groupId AND chat_id = :chatId AND chain = :chain")
    fun getAlerts(groupId: String, chatId: String, chain: Chain): List<AlertEntity>

    @Transaction
    suspend fun updateData(
        newList: List<AlertEntity>,
        updateList: List<AlertEntity>,
        deleteList: List<AlertEntity>,
    ) {
        if (newList.isNotEmpty()) {
            insert(newList)
        }
        if (updateList.isNotEmpty()) {
            update(updateList)
        }
        if (deleteList.isNotEmpty()) {
            deletes(deleteList)
        }
    }
}
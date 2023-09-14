package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ALERT
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao : BaseDao<AlertEntity> {
    @Query("SELECT * FROM $TABLE_ALERT WHERE group_id = :groupId AND chat_id = :chatId AND chain = :chain")
    fun getAlerts(groupId: String, chatId: String, chain: Chain): Flow<List<AlertEntity>>

    @Transaction
    suspend fun updateData(
        updateOrInsertList: List<AlertEntity>,
        deleteList: List<AlertEntity>,
    ) {
        if (updateOrInsertList.isNotEmpty()) {
            updateOrInsert(updateOrInsertList)
        }

        if (deleteList.isNotEmpty()) {
            deletes(deleteList)
        }
    }
}
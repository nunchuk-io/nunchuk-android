package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ASSISTED_WALLET
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.persistence.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao : BaseDao<GroupEntity> {
    @Query("SELECT * FROM $TABLE_GROUP ORDER BY group_id")
    fun getGroups(): Flow<List<GroupEntity>>

    @Query("DELETE FROM $TABLE_GROUP where group_id =:id")
    suspend fun deleteGroup(id: String): Int

    @Query("DELETE FROM $TABLE_GROUP")
    suspend fun deleteAll()

    @Query("SELECT * FROM $TABLE_GROUP WHERE group_id =:id ")
    suspend fun getById(id: String): GroupEntity?
}
package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.persistence.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao : BaseDao<GroupEntity> {
    @Query("SELECT * FROM $TABLE_GROUP WHERE chatId = :chatId ORDER BY group_id ")
    fun getGroups(chatId: String): Flow<List<GroupEntity>>

    @Query("DELETE FROM $TABLE_GROUP WHERE group_id IN (:groupIds) AND chatId = :chatId")
    suspend fun deleteGroups(groupIds: List<String>, chatId: String): Int

    @Query("SELECT * FROM $TABLE_GROUP WHERE group_id =:id AND chatId = :chatId")
    fun getById(id: String, chatId: String): Flow<GroupEntity>

    @Query("SELECT * FROM $TABLE_GROUP WHERE group_id =:id AND chatId = :chatId")
    fun getGroupById(id: String, chatId: String): GroupEntity?
}
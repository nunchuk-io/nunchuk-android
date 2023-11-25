package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.persistence.entity.GroupEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao : BaseDao<GroupEntity> {
    companion object {
        const val GET_GROUPS_STATEMENT = "SELECT * FROM $TABLE_GROUP WHERE chatId = :chatId AND chain = :chain ORDER BY group_id"
    }

    @Query(GET_GROUPS_STATEMENT)
    fun getGroupsFlow(chatId: String, chain: Chain): Flow<List<GroupEntity>>

    @Query(GET_GROUPS_STATEMENT)
    fun getGroups(chatId: String, chain: Chain): List<GroupEntity>

    @Query("DELETE FROM $TABLE_GROUP WHERE group_id IN (:groupIds) AND chatId = :chatId")
    suspend fun deleteGroups(groupIds: List<String>, chatId: String): Int

    @Query("SELECT * FROM $TABLE_GROUP WHERE group_id =:id AND chatId = :chatId AND chain = :chain")
    fun getById(id: String, chatId: String, chain: Chain): Flow<GroupEntity>

    @Query("SELECT * FROM $TABLE_GROUP WHERE group_id =:id AND chatId = :chatId AND chain = :chain")
    fun getGroupById(id: String, chatId: String, chain: Chain): GroupEntity?
}
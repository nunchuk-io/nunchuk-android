package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.persistence.TABLE_GROUP_CHAT
import com.nunchuk.android.persistence.entity.GroupChatEntity
import com.nunchuk.android.persistence.entity.GroupEntity
import com.nunchuk.android.persistence.entity.KeyHealthStatusEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupChatDao : BaseDao<GroupChatEntity> {
    @Query("SELECT * FROM $TABLE_GROUP_CHAT WHERE chatId = :chatId AND chain = :chain")
    fun getGroupChats(chatId: String, chain: Chain): List<GroupChatEntity>

    @Query("SELECT * FROM $TABLE_GROUP_CHAT WHERE room_id =:roomId AND chatId = :chatId AND chain = :chain")
    fun getByRoomId(roomId: String, chatId: String, chain: Chain): GroupChatEntity?

    @Query("SELECT * FROM $TABLE_GROUP_CHAT WHERE group_id =:id AND chatId = :chatId AND chain = :chain")
    fun getGroupById(id: String, chatId: String, chain: Chain): GroupChatEntity?

    @Transaction
    suspend fun updateData(
        updateOrInsertList: List<GroupChatEntity>,
        deleteList: List<GroupChatEntity>,
    ) {
        if (updateOrInsertList.isNotEmpty()) {
            updateOrInsert(updateOrInsertList)
        }
        if (deleteList.isNotEmpty()) {
            deletes(deleteList)
        }
    }
}
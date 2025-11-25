package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ADD_DESKTOP_KEY
import com.nunchuk.android.persistence.entity.RequestAddKeyEntity
import com.nunchuk.android.type.Chain

@Dao
interface RequestAddKeyDao : BaseDao<RequestAddKeyEntity> {
    @Query("SELECT * FROM $TABLE_ADD_DESKTOP_KEY WHERE chat_id = :chatId AND chain = :chain AND step = :step AND tag = :tag AND group_id = :groupId")
    suspend fun getRequest(
        chatId: String,
        chain: Chain,
        step: MembershipStep,
        tag: String,
        groupId: String = ""
    ): RequestAddKeyEntity?

    @Query("SELECT * FROM $TABLE_ADD_DESKTOP_KEY WHERE chat_id = :chatId AND chain = :chain AND step = :step AND group_id = :groupId")
    suspend fun getRequest(
        chatId: String,
        chain: Chain,
        step: MembershipStep,
        groupId: String = ""
    ): RequestAddKeyEntity?

    @Query("SELECT * FROM $TABLE_ADD_DESKTOP_KEY WHERE request_id = :requestId")
    suspend fun getRequest(requestId: String): RequestAddKeyEntity?

    @Query("SELECT * FROM $TABLE_ADD_DESKTOP_KEY WHERE chat_id = :chatId AND chain = :chain AND group_id = :groupId")
    suspend fun getRequests(
        chatId: String,
        chain: Chain,
        groupId: String = ""
    ): List<RequestAddKeyEntity>

    @Query("SELECT * FROM $TABLE_ADD_DESKTOP_KEY WHERE group_id = :groupId")
    suspend fun getRequests(
        groupId: String = ""
    ): List<RequestAddKeyEntity>

    @Query("DELETE FROM $TABLE_ADD_DESKTOP_KEY WHERE chat_id = :chatId AND chain = :chain AND group_id = :groupId")
    suspend fun deleteRequests(chatId: String, chain: Chain, groupId: String = "")

    @Query("DELETE FROM $TABLE_ADD_DESKTOP_KEY WHERE group_id = :groupId")
    suspend fun deleteRequests(groupId: String)
}
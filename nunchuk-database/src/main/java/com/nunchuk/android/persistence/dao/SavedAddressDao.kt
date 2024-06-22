package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_SAVED_ADDRESS
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.persistence.entity.SavedAddressEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAddressDao : BaseDao<SavedAddressEntity> {

    @Query("SELECT * FROM $TABLE_SAVED_ADDRESS WHERE chat_id = :chatId AND chain = :chain")
    fun getSavedAddressFlow(
        chatId: String,
        chain: Chain
    ): Flow<List<SavedAddressEntity>>

    @Query("SELECT * FROM $TABLE_SAVED_ADDRESS WHERE chat_id = :chatId AND chain = :chain")
    suspend fun getSavedAddressList(
        chatId: String,
        chain: Chain
    ): List<SavedAddressEntity>

    @Query("SELECT * FROM $TABLE_SAVED_ADDRESS WHERE chat_id = :chatId AND chain = :chain AND address = :address")
    fun getByAddress(address: String, chatId: String, chain: Chain): SavedAddressEntity?

    @Transaction
    suspend fun updateData(
        updateOrInsertList: List<SavedAddressEntity>,
        deleteList: List<SavedAddressEntity>,
    ) {
        if (updateOrInsertList.isNotEmpty()) {
            updateOrInsert(updateOrInsertList)
        }

        if (deleteList.isNotEmpty()) {
            deletes(deleteList)
        }
    }
}
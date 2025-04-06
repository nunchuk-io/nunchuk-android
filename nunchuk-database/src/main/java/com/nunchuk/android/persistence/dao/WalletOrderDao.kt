package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.persistence.entity.WalletOrderEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletOrderDao {
    @Query("SELECT * FROM wallet_order WHERE chat_id = :chatId AND chain = :chain")
    fun getAll(chatId: String, chain: Chain): Flow<List<WalletOrderEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entities: List<WalletOrderEntity>)

    @Query("DELETE FROM wallet_order WHERE chat_id = :chatId AND chain = :chain")
    suspend fun deleteAll(chatId: String, chain: Chain)

    @Transaction
    suspend fun replaceWalletOrders(chatId: String, chain: Chain, newOrders: List<WalletOrderEntity>) {
        // Delete existing wallet orders for the given chatId and chain
        deleteAll(chatId, chain)
        // Insert new wallet orders
        insert(newOrders)
    }
}
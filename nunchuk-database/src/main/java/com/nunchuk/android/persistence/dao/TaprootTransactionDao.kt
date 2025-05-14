package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nunchuk.android.persistence.entity.TaprootTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaprootTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaprootTransactionEntity)

    @Query("DELETE FROM taproot_transaction WHERE transaction_id = :transactionId")
    suspend fun delete(transactionId: String)

    @Query("SELECT * FROM taproot_transaction WHERE transaction_id = :transactionId")
    fun getByTransactionId(transactionId: String): Flow<TaprootTransactionEntity?>
} 
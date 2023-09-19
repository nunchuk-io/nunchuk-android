package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_DUMMY_TRANSACTION
import com.nunchuk.android.persistence.entity.DummyTransactionEntity

@Dao
interface DummyTransactionDao : BaseDao<DummyTransactionEntity> {
    @Query("SELECT * FROM $TABLE_DUMMY_TRANSACTION WHERE id =:id")
    suspend fun getById(id: String): DummyTransactionEntity
}
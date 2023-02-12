package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ASSISTED_WALLET
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistedWalletDao : BaseDao<AssistedWalletEntity> {
    @Query("SELECT * FROM $TABLE_ASSISTED_WALLET")
    fun getAssistedWallets(): Flow<List<AssistedWalletEntity>>

    @Query("DELETE FROM $TABLE_ASSISTED_WALLET where local_id in (:ids)")
    suspend fun deleteBatch(ids: List<String>)

    @Query("DELETE FROM $TABLE_ASSISTED_WALLET")
    suspend fun deleteAll()

    @Query("SELECT * FROM $TABLE_ASSISTED_WALLET WHERE local_id =:id ")
    suspend fun getById(id: String): AssistedWalletEntity
}
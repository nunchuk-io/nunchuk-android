/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ASSISTED_WALLET
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistedWalletDao : BaseDao<AssistedWalletEntity> {
    @Query("SELECT * FROM $TABLE_ASSISTED_WALLET ORDER BY id")
    fun getAssistedWalletsFlow(): Flow<List<AssistedWalletEntity>>

    @Query("SELECT * FROM $TABLE_ASSISTED_WALLET ORDER BY id")
    fun getAssistedWallets(): List<AssistedWalletEntity>

    @Query("DELETE FROM $TABLE_ASSISTED_WALLET where local_id in (:ids)")
    suspend fun deleteBatch(ids: List<String>) : Int

    @Query("DELETE FROM $TABLE_ASSISTED_WALLET")
    suspend fun deleteAll()

    @Query("SELECT * FROM $TABLE_ASSISTED_WALLET WHERE local_id =:id ")
    suspend fun getById(id: String): AssistedWalletEntity?

    @Query("SELECT * FROM $TABLE_ASSISTED_WALLET WHERE group_id =:id ")
    suspend fun getByGroupId(id: String): AssistedWalletEntity?

    @Query("DELETE FROM $TABLE_ASSISTED_WALLET WHERE group_id IN (:groupIds)")
    suspend fun deleteByGroupIds(groupIds: List<String>): Int

    @Query("DELETE FROM $TABLE_ASSISTED_WALLET WHERE local_id NOT IN (:ids) AND group_id = ''")
    suspend fun deleteAllPersonalWalletsExcept(ids: List<String>): Int
}
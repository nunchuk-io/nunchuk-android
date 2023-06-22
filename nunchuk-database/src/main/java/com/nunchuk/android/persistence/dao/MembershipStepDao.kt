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
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_MEMBERSHIP_STEP
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipStepDao : BaseDao<MembershipStepEntity> {
    @Query("SELECT * FROM $TABLE_MEMBERSHIP_STEP WHERE chat_id = :chatId AND chain = :chain AND `plan` = :plan")
    fun getSteps(chatId: String, chain: Chain, plan: MembershipPlan): Flow<List<MembershipStepEntity>>

    @Query("SELECT * FROM $TABLE_MEMBERSHIP_STEP WHERE chat_id = :chatId AND chain = :chain AND step = :step")
    suspend fun getStep(chatId: String, chain: Chain, step: MembershipStep): MembershipStepEntity?

    @Query("SELECT * FROM $TABLE_MEMBERSHIP_STEP WHERE chat_id = :email AND chain = :chain AND master_signer_id = :masterSignerId")
    suspend fun getStepByMasterSignerId(
        email: String,
        chain: Chain,
        masterSignerId: String
    ): MembershipStepEntity?

    @Query("DELETE FROM $TABLE_MEMBERSHIP_STEP WHERE chat_id = :chatId AND chain = :chain AND master_signer_id = :masterSignerId")
    suspend fun deleteByMasterSignerId(chatId: String, chain: Chain, masterSignerId: String)

    @Query("DELETE FROM $TABLE_MEMBERSHIP_STEP WHERE chat_id = :chatId AND chain = :chain")
    suspend fun deleteStepByChatId(chain: Chain, chatId: String)
}
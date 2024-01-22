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

package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.persistence.TABLE_ASSISTED_WALLET

@Entity(tableName = TABLE_ASSISTED_WALLET)
data class AssistedWalletEntity(
    @PrimaryKey
    @ColumnInfo(name = "local_id")
    val localId: String,
    @ColumnInfo(name = "group_id", defaultValue = "")
    val groupId: String = "",
    @ColumnInfo(name = "id", defaultValue = "0")
    val id: Long,
    @ColumnInfo(name = "plan")
    val plan: MembershipPlan,
    @ColumnInfo(name = "is_set_up_inheritance")
    val isSetupInheritance: Boolean = false,
    @ColumnInfo(name = "register_coldcard_count", defaultValue = "0")
    val registerColdcardCount: Int = 0,
    @ColumnInfo(name = "register_airgap_count", defaultValue = "0")
    val registerAirgapCount: Int = 0,
    @ColumnInfo(name = "ext")
    val ext: String? = null, // AssistedWalletBriefExt,
    @ColumnInfo(name = "primary_membership_id")
    val primaryMembershipId: String? = null,
    @ColumnInfo(name = "alias", defaultValue = "")
    val alias: String = "",
)
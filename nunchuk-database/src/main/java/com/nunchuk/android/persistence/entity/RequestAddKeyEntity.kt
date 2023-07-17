/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.persistence.TABLE_ADD_DESKTOP_KEY
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_ADD_DESKTOP_KEY)
data class RequestAddKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "request_id")
    val requestId: String = "",
    @ColumnInfo(name = "chat_id")
    val chatId: String = "",
    @ColumnInfo(name = "step")
    val step: MembershipStep = MembershipStep.IRON_ADD_HARDWARE_KEY_1,
    @ColumnInfo(name = "chain")
    val chain: Chain = Chain.MAIN,
    @ColumnInfo(name = "tag", defaultValue = "")
    val tag: String = ""
)
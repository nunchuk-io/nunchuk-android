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

package com.nunchuk.android.persistence

import androidx.room.TypeConverter
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.type.SignerType

class Converters {
    @TypeConverter
    fun fromMembershipStepInt(value: Int): MembershipStep {
        return MembershipStep.entries[value]
    }

    @TypeConverter
    fun membershipStepToInt(value: MembershipStep): Int {
        return value.ordinal
    }

    @TypeConverter
    fun fromMembershipPlanInt(value: Int): MembershipPlan {
        return MembershipPlan.entries[value]
    }

    @TypeConverter
    fun membershipPlanToInt(value: MembershipPlan): Int {
        return value.ordinal
    }

    @TypeConverter
    fun fromVerifyTypeInt(value: Int): VerifyType {
        return VerifyType.entries[value]
    }

    @TypeConverter
    fun verifyTypeToInt(value: VerifyType): Int {
        return value.ordinal
    }

    @TypeConverter
    fun fromSignerTypesString(value: String): List<SignerType> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { SignerType.valueOf(it) }
    }

    @TypeConverter
    fun signerTypesToString(value: List<SignerType>): String {
        return value.joinToString(",")
    }
}

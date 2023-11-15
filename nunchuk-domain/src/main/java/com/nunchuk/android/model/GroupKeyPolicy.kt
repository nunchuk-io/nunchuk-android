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

package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupKeyPolicy(
    val autoBroadcastTransaction: Boolean = false,
    val signingDelayInSeconds: Int = 0,
    val spendingPolicies: Map<String, SpendingPolicy> = emptyMap(),
    val isApplyAll: Boolean = false
) : Parcelable {

    companion object {
        const val ONE_HOUR_TO_SECONDS = 60 * 60
        const val ONE_MINUTE_TO_SECONDS = 60
    }

    fun getSigningDelayInHours() = signingDelayInSeconds / ONE_HOUR_TO_SECONDS
    fun getSigningDelayInMinutes() =  (signingDelayInSeconds % ONE_HOUR_TO_SECONDS) / ONE_MINUTE_TO_SECONDS
}
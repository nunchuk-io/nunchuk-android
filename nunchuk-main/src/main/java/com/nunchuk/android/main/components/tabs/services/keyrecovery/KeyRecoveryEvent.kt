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

package com.nunchuk.android.main.components.tabs.services.keyrecovery

import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.byzantine.AssistedWalletRole

sealed class KeyRecoveryEvent {
    data class Loading(val isLoading: Boolean) : KeyRecoveryEvent()
    data class ProcessFailure(val message: String) : KeyRecoveryEvent()
    data class ItemClick(val item: KeyRecoveryActionItem) : KeyRecoveryEvent()
    data class CheckPasswordSuccess(val item: KeyRecoveryActionItem, val verifyToken: String) :
        KeyRecoveryEvent()
}

data class KeyRecoveryState(
    val actionItems: List<KeyRecoveryActionItem> = arrayListOf(),
    val plan: MembershipPlan = MembershipPlan.NONE,
    val myUserRole: String = AssistedWalletRole.NONE.name,
) {
    fun initRowItems(): List<KeyRecoveryActionItem> {
        val items = mutableListOf<KeyRecoveryActionItem>()
        when (plan) {
            MembershipPlan.BYZANTINE, MembershipPlan.BYZANTINE_PRO -> {
                items.add(KeyRecoveryActionItem.StartKeyRecovery)
                if (myUserRole == AssistedWalletRole.MASTER.name) {
                    items.add(KeyRecoveryActionItem.UpdateRecoveryQuestion)
                }
            }

            else -> {
                items.add(KeyRecoveryActionItem.StartKeyRecovery)
                items.add(KeyRecoveryActionItem.UpdateRecoveryQuestion)
            }
        }
        return items
    }
}

sealed class KeyRecoveryActionItem(val title: Int) {
    data object StartKeyRecovery : KeyRecoveryActionItem(R.string.nc_start_key_recovery)
    data object UpdateRecoveryQuestion : KeyRecoveryActionItem(R.string.nc_update_recovery_question)
}


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

package com.nunchuk.android.settings

import com.nunchuk.android.model.MembershipPlan

sealed class AccountSettingEvent {
    data class Loading(val isLoading: Boolean) : AccountSettingEvent()
    data object RequestDeleteSuccess : AccountSettingEvent()
    data object DeletePrimaryKeySuccess : AccountSettingEvent()
    data class Error(val message: String) : AccountSettingEvent()
    data class CheckNeedPassphraseSent(val isNeeded: Boolean) : AccountSettingEvent()
    data class CheckPasswordSuccess(val token: String) : AccountSettingEvent()
    data object None : AccountSettingEvent()
}

data class AccountSettingState(
    val isSyncEnable: Boolean = false,
    val hasAssistedWallets: Boolean = false,
    val plans: List<MembershipPlan> = emptyList(),
)

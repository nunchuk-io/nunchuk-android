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

package com.nunchuk.android.settings.walletsecurity

import com.nunchuk.android.model.setting.WalletSecuritySetting

data class WalletSecuritySettingState(
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
    val walletPin: String = "",
    val isEnablePassphrase: Boolean = false
)

sealed class WalletSecuritySettingEvent {
    object UpdateConfigSuccess : WalletSecuritySettingEvent()
    object CheckPasswordSuccess : WalletSecuritySettingEvent()
    object CheckPassphraseSuccess : WalletSecuritySettingEvent()
    data class CheckWalletPin(val match: Boolean, val isHideWalletDetailFlow: Boolean) : WalletSecuritySettingEvent()
    data class Loading(val loading: Boolean) : WalletSecuritySettingEvent()
    data class Error(val message: String) : WalletSecuritySettingEvent()
    object None : WalletSecuritySettingEvent()
}
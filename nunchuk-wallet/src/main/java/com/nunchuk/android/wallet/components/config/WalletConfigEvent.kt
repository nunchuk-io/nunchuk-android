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

package com.nunchuk.android.wallet.components.config

sealed class WalletConfigEvent {

    object UpdateNameSuccessEvent : WalletConfigEvent()

    data class UpdateNameErrorEvent(val message: String) : WalletConfigEvent()

    data class WalletDetailsError(val message: String) : WalletConfigEvent()

    object DeleteWalletSuccess : WalletConfigEvent()

    data class VerifyPasswordSuccess(val token: String, val xfp: String) : WalletConfigEvent()
    class Loading(val isLoading: Boolean) : WalletConfigEvent()
}
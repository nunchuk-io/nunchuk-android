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

package com.nunchuk.android.share.result

object GlobalResultKey {
    const val SIGNATURE_EXTRA = "_a"
    const val TRANSACTION_EXTRA = "_b"
    const val EXTRA_HEALTH_CHECK_XFP = "_c"
    const val EXTRA_DUMMY_TX_TYPE = "_d"
    const val SECURITY_QUESTION_TOKEN = "_e"
    const val CONFIRM_CODE_TOKEN = "_f"
    const val CONFIRM_CODE_NONCE = "_g"
    const val CONFIRM_CODE = "_h"
    const val UPDATE_INHERITANCE = "_i"
    const val WALLET_ID = "_j"
    const val EXTRA_COINS = "_k"
    const val EXTRA_SIGNER = "_l"
    const val DUMMY_TX_ID = "_m"
    const val REQUIRED_SIGNATURES = "_n"
    const val DUMMY_TX_INTRO_DO_LATER = "_o"
}
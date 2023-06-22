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

package com.nunchuk.android.messages.util

const val KEY = "msgtype"

enum class WalletEventType(val type: String) {
    INIT("io.nunchuk.wallet.init"),
    JOIN("io.nunchuk.wallet.join"),
    CREATE("io.nunchuk.wallet.create"),
    LEAVE("io.nunchuk.wallet.leave"),
    CANCEL("io.nunchuk.wallet.cancel"),
    READY("io.nunchuk.wallet.ready");

    companion object {
        fun of(type: String): WalletEventType = values().firstOrNull { it.type == type } ?: throw IllegalArgumentException("Invalid wallet type $type")
    }

}

enum class TransactionEventType(val type: String) {
    INIT("io.nunchuk.transaction.init"),
    SIGN("io.nunchuk.transaction.sign"),
    REJECT("io.nunchuk.transaction.reject"),
    RECEIVE("io.nunchuk.transaction.receive"),
    CANCEL("io.nunchuk.transaction.cancel"),
    READY("io.nunchuk.transaction.ready"),
    BROADCAST("io.nunchuk.transaction.broadcast");

    companion object {
        fun of(type: String) = values().firstOrNull { it.type == type } ?: throw IllegalArgumentException("Invalid transaction type $type")
    }
}

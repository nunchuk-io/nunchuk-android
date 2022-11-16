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

package com.nunchuk.android.core.constants

object Constants {
    const val MAIN_NET_HOST = "mainnet.nunchuk.io:51001"
    const val TEST_NET_HOST = "testnet.nunchuk.io:50001"
    const val SIG_NET_HOST = "signet.nunchuk.io:50002"

    const val TESTNET_URL_TEMPLATE = "https://mempool.space/testnet/tx/"
    const val MAINNET_URL_TEMPLATE = "https://mempool.space/tx/"
    const val BLOCKSTREAM_MAINNET_ADDRESS_TEMPLATE = "https://mempool.space/address/"
    const val GLOBAL_SIGNET_EXPLORER = "https://explorer.bc-2.jp"

}

enum class RoomAction {
    SEND, RECEIVE
}

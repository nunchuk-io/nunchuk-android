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

package com.nunchuk.android.messages.components.detail

enum class MessageType(val index: Int) {
    TYPE_CHAT_MINE(0),
    TYPE_CHAT_PARTNER(1),
    TYPE_NOTIFICATION(2),
    TYPE_DATE(3),
    TYPE_NUNCHUK_WALLET_CARD(4),
    TYPE_NUNCHUK_WALLET_NOTIFICATION(5),
    TYPE_NUNCHUK_TRANSACTION_CARD(6),
    TYPE_NUNCHUK_TRANSACTION_NOTIFICATION(7),
    TYPE_NUNCHUK_BANNER_NEW_CHAT(8)
}
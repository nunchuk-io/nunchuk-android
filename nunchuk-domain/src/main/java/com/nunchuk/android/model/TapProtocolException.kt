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

object TapProtocolException {
    const val TAP_PROTOCOL_ERROR = -6000
    const val UNLUCKY_NUMBER = TAP_PROTOCOL_ERROR - 205
    const val BAD_ARGUMENT = TAP_PROTOCOL_ERROR - 400
    const val BAD_AUTH = TAP_PROTOCOL_ERROR - 401
    const val NEED_AUTH = TAP_PROTOCOL_ERROR - 403
    const val UNKNOW_COMMAND = TAP_PROTOCOL_ERROR - 404
    const val INVALID_COMMAND = TAP_PROTOCOL_ERROR - 405
    const val INVALID_STATE = TAP_PROTOCOL_ERROR - 406
    const val WEAK_NONCE = TAP_PROTOCOL_ERROR - 417
    const val BAD_CBOR = TAP_PROTOCOL_ERROR - 422
    const val BACKUP_FIRST = TAP_PROTOCOL_ERROR - 425
    const val RATE_LIMIT = TAP_PROTOCOL_ERROR - 429
}
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

package com.nunchuk.android.share

import io.realm.internal.Keep

@Keep
enum class ColdcardAction {
    CREATE, RECOVER_KEY, RECOVER_MULTI_SIG_WALLET, RECOVER_SINGLE_SIG_WALLET, PARSE_MULTISIG_WALLET, PARSE_SINGLE_SIG_WALLET, INHERITANCE_PASSPHRASE_QUESTION, VERIFY_KEY, UPLOAD_BACKUP
}

val ColdcardAction.isParseAction: Boolean
    get() = this == ColdcardAction.PARSE_MULTISIG_WALLET || this == ColdcardAction.PARSE_SINGLE_SIG_WALLET
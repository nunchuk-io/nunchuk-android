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

package com.nunchuk.android.signer.software.components.primarykey.passphrase

import com.nunchuk.android.core.domain.CheckPassphrasePrimaryKeyUseCase

sealed class PKeyEnterPassphraseEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyEnterPassphraseEvent()
    data class CheckPassphraseSuccess(val result: CheckPassphrasePrimaryKeyUseCase.Result) : PKeyEnterPassphraseEvent()
    object CheckPassphraseError : PKeyEnterPassphraseEvent()
}

data class PKeyEnterPassphraseState(
    val args: PKeyEnterPassphraseArgs? = null,
    val passphrase: String = ""
)
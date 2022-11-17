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

package com.nunchuk.android.signer.software.components.passphrase

import com.nunchuk.android.model.MasterSigner

sealed class SetPassphraseEvent {
    data class LoadingEvent(val loading: Boolean) : SetPassphraseEvent()
    object PassPhraseRequiredEvent : SetPassphraseEvent()
    object PassPhraseValidEvent : SetPassphraseEvent()
    object ConfirmPassPhraseRequiredEvent : SetPassphraseEvent()
    object ConfirmPassPhraseNotMatchedEvent : SetPassphraseEvent()
    data class CreateSoftwareSignerCompletedEvent(
        val masterSigner: MasterSigner? = null,
        val skipPassphrase: Boolean
    ) : SetPassphraseEvent()

    data class CreateSoftwareSignerErrorEvent(val message: String) : SetPassphraseEvent()
    data class CreateWalletSuccessEvent(val walletId: String) : SetPassphraseEvent()
    data class CreateWalletErrorEvent(val message: String) : SetPassphraseEvent()
}

data class SetPassphraseState(
    val passphrase: String = "",
    val confirmPassphrase: String = ""
)
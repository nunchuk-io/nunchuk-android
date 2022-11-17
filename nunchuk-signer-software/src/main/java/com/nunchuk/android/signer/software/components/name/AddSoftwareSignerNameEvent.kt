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

package com.nunchuk.android.signer.software.components.name

sealed class AddSoftwareSignerNameEvent {
    data class SignerNameInputCompletedEvent(val signerName: String) : AddSoftwareSignerNameEvent()
    object SignerNameRequiredEvent : AddSoftwareSignerNameEvent()
    data class ImportPrimaryKeyErrorEvent(val message: String) : AddSoftwareSignerNameEvent()
    data class LoadingEvent(val loading: Boolean) : AddSoftwareSignerNameEvent()
    data class InitFailure(val message: String) : AddSoftwareSignerNameEvent()
    data class GetTurnOnNotificationSuccess(val isTurnOn: Boolean) : AddSoftwareSignerNameEvent()
}

data class AddSoftwareSignerNameState(
    val args: AddSoftwareSignerNameArgs? = null,
    val signerName: String = ""
)
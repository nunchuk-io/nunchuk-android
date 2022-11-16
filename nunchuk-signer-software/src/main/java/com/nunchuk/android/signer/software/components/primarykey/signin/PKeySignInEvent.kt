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

package com.nunchuk.android.signer.software.components.primarykey.signin

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.PrimaryKey

sealed class PKeySignInEvent {
    data class LoadingEvent(val loading: Boolean) : PKeySignInEvent()
    data class ProcessErrorEvent(val message: String) : PKeySignInEvent()
    object SignInSuccessEvent : PKeySignInEvent()
    data class InitFailure(val message: String) : PKeySignInEvent()
}

data class PKeySignInState(
    val primaryKey: PrimaryKey? = null,
    val staySignedIn: Boolean = false,
    val appSettings: AppSettings? = null
)
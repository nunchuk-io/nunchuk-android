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

package com.nunchuk.android.auth.components.signin

import com.nunchuk.android.core.network.ErrorDetail

internal sealed class SignInEvent {
    data object EmailRequiredEvent : SignInEvent()
    data object EmailValidEvent : SignInEvent()
    data object EmailInvalidEvent : SignInEvent()
    data object PasswordRequiredEvent : SignInEvent()
    data object NameRequiredEvent : SignInEvent()
    data object NameValidEvent : SignInEvent()
    data object PasswordValidEvent : SignInEvent()
    data class ProcessingEvent(val isLoading: Boolean = true) : SignInEvent()
    data class SignInSuccessEvent(val ignoreCheckBiometric: Boolean = false) : SignInEvent()
    data class SignInErrorEvent(val code: Int? = null, val message: String? = null, val errorDetail: ErrorDetail? = null) : SignInEvent()
    data class RequireChangePassword(val isNew: Boolean) : SignInEvent()
    data object OpenMainScreen: SignInEvent()
}
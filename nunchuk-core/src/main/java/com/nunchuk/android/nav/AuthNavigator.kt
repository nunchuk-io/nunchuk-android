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

package com.nunchuk.android.nav

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.account.SignInType

interface AuthNavigator {
    fun openSignInScreen(
        activityContext: Context,
        isNeedNewTask: Boolean = true,
        isAccountDeleted: Boolean = false,
        type: SignInType = SignInType.EMAIL,
    )

    fun openSignUpScreen(activityContext: Context)

    fun openChangePasswordScreen(activityContext: Context, isNewAccount: Boolean = false)

    fun openRecoverPasswordScreen(activityContext: Context, email: String)

    fun openForgotPasswordScreen(activityContext: Context)

    fun openVerifyNewDeviceScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        email: String,
        loginHalfToken: String,
        deviceId: String,
        staySignedIn: Boolean
    )
}
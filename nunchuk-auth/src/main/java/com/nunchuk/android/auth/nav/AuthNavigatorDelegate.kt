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

package com.nunchuk.android.auth.nav

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.auth.components.changepass.ChangePasswordActivity
import com.nunchuk.android.auth.components.forgot.ForgotPasswordActivity
import com.nunchuk.android.auth.components.recover.RecoverPasswordActivity
import com.nunchuk.android.auth.components.signin.SignInActivity
import com.nunchuk.android.auth.components.signup.SignUpActivity
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceActivity
import com.nunchuk.android.nav.AuthNavigator

interface AuthNavigatorDelegate : AuthNavigator {

    override fun openSignInScreen(activityContext: Context, isNeedNewTask: Boolean, isAccountDeleted: Boolean) {
        SignInActivity.start(activityContext, isNeedNewTask, isAccountDeleted)
    }

    override fun openSignUpScreen(activityContext: Context) {
        SignUpActivity.start(activityContext)
    }

    override fun openChangePasswordScreen(activityContext: Context) {
        ChangePasswordActivity.start(activityContext)
    }

    override fun openRecoverPasswordScreen(activityContext: Context, email: String) {
        RecoverPasswordActivity.start(activityContext = activityContext, email = email)
    }

    override fun openForgotPasswordScreen(activityContext: Context) {
        ForgotPasswordActivity.start(activityContext)
    }

    override fun openVerifyNewDeviceScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        email: String,
        loginHalfToken: String,
        deviceId: String,
        staySignedIn: Boolean
    ) {
        launcher.launch(VerifyNewDeviceActivity.buildIntent(activityContext, email, loginHalfToken, deviceId, staySignedIn))
    }
}
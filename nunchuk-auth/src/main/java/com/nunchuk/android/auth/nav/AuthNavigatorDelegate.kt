package com.nunchuk.android.auth.nav

import android.content.Context
import com.nunchuk.android.auth.components.changepass.ChangePasswordActivity
import com.nunchuk.android.auth.components.forgot.ForgotPasswordActivity
import com.nunchuk.android.auth.components.recover.RecoverPasswordActivity
import com.nunchuk.android.auth.components.signin.SignInActivity
import com.nunchuk.android.auth.components.signup.SignUpActivity
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceActivity
import com.nunchuk.android.nav.AuthNavigator

interface AuthNavigatorDelegate : AuthNavigator {

    override fun openSignInScreen(activityContext: Context) {
        SignInActivity.start(activityContext)
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

    override fun openVerifyNewDeviceScreen(activityContext: Context, email: String, loginHalfToken: String, deviceId: String, staySignedIn: Boolean) {
        VerifyNewDeviceActivity.start(
            activityContext = activityContext,
            email = email,
            loginHalfToken = loginHalfToken,
            deviceId = deviceId,
            staySignedIn = staySignedIn
        )
    }
}
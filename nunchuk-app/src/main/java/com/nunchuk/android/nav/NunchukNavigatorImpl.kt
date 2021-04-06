package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.app.intro.IntroActivity
import com.nunchuk.android.auth.components.changepass.ChangePasswordActivity
import com.nunchuk.android.auth.components.forgot.ForgotPasswordActivity
import com.nunchuk.android.auth.components.recover.RecoverPasswordActivity
import com.nunchuk.android.auth.components.signin.SignInActivity
import com.nunchuk.android.auth.components.signup.SignUpActivity
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.add.AddSignerActivity
import com.nunchuk.android.signer.details.SignerInfoActivity
import javax.inject.Inject

internal class NunchukNavigatorImpl @Inject constructor() : NunchukNavigator {

    override fun openSignInScreen(activityContext: Context) {
        SignInActivity.start(activityContext)
    }

    override fun openSignUpScreen(activityContext: Context) {
        SignUpActivity.start(activityContext)
    }

    override fun openIntroScreen(activityContext: Context) {
        IntroActivity.start(activityContext)
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

    override fun openMainScreen(activityContext: Context) {
        MainActivity.start(activityContext)
    }

    override fun openSignerIntroScreen(activityContext: Context) {
        SignerIntroActivity.start(activityContext)
    }

    override fun openSignerInfoScreen(activityContext: Context, signerName: String, signerSpec: String, justAdded: Boolean) {
        SignerInfoActivity.start(
            activityContext = activityContext,
            signerName = signerName,
            signerSpec = signerSpec,
            justAdded = justAdded
        )
    }

    override fun openAddSignerScreen(activityContext: Context) {
        AddSignerActivity.start(activityContext)
    }

    override fun openWalletIntroScreen(activityContext: Context) {
    }

    override fun openAddWalletScreen(activityContext: Context) {
    }

}
package com.nunchuk.android.nav

import android.content.Context

interface AuthNavigator {
    fun openSignInScreen(activityContext: Context)

    fun openSignUpScreen(activityContext: Context)

    fun openIntroScreen(activityContext: Context)

    fun openChangePasswordScreen(activityContext: Context)

    fun openRecoverPasswordScreen(activityContext: Context, email: String)

    fun openForgotPasswordScreen(activityContext: Context)
}

interface MainNavigator {
    fun openMainScreen(activityContext: Context)
}

interface SignerNavigator {
    fun openSignerIntroScreen(activityContext: Context)

    fun openSignerInfoScreen(activityContext: Context, signerName: String, signerSpec: String, justAdded: Boolean = false)

    fun openAddSignerScreen(activityContext: Context)
}

interface WalletNavigator {
    fun openWalletIntroScreen(activityContext: Context)

    fun openAddWalletScreen(activityContext: Context)
}

interface NunchukNavigator : AuthNavigator, MainNavigator, SignerNavigator, WalletNavigator
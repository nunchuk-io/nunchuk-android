package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

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
    fun openAddWalletScreen(activityContext: Context)

    fun openAssignSignerScreen(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType)
}

interface NunchukNavigator : AuthNavigator, MainNavigator, SignerNavigator, WalletNavigator
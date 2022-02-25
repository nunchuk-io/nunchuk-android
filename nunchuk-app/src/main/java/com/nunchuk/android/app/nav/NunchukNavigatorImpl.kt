package com.nunchuk.android.app.nav

import android.app.Activity
import android.content.Context
import com.nunchuk.android.app.intro.GuestModeIntroActivity
import com.nunchuk.android.app.intro.GuestModeMessageIntroActivity
import com.nunchuk.android.app.intro.IntroActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.auth.nav.AuthNavigatorDelegate
import com.nunchuk.android.contact.nav.ContactNavigatorDelegate
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.messages.nav.MessageNavigatorDelegate
import com.nunchuk.android.nav.AppNavigator
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.nav.SettingNavigatorDelegate
import com.nunchuk.android.signer.nav.SignerNavigatorDelegate
import com.nunchuk.android.transaction.nav.TransactionNavigatorDelegate
import com.nunchuk.android.wallet.nav.WalletNavigatorDelegate
import javax.inject.Inject

internal class NunchukNavigatorImpl @Inject constructor(
) : NunchukNavigator,
    AppNavigatorDelegate,
    AuthNavigatorDelegate,
    SignerNavigatorDelegate,
    WalletNavigatorDelegate,
    TransactionNavigatorDelegate,
    ContactNavigatorDelegate,
    MessageNavigatorDelegate,
    SettingNavigatorDelegate {

    override fun openIntroScreen(activityContext: Context) {
        IntroActivity.start(activityContext)
    }

    override fun openMainScreen(
        activityContext: Context,
        loginHalfToken: String?,
        deviceId: String?,
        bottomNavViewPosition: Int?
    ) {
        MainActivity.start(activityContext, loginHalfToken, deviceId, bottomNavViewPosition)
    }

    override fun openGuestModeIntroScreen(activityContext: Context) {
        GuestModeIntroActivity.start(activityContext)
    }

    override fun openGuestModeMessageIntroScreen(activityContext: Context) {
        GuestModeMessageIntroActivity.start(activityContext)
    }
}

interface AppNavigatorDelegate : AppNavigator {

    override fun restartApp(activity: Activity) {
        SplashActivity.start(activity)
    }
}
package com.nunchuk.android.app.nav

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.jakewharton.processphoenix.ProcessPhoenix
import com.nunchuk.android.QuickWalletNavigationDirections
import com.nunchuk.android.app.intro.GuestModeIntroActivity
import com.nunchuk.android.app.intro.GuestModeMessageIntroActivity
import com.nunchuk.android.app.intro.IntroActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.wallet.QuickWalletActivity
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
        bottomNavViewPosition: Int?,
        isNewDevice: Boolean
    ) {
        MainActivity.start(activityContext, loginHalfToken, deviceId, bottomNavViewPosition, isNewDevice)
    }

    override fun openGuestModeIntroScreen(activityContext: Context) {
        GuestModeIntroActivity.start(activityContext)
    }

    override fun openGuestModeMessageIntroScreen(activityContext: Context) {
        GuestModeMessageIntroActivity.start(activityContext)
    }

    override fun openQuickWalletScreen(activityContext: Context) {
        QuickWalletActivity.start(activityContext)
    }

    override fun openCreateNewSeedScreen(fragment: Fragment) {
        fragment.findNavController().navigate(QuickWalletNavigationDirections.actionWalletIntermediaryFragmentToCreateNewSeedFragment())
    }
}

interface AppNavigatorDelegate : AppNavigator {

    override fun restartApp(activityContext: Context) {
        ProcessPhoenix.triggerRebirth(activityContext, Intent(activityContext, SplashActivity::class.java))
    }
}
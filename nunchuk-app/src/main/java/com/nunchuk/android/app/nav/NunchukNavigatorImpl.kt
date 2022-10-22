package com.nunchuk.android.app.nav

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.jakewharton.processphoenix.ProcessPhoenix
import com.nunchuk.android.QuickWalletNavigationDirections
import com.nunchuk.android.app.intro.GuestModeIntroActivity
import com.nunchuk.android.app.intro.GuestModeMessageIntroActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.wallet.QuickWalletActivity
import com.nunchuk.android.auth.nav.AuthNavigatorDelegate
import com.nunchuk.android.contact.nav.ContactNavigatorDelegate
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.messages.nav.MessageNavigatorDelegate
import com.nunchuk.android.nav.AppNavigator
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.nav.SettingNavigatorDelegate
import com.nunchuk.android.signer.nav.NfcNavigatorDelegate
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
    SettingNavigatorDelegate,
    NfcNavigatorDelegate {

    override fun openMainScreen(
        activityContext: Context,
        loginHalfToken: String?,
        deviceId: String?,
        bottomNavViewPosition: Int?,
        messages: ArrayList<String>?,
        isNewDevice: Boolean,
        isClearTask: Boolean
    ) {
        MainActivity.start(
            activityContext, loginHalfToken, deviceId, bottomNavViewPosition, isNewDevice,
            messages = messages, isClearTask = isClearTask
        )
    }

    override fun openGuestModeIntroScreen(activityContext: Context) {
        GuestModeIntroActivity.start(activityContext)
    }

    override fun openGuestModeMessageIntroScreen(activityContext: Context) {
        GuestModeMessageIntroActivity.start(activityContext)
    }

    override fun openQuickWalletScreen(launcher: ActivityResultLauncher<Intent>, activityContext: Context) {
        QuickWalletActivity.start(launcher, activityContext)
    }

    override fun openCreateNewSeedScreen(fragment: Fragment, isQuickWallet: Boolean) {
        fragment.findNavController().navigate(QuickWalletNavigationDirections.showCreateNewSeedFragment(isQuickWallet))
    }
}

interface AppNavigatorDelegate : AppNavigator {

    override fun restartApp(activityContext: Context) {
        ProcessPhoenix.triggerRebirth(activityContext, Intent(activityContext, SplashActivity::class.java))
    }
}
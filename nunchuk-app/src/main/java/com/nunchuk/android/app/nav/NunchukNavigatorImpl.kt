package com.nunchuk.android.app.nav

import android.content.Context
import com.nunchuk.android.app.intro.IntroActivity
import com.nunchuk.android.auth.nav.AuthNavigatorDelegate
import com.nunchuk.android.contact.nav.ContactNavigatorDelegate
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.messages.nav.MessageNavigatorDelegate
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.nav.SettingNavigatorDelegate
import com.nunchuk.android.signer.nav.SignerNavigatorDelegate
import com.nunchuk.android.transaction.nav.TransactionNavigatorDelegate
import com.nunchuk.android.wallet.nav.WalletNavigatorDelegate
import javax.inject.Inject

internal class NunchukNavigatorImpl @Inject constructor(
) : NunchukNavigator,
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
        deviceId: String?
    ) {
        MainActivity.start(activityContext, loginHalfToken, deviceId)
    }
}
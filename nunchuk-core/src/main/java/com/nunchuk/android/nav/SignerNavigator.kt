package com.nunchuk.android.nav

import android.content.Context
import androidx.fragment.app.Fragment
import com.nunchuk.android.type.SignerType

interface SignerNavigator {
    fun openSignerIntroScreen(activityContext: Context)

    fun openSignerInfoScreen(
        activityContext: Context,
        id: String,
        name: String,
        type: SignerType,
        justAdded: Boolean = false,
        setPassphrase: Boolean = false,
        isInWallet: Boolean = false
    )

    fun openAddAirSignerScreen(activityContext: Context)

    fun openAddAirSignerIntroScreen(activityContext: Context)

    fun openAddSoftwareSignerScreen(activityContext: Context)

    fun openCreateNewSeedScreen(activityContext: Context)

    fun openCreateNewSeedScreen(fragment: Fragment)

    fun openRecoverSeedScreen(activityContext: Context)

    fun openSelectPhraseScreen(activityContext: Context, mnemonic: String)

    fun openAddSoftwareSignerNameScreen(activityContext: Context, mnemonic: String)

    fun openSetPassphraseScreen(activityContext: Context, mnemonic: String, signerName: String)
}
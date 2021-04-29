package com.nunchuk.android.nav

import android.content.Context

interface SignerNavigator {
    fun openSignerIntroScreen(activityContext: Context)

    fun openSignerInfoScreen(
        activityContext: Context,
        signerName: String,
        signerSpec: String,
        justAdded: Boolean = false
    )

    fun openAddAirSignerScreen(activityContext: Context)

    fun openAddAirSignerIntroScreen(activityContext: Context)

    fun openAddSoftwareSignerScreen(activityContext: Context)

    fun openCreateNewSeedScreen(activityContext: Context)

    fun openRecoverSeedScreen(activityContext: Context)

    fun openSelectPhraseScreen(activityContext: Context, mnemonic: String)

    fun openAddSoftwareSignerNameScreen(activityContext: Context, mnemonic: String)

    fun openSetPassphraseScreen(activityContext: Context, mnemonic: String, signerName: String)
}
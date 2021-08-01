package com.nunchuk.android.signer.nav

import android.content.Context
import com.nunchuk.android.nav.SignerNavigator
import com.nunchuk.android.signer.AirSignerIntroActivity
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.SoftwareSignerIntroActivity
import com.nunchuk.android.signer.components.add.AddSignerActivity
import com.nunchuk.android.signer.components.details.SignerInfoActivity
import com.nunchuk.android.signer.components.ss.confirm.ConfirmSeedActivity
import com.nunchuk.android.signer.components.ss.create.CreateNewSeedActivity
import com.nunchuk.android.signer.components.ss.name.AddSoftwareSignerNameActivity
import com.nunchuk.android.signer.components.ss.passphrase.SetPassphraseActivity
import com.nunchuk.android.signer.components.ss.recover.RecoverSeedActivity

interface SignerNavigatorDelegate : SignerNavigator {

    override fun openSignerIntroScreen(activityContext: Context) {
        SignerIntroActivity.start(activityContext)
    }

    override fun openSignerInfoScreen(
        activityContext: Context,
        id: String,
        name: String,
        justAdded: Boolean,
        software: Boolean,
        setPassphrase: Boolean
    ) {
        SignerInfoActivity.start(
            activityContext = activityContext,
            id = id,
            name = name,
            justAdded = justAdded,
            software = software,
            setPassphrase = setPassphrase
        )
    }

    override fun openAddAirSignerIntroScreen(activityContext: Context) {
        AirSignerIntroActivity.start(activityContext)
    }

    override fun openAddAirSignerScreen(activityContext: Context) {
        AddSignerActivity.start(activityContext)
    }

    override fun openAddSoftwareSignerScreen(activityContext: Context) {
        SoftwareSignerIntroActivity.start(activityContext)
    }

    override fun openCreateNewSeedScreen(activityContext: Context) {
        CreateNewSeedActivity.start(activityContext)
    }

    override fun openRecoverSeedScreen(activityContext: Context) {
        RecoverSeedActivity.start(activityContext)
    }

    override fun openSelectPhraseScreen(activityContext: Context, mnemonic: String) {
        ConfirmSeedActivity.start(activityContext, mnemonic)
    }

    override fun openAddSoftwareSignerNameScreen(activityContext: Context, mnemonic: String) {
        AddSoftwareSignerNameActivity.start(activityContext, mnemonic)
    }

    override fun openSetPassphraseScreen(activityContext: Context, mnemonic: String, signerName: String) {
        SetPassphraseActivity.start(activityContext, mnemonic, signerName)
    }

}
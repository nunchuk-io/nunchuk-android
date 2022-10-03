package com.nunchuk.android.signer.nav

import android.content.Context
import com.nunchuk.android.nav.SignerNavigator
import com.nunchuk.android.signer.AirSignerIntroActivity
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.components.add.AddSignerActivity
import com.nunchuk.android.signer.components.details.SignerInfoActivity
import com.nunchuk.android.signer.software.SoftwareSignerIntroActivity
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedActivity
import com.nunchuk.android.signer.software.components.create.CreateNewSeedActivity
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameActivity
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseActivity
import com.nunchuk.android.signer.software.components.recover.RecoverSeedActivity
import com.nunchuk.android.type.SignerType

interface SignerNavigatorDelegate : SignerNavigator {

    override fun openSignerIntroScreen(activityContext: Context) {
        SignerIntroActivity.start(activityContext)
    }

    override fun openSignerInfoScreen(
        activityContext: Context,
        id: String,
        masterFingerprint: String,
        name: String,
        type: SignerType,
        derivationPath: String,
        justAdded: Boolean,
        setPassphrase: Boolean,
        isInWallet: Boolean
    ) {
        SignerInfoActivity.start(
            activityContext = activityContext,
            id = id,
            name = name,
            justAdded = justAdded,
            type = type,
            setPassphrase = setPassphrase,
            isInWallet = isInWallet,
            derivationPath = derivationPath,
            masterFingerprint = masterFingerprint
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
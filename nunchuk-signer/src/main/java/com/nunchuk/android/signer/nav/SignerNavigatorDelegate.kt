package com.nunchuk.android.signer.nav

import android.content.Context
import com.nunchuk.android.nav.SignerNavigator
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.SoftwareSignerIntroActivity
import com.nunchuk.android.signer.add.AddSignerActivity
import com.nunchuk.android.signer.details.SignerInfoActivity
import com.nunchuk.android.signer.ss.create.CreateNewSeedActivity
import com.nunchuk.android.signer.ss.recover.RecoverSeedActivity

interface SignerNavigatorDelegate : SignerNavigator {

    override fun openSignerIntroScreen(activityContext: Context) {
        SignerIntroActivity.start(activityContext)
    }

    override fun openSignerInfoScreen(activityContext: Context, signerName: String, signerSpec: String, justAdded: Boolean) {
        SignerInfoActivity.start(
            activityContext = activityContext,
            signerName = signerName,
            signerSpec = signerSpec,
            justAdded = justAdded
        )
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

}
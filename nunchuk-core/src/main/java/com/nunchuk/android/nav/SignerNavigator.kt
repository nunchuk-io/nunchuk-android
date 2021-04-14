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

    fun openAddSignerScreen(activityContext: Context)
}
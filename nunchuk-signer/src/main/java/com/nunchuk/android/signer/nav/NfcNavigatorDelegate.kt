package com.nunchuk.android.signer.nav

import android.app.Activity
import android.content.Intent
import com.nunchuk.android.nav.NfcNavigator
import com.nunchuk.android.signer.mk4.Mk4Activity

interface NfcNavigatorDelegate : NfcNavigator {
    override fun openSetupMk4(activity: Activity) {
        activity.startActivity(Intent(activity, Mk4Activity::class.java))
    }
}
package com.nunchuk.android.share.result

import android.app.Activity
import androidx.activity.result.ActivityResult

/**
 * "Exit wizard" was confirmed on a screen the wizard opened (the key-type picker, or a flow that
 * relays its result). Closing that screen alone would drop the owner back inside the wizard, so
 * whoever hosts the wizard has to close too.
 */
fun ActivityResult.isExitWizard(): Boolean =
    resultCode == Activity.RESULT_OK &&
        data?.getBooleanExtra(GlobalResultKey.EXTRA_EXIT_WIZARD, false) == true

package com.nunchuk.android.signer.util

import android.app.Activity
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.NCWarningDialog

internal fun Activity.showBadRatioNotRecommend(
    onDenied: () -> Unit = {},
    onConfirmed: () -> Unit = {}
) {
    NCWarningDialog(this).showDialog(
        message = getString(R.string.nc_signer_bad_ratio_not_recommend),
        onYesClick = onConfirmed,
        onNoClick = onDenied
    )
}

internal fun Activity.showBadRatioRecommend(
    onDenied: () -> Unit = {},
    onConfirmed: () -> Unit = {}
) {
    NCWarningDialog(this).showDialog(
        message = getString(R.string.nc_signer_bad_ratio_recommend),
        onYesClick = onConfirmed,
        onNoClick = onDenied
    )
}
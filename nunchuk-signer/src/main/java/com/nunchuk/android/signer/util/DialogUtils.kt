package com.nunchuk.android.signer.util

import android.app.Activity
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog

internal fun Activity.showNfcAlreadyAdded() {
    NCInfoDialog(this).showDialog(
        message = getString(R.string.nc_nfc_key_already_added),
    ).show()
}

internal fun Activity.showSetupNfc(callback : () -> Unit) {
    NCWarningDialog(this).showDialog(
        title = getString(R.string.nc_set_up_nfc_key),
        message = getString(R.string.nc_setup_nfc_key_desc),
        onYesClick = callback
    ).show()
}

internal fun Activity.showAddNfcKey(callback : () -> Unit) {
    NCWarningDialog(this).showDialog(
        title = getString(R.string.nc_add_nfc_key),
        message = getString(R.string.nc_add_nfc_key_desc),
        onYesClick = callback
    ).show()
}
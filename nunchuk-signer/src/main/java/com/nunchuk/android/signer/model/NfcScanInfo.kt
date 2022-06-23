package com.nunchuk.android.signer.model

import android.nfc.Tag

data class NfcScanInfo(val requestCode: Int, val tag: Tag)
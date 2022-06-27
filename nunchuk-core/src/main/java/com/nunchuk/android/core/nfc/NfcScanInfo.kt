package com.nunchuk.android.core.nfc

import android.nfc.Tag

data class NfcScanInfo(val requestCode: Int, val tag: Tag)
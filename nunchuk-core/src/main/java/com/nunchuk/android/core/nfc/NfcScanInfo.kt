package com.nunchuk.android.core.nfc

import android.nfc.NdefRecord
import android.nfc.Tag

data class NfcScanInfo(val requestCode: Int, val tag: Tag, val records: List<NdefRecord>)
/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.qr

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanOptions

// Customize later
open class ScannerActivity : CaptureActivity()

fun Activity.startQRCodeScan() {
    val scanIntegrator = IntentIntegrator(this)
    scanIntegrator.setPrompt("Nunchuk")
    scanIntegrator.setBeepEnabled(true)
    scanIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
    scanIntegrator.captureActivity = ScannerActivity::class.java
    scanIntegrator.setOrientationLocked(true)
    scanIntegrator.setBarcodeImageEnabled(true)
    scanIntegrator.initiateScan()
}

fun startQRCodeScan(launcher: ActivityResultLauncher<ScanOptions>) {
    val scanOptions = ScanOptions()
    scanOptions.setPrompt("Nunchuk")
    scanOptions.setBeepEnabled(true)
    scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    scanOptions.captureActivity = ScannerActivity::class.java
    scanOptions.setOrientationLocked(true)
    scanOptions.setBarcodeImageEnabled(true)
    launcher.launch(scanOptions)
}


object QRCodeParser {

    fun parse(requestCode: Int, resultCode: Int, data: Intent?): String? {
        return IntentIntegrator.parseActivityResult(requestCode, resultCode, data).contents
    }

}
/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanOptions

// Customize later
open class ScannerActivity : CaptureActivity()

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
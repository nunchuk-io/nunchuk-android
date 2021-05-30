package com.nunchuk.android.qr

import android.app.Activity
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

// Customize later
class ScannerActivity : CaptureActivity()

fun Activity.startQRCodeScan() {
    val scanIntegrator = IntentIntegrator(this)
    scanIntegrator.setPrompt("Nunchuk")
    scanIntegrator.setBeepEnabled(true)
    scanIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
    scanIntegrator.captureActivity = ScannerActivity::class.java
    scanIntegrator.setOrientationLocked(true)
    scanIntegrator.setBarcodeImageEnabled(true)
    scanIntegrator.initiateScan()
}


object QRCodeParser {

    fun parse(requestCode: Int, resultCode: Int, data: Intent?): String? {
        return IntentIntegrator.parseActivityResult(requestCode, resultCode, data).contents
    }

}
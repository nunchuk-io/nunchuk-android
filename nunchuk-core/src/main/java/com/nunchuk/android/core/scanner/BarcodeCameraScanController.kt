package com.nunchuk.android.core.scanner

import android.content.Intent
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class BarcodeCameraScanController(
    private val barcodeView: DecoratedBarcodeView,
) : CameraScanController {

    private var listener: ((String) -> Unit)? = null

    override fun setOnBarcodeResultListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    override fun startScanning(intent: Intent) {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        barcodeView.initializeFromIntent(barcodeViewIntent)
        barcodeView.decodeContinuous { result ->
            listener?.invoke(result.text)
        }
        barcodeView.resume()
    }

    override fun stopScanning() {
        barcodeView.pause()
    }

    override fun resumeScanning() {
        barcodeView.resume()
    }

    override fun onDestroy() {
//        barcodeView.stopCamera()
    }

    override fun torchState(isOn: Boolean) {
        if (isOn) {
            barcodeView.setTorchOn()
        } else {
            barcodeView.setTorchOff()
        }
    }
}
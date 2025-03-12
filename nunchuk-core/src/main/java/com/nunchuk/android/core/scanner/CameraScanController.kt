package com.nunchuk.android.core.scanner

import android.content.Intent

interface CameraScanController {
    fun startScanning(intent: Intent)
    fun stopScanning()
    fun resumeScanning()
    fun onDestroy()
    fun setOnBarcodeResultListener(listener: (String) -> Unit)
    fun torchState(isOn: Boolean)
}
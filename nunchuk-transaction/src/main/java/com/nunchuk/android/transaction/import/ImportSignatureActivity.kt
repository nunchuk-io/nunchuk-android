package com.nunchuk.android.transaction.import

import android.os.Bundle
import com.nunchuk.android.qr.ScannerActivity
import com.nunchuk.android.transaction.databinding.ActivityImportSignatureBinding
import com.nunchuk.android.widget.util.setLightStatusBar

class ImportSignatureActivity : ScannerActivity() {

    private lateinit var binding: ActivityImportSignatureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityImportSignatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

}
package com.nunchuk.android.core.nfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.databinding.ActivityTurnOnNfcBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TurnOnNfcActivity : BaseActivity<ActivityTurnOnNfcBinding>() {
    private val requestEnableNfc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (NfcAdapter.getDefaultAdapter(this).isEnabled) {
                setResult(RESULT_OK)
                finish()
            }
        }

    override fun initializeBinding(): ActivityTurnOnNfcBinding = ActivityTurnOnNfcBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerEvents()
    }

    private fun registerEvents() {
        binding.btnGotIt.setOnClickListener {
            try {
                requestEnableNfc.launch(Intent(Settings.ACTION_NFC_SETTINGS))
            } catch (ignore: Exception) {
            }
        }
        binding.toolbar.setNavigationOnClickListener {
           finish()
        }
    }
}
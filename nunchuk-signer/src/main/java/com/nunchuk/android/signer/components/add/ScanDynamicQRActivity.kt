package com.nunchuk.android.signer.components.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.gson.Gson
import com.google.zxing.client.android.Intents
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivityScanDynamicQrBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ScanDynamicQRActivity : BaseActivity<ActivityScanDynamicQrBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var gson: Gson

    private val viewModel: AddSignerViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityScanDynamicQrBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous { result ->
            viewModel.handAddPassportSigners(result.text) {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(PASSPORT_EXTRA_KEYS, gson.toJson(it))
                })
                finish()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.resume()
    }

    companion object {
        fun start(activityContext: Activity, requestCode: Int) {
            activityContext.startActivityForResult(Intent(activityContext, ScanDynamicQRActivity::class.java), requestCode)
        }
    }

}


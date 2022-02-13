package com.nunchuk.android.signer.components.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.toSpec
import com.nunchuk.android.signer.databinding.ActivityAddPassportSignersBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AddPassportSignersActivity : BaseActivity<ActivityAddPassportSignersBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: AddSignerViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityAddPassportSignersBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous(object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult) {
                viewModel.handAddPassportSigners(result.text, {
                    binding.frameCounter.text = "Frames::$it"
                }, {
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(PASSPORT_EXTRA_KEY_SPEC, it.toSpec())
                        putExtra(PASSPORT_EXTRA_KEY_NAME, it.name)
                    })
                    finish()
                })
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

            }
        })

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
            activityContext.startActivityForResult(Intent(activityContext, AddPassportSignersActivity::class.java), requestCode)
        }
    }

}


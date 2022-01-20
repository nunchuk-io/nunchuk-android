package com.nunchuk.android.wallet.personal.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.wallet.personal.databinding.ActivityImportWalletQrcodeBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class RecoverWalletQrCodeActivity : BaseActivity<ActivityImportWalletQrcodeBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: RecoverWalletQrCodeViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityImportWalletQrcodeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous(object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult) {
                viewModel.updateQRCode(result.text, "")
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

            }
        })

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: RecoverWalletQrCodeEvent) {
        when (event) {
            is RecoverWalletQrCodeEvent.ImportQRCodeError -> onImportQRCodeError(event)
            is RecoverWalletQrCodeEvent.ImportQRCodeSuccess -> onImportQRCodeSuccess(event)
        }
    }

    private fun onImportQRCodeSuccess(event: RecoverWalletQrCodeEvent.ImportQRCodeSuccess) {
        hideLoading()
        navigator.openAddRecoverWalletScreen(
            this, RecoverWalletData(
                type = RecoverWalletType.QR_CODE,
                walletId = event.walletId
            )
        )
        finish()
    }

    private fun onImportQRCodeError(event: RecoverWalletQrCodeEvent.ImportQRCodeError) {
        hideLoading()
        NCToastMessage(this).showWarning(event.message)
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
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, RecoverWalletQrCodeActivity::class.java))
        }
    }

}


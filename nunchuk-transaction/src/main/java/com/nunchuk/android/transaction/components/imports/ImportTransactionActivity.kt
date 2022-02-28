package com.nunchuk.android.transaction.components.imports

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
import com.nunchuk.android.core.util.CHOOSE_FILE_REQUEST_CODE
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.TransactionOption
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.transaction.databinding.ActivityImportTransactionBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ImportTransactionActivity : BaseActivity<ActivityImportTransactionBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: ImportTransactionArgs by lazy { ImportTransactionArgs.deserializeFrom(intent) }

    private val viewModel: ImportTransactionViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityImportTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        viewModel.init(walletId = args.walletId, transactionOption = args.transactionOption)
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
                viewModel.updateQRCode(result.text)
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

            }
        })

        binding.btnImportViaFile.setOnClickListener {
            openSelectFileChooser()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: ImportTransactionEvent) {
        when (event) {
            is ImportTransactionError -> onImportTransactionError(event)
            ImportTransactionSuccess -> onImportTransactionSuccess()
        }
    }

    private fun onImportTransactionSuccess() {
        hideLoading()
        NCToastMessage(this).showMessage(getString(R.string.nc_transaction_imported))
        finish()
    }

    private fun onImportTransactionError(event: ImportTransactionError) {
        hideLoading()
        NCToastMessage(this).showWarning(getString(R.string.nc_transaction_imported_failed) + event.message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            intent?.data?.let {
                getFileFromUri(contentResolver, it, cacheDir)
            }?.absolutePath?.let(viewModel::importTransaction)
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

        fun start(activityContext: Activity, walletId: String, transactionOption: TransactionOption) {
            activityContext.startActivity(
                ImportTransactionArgs(
                    walletId = walletId,
                    transactionOption = transactionOption
                ).buildIntent(activityContext)
            )
        }

    }

}

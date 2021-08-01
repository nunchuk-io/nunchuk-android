package com.nunchuk.android.transaction.components.imports

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
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
        viewModel.init(args.walletId)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun setupViews() {
        binding.btnImportViaFile.setOnClickListener {
            openSelectFileChooser()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: ImportTransactionEvent) {
        when (event) {
            is ImportTransactionError -> onImportTransactionSuccess(event)
            ImportTransactionSuccess -> onImportTransactionError()
        }
    }

    private fun onImportTransactionError() {
        hideLoading()
        NCToastMessage(this).showMessage("Transaction Imported")
    }

    private fun onImportTransactionSuccess(event: ImportTransactionError) {
        hideLoading()
        NCToastMessage(this).showError("Import failed :${event.message}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            intent?.data?.path?.let {
                showLoading()
                viewModel.importTransaction(it)
            }
        }
    }

    private fun openSelectFileChooser() {
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Please select your transaction file"), REQUEST_CODE)
    }

    companion object {
        private const val REQUEST_CODE = 1248

        fun start(activityContext: Activity, walletId: String) {
            activityContext.startActivity(
                ImportTransactionArgs(walletId = walletId).buildIntent(activityContext)
            )
        }

    }

}


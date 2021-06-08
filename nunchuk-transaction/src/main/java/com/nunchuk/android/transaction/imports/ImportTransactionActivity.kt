package com.nunchuk.android.transaction.imports

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.databinding.ActivityImportTransactionBinding
import com.nunchuk.android.transaction.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ImportTransactionActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: ImportTransactionArgs by lazy { ImportTransactionArgs.deserializeFrom(intent) }

    private val viewModel: ImportTransactionViewModel by lazy {
        ViewModelProviders.of(this, factory).get(ImportTransactionViewModel::class.java)
    }

    private lateinit var binding: ActivityImportTransactionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityImportTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            is ImportTransactionError -> NCToastMessage(this).showError("Import failed :${event.message}")
            ImportTransactionSuccess -> NCToastMessage(this).showMessage("Transaction Imported")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            intent?.data?.path?.let(viewModel::importTransaction)
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


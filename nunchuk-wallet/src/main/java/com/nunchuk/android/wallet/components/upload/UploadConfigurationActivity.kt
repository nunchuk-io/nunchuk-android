package com.nunchuk.android.wallet.components.upload

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.checkReadExternalPermission
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.*
import com.nunchuk.android.wallet.databinding.ActivityWalletUploadConfigurationBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class UploadConfigurationActivity : BaseActivity<ActivityWalletUploadConfigurationBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private var isShared: Boolean = false

    private val args: UploadConfigurationArgs by lazy { UploadConfigurationArgs.deserializeFrom(intent) }

    private val viewModel: UploadConfigurationViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityWalletUploadConfigurationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.walletId)
    }

    override fun onResume() {
        super.onResume()
        if (isShared) {
            navigator.openWalletConfigScreen(this, args.walletId)
            isShared = false
        }
    }

    private fun setupViews() {
        binding.btnQRCode.setOnClickListener { viewModel.handleShowQREvent() }
        binding.btnUpload.setOnClickListener { handleUploadWallet() }
        binding.btnSkipUpload.setOnClickListener { navigator.openWalletConfigScreen(this, args.walletId) }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleUploadWallet() {
        if (checkReadExternalPermission()) {
            viewModel.handleUploadEvent()
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: UploadConfigurationEvent) {
        when (event) {
            is ExportColdcardSuccess -> shareConfigurationFile(event.filePath)
            is ExportColdcardFailure -> showError(event)
            is OpenDynamicQRScreen -> openDynamicQRScreen(event)
        }
    }

    private fun showError(event: ExportColdcardFailure) {
        NCToastMessage(this).showWarning(event.message)
    }

    private fun openDynamicQRScreen(event: OpenDynamicQRScreen) {
        navigator.openDynamicQRScreen(this, event.values)
    }

    private fun shareConfigurationFile(filePath: String) {
        isShared = true
        controller.shareFile(filePath)
    }

    companion object {

        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(UploadConfigurationArgs(walletId).buildIntent(activityContext))
        }
    }

}
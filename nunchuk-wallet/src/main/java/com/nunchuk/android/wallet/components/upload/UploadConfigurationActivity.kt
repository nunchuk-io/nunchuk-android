package com.nunchuk.android.wallet.components.upload

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.ExportColdcardSuccess
import com.nunchuk.android.wallet.databinding.ActivityWalletUploadConfigurationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UploadConfigurationActivity : BaseWalletConfigActivity<ActivityWalletUploadConfigurationBinding>() {

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private val args: UploadConfigurationArgs by lazy { UploadConfigurationArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityWalletUploadConfigurationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        sharedViewModel.init(args.walletId)
    }

    private fun setupViews() {
        binding.btnQRCode.setOnDebounceClickListener { sharedViewModel.handleShowQREvent() }
        binding.btnUpload.setOnDebounceClickListener { showExportColdcardOptions() }
        binding.btnSkipUpload.setOnDebounceClickListener {
            goToWalletConfigScreen()
        }
        binding.toolbar.setNavigationOnClickListener {
            ActivityManager.popUntilRoot()
        }
    }

    override fun handleSharedEvent(event: UploadConfigurationEvent) {
        super.handleSharedEvent(event)
        if (event is ExportColdcardSuccess) shareConfigurationFile(event.filePath)
    }

    private fun shareConfigurationFile(filePath: String?) {
        if (filePath.isNullOrEmpty().not()) {
            controller.shareFile(filePath.orEmpty())
        }
        goToWalletConfigScreen()
    }

    private fun goToWalletConfigScreen() {
        navigator.openWalletConfigScreen(this, args.walletId)
        ActivityManager.popUntilRoot()
    }

    companion object {
        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(UploadConfigurationArgs(walletId).buildIntent(activityContext))
        }
    }
}
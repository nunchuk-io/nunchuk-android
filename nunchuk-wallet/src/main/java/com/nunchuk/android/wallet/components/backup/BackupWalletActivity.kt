package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Failure
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Success
import com.nunchuk.android.wallet.databinding.ActivityWalletBackupWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackupWalletActivity : BaseActivity<ActivityWalletBackupWalletBinding>() {

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private var isShared: Boolean = false

    private val args: BackupWalletArgs by lazy { BackupWalletArgs.deserializeFrom(intent) }

    private val viewModel: BackupWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityWalletBackupWalletBinding.inflate(layoutInflater)

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
            navigateToNextScreen()
            isShared = false
        }
    }

    private fun setupViews() {
        NCToastMessage(this).show(R.string.nc_wallet_has_been_created)
        binding.btnBackup.setOnClickListener { viewModel.handleBackupDescriptorEvent() }
        binding.btnSkipBackup.setOnClickListener {
            navigateToNextScreen()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun navigateToNextScreen() {
        if (args.isQuickWallet) {
            finish()
        } else if (args.numberOfSignKey > 1) {
            navigator.openUploadConfigurationScreen(this, args.walletId)
        } else {
            navigator.openMainScreen(this)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: BackupWalletEvent) {
        when (event) {
            is Success -> shareFile(event)
            is Failure -> NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun shareFile(event: Success) {
        isShared = true
        controller.shareFile(event.filePath)
    }

    companion object {

        fun start(activityContext: Context, walletId: String, totalRequireSigns: Int, isQuickWallet: Boolean) {
            activityContext.startActivity(BackupWalletArgs(walletId, totalRequireSigns, isQuickWallet).buildIntent(activityContext))
        }
    }

}
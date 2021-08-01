package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.share.IntentSharingEventBus
import com.nunchuk.android.core.share.IntentSharingListener
import com.nunchuk.android.core.share.IntentSharingListenerWrapper
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.*
import com.nunchuk.android.wallet.databinding.ActivityWalletBackupWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class BackupWalletActivity : BaseActivity<ActivityWalletBackupWalletBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private val listener: IntentSharingListener = IntentSharingListenerWrapper {
        navigator.openUploadConfigurationScreen(this, args.walletId)
    }

    private val args: BackupWalletArgs by lazy { BackupWalletArgs.deserializeFrom(intent) }

    private val viewModel: BackupWalletViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityWalletBackupWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.walletId, args.descriptor)
    }

    override fun onResume() {
        super.onResume()
        IntentSharingEventBus.instance.subscribe(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        IntentSharingEventBus.instance.unsubscribe()
    }

    private fun setupViews() {
        NCToastMessage(this).show(R.string.nc_wallet_has_been_created)
        binding.btnBackup.setOnClickListener { viewModel.handleBackupDescriptorEvent() }
        binding.btnSkipBackup.setOnClickListener { viewModel.handleSkipBackupEvent() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: BackupWalletEvent) {
        when (event) {
            is SetLoadingEvent -> binding.progress.isVisible = event.showLoading
            is BackupDescriptorEvent -> shareDescriptor(event.descriptor)
            is SkipBackupWalletEvent -> navigator.openUploadConfigurationScreen(this, event.walletId)
        }
    }

    private fun shareDescriptor(descriptor: String) {
        controller.shareText(descriptor)
    }

    companion object {

        fun start(activityContext: Context, walletId: String, descriptor: String) {
            activityContext.startActivity(BackupWalletArgs(walletId, descriptor).buildIntent(activityContext))
        }
    }

}
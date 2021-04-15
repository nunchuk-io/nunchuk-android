package com.nunchuk.android.wallet.backup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.backup.BackupWalletEvent.*
import com.nunchuk.android.wallet.databinding.ActivityWalletBackupWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

class BackupWalletActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: BackupWalletArgs by lazy { BackupWalletArgs.deserializeFrom(intent) }

    private val viewModel: BackupWalletViewModel by lazy {
        ViewModelProviders.of(this, factory).get(BackupWalletViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletBackupWalletBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletBackupWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()

        viewModel.init(args.descriptor)
    }

    private fun setupViews() {
        NCToastMessage(this).show(R.string.nc_wallet_has_been_created)
        binding.btnBackup.setOnClickListener { viewModel.handleBackupDescriptorEvent() }
        binding.btnSkipBackup.setOnClickListener { viewModel.handleSkipBackupEvent() }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: BackupWalletEvent) {
        when (event) {
            is SetLoadingEvent -> {
            }
            is BackupDescriptorEvent -> shareDescriptor(event.descriptor)
            SkipBackupWalletEvent -> navigator.openUploadConfigurationScreen(this)
        }
    }

    private fun shareDescriptor(descriptor: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, descriptor)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(sendIntent, null))
    }

    companion object {

        fun start(activityContext: Context, descriptor: String) {
            activityContext.startActivity(BackupWalletArgs(descriptor).buildIntent(activityContext))
        }
    }

}
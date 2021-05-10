package com.nunchuk.android.wallet.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.share.IntentSharingEventBus
import com.nunchuk.android.core.share.IntentSharingListener
import com.nunchuk.android.core.share.IntentSharingListenerWrapper
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.databinding.ActivityWalletUploadConfigurationBinding
import com.nunchuk.android.wallet.upload.UploadConfigurationEvent.*
import com.nunchuk.android.widget.NCToastMessage
import java.io.File
import javax.inject.Inject

class UploadConfigurationActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    @Inject
    lateinit var controller: IntentSharingController

    private val listener: IntentSharingListener = IntentSharingListenerWrapper {
        navigator.openWalletReviewScreen(this, args.walletId)
    }

    private val args: UploadConfigurationArgs by lazy { UploadConfigurationArgs.deserializeFrom(intent) }

    private val viewModel: UploadConfigurationViewModel by lazy {
        ViewModelProviders.of(this, factory).get(UploadConfigurationViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletUploadConfigurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletUploadConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()

        viewModel.init(args.walletId)
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
        binding.btnQRCode.setOnClickListener { viewModel.handleShowQREvent() }
        binding.btnUpload.setOnClickListener { viewModel.handleUploadEvent() }
        binding.btnSkipUpload.setOnClickListener { navigator.openWalletReviewScreen(this, args.walletId) }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: UploadConfigurationEvent) {
        when (event) {
            is SetLoadingEvent -> binding.progress.isVisible = event.showLoading
            is ExportWalletSuccessEvent -> shareConfigurationFile(event.filePath)
            is UploadConfigurationError -> NCToastMessage(this).showWarning(event.message)
            is OpenDynamicQRScreen -> openDynamicQRScreen(event)
        }
    }

    private fun openDynamicQRScreen(event: OpenDynamicQRScreen) {
        navigator.openDynamicQRScreen(this, event.values)
    }

    private fun shareConfigurationFile(filePath: String) {
        controller.share(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
            type = "*/*"
        })
    }

    companion object {

        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(UploadConfigurationArgs(walletId).buildIntent(activityContext))
        }
    }

}
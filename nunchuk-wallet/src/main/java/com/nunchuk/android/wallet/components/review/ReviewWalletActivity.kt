package com.nunchuk.android.wallet.components.review

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.components.config.SignersViewBinder
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.*
import com.nunchuk.android.wallet.databinding.ActivityReviewWalletBinding
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ReviewWalletActivity : BaseActivity<ActivityReviewWalletBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: ReviewWalletArgs by lazy { ReviewWalletArgs.deserializeFrom(intent) }

    private val viewModel: ReviewWalletViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityReviewWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: ReviewWalletEvent) {
        when (event) {
            is SetLoadingEvent -> handleLoading(event.showLoading)
            is CreateWalletSuccessEvent -> onCreateWalletSuccess(event)
            is CreateWalletErrorEvent -> onCreateWalletError(event)
        }
    }

    private fun handleLoading(showLoading: Boolean) {
        if (showLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private fun onCreateWalletError(event: CreateWalletErrorEvent) {
        val message = event.message
        NCToastMessage(this).showWarning(message)
        if (message.isWalletExisted()) {
            navigator.openMainScreen(this)
        }
    }

    private fun onCreateWalletSuccess(event: CreateWalletSuccessEvent) {
        navigator.openBackupWalletScreen(this, event.walletId, event.descriptor)
    }

    private fun setupViews() {
        binding.walletName.text = args.walletName
        val signers = args.masterSigners.map(MasterSigner::toModel) + args.remoteSigners.map(SingleSigner::toModel)
        binding.configuration.bindWalletConfiguration(
            totalSigns = args.totalRequireSigns,
            assignedSigns = signers.size
        )

        binding.walletType.text = args.walletType.toReadableString(this)
        binding.addressType.text = args.addressType.toReadableString(this)
        SignersViewBinder(binding.signersContainer, signers).bindItems()

        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent(
                walletName = args.walletName,
                walletType = args.walletType,
                addressType = args.addressType,
                totalRequireSigns = args.totalRequireSigns,
                masterSigners = args.masterSigners,
                remoteSigners = args.remoteSigners
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            totalRequireSigns: Int,
            masterSigners: List<MasterSigner>,
            remoteSigners: List<SingleSigner>
        ) {
            activityContext.startActivity(
                ReviewWalletArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    totalRequireSigns = totalRequireSigns,
                    masterSigners = masterSigners,
                    remoteSigners = remoteSigners
                ).buildIntent(activityContext)
            )
        }
    }

}
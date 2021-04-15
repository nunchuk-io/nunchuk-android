package com.nunchuk.android.wallet.confirm

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.confirm.WalletConfirmEvent.*
import com.nunchuk.android.wallet.databinding.ActivityWalletConfirmationBinding
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

class WalletConfirmActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: WalletConfirmArgs by lazy { WalletConfirmArgs.deserializeFrom(intent) }

    private val viewModel: WalletConfirmViewModel by lazy {
        ViewModelProviders.of(this, factory).get(WalletConfirmViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: WalletConfirmEvent) {
        when (event) {
            is SetLoadingEvent -> binding.progress.isVisible = event.showLoading
            is CreateWalletSuccessEvent -> navigator.openBackupWalletScreen(this, event.walletId, event.descriptor)
            is CreateWalletErrorEvent -> NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun setupViews() {
        binding.walletName.text = args.walletName

        val configutation = "${args.totalRequireSigns}/${args.signers.size}"
        binding.multisigConfigutation.text = configutation

        binding.walletType.text = args.walletType.toReadableString(this)
        binding.addressType.text = args.addressType.toReadableString(this)
        SignersViewBinder(binding.signersContainer, args.signers).bindItems()

        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent(
                walletName = args.walletName,
                walletType = args.walletType,
                addressType = args.addressType,
                totalRequireSigns = args.totalRequireSigns,
                signers = args.signers
            )
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            totalRequireSigns: Int,
            signers: List<SingleSigner>
        ) {
            activityContext.startActivity(
                WalletConfirmArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    totalRequireSigns = totalRequireSigns,
                    signers = signers
                ).buildIntent(activityContext)
            )
        }
    }

}
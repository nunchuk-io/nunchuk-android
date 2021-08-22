package com.nunchuk.android.wallet.shared.components.configure

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.bindEnableState
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletEvent.ConfigureCompletedEvent
import com.nunchuk.android.wallet.shared.databinding.ActivityConfigureSharedWalletBinding
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ConfigureSharedWalletActivity : BaseActivity<ActivityConfigureSharedWalletBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: ConfigureSharedWalletArgs by lazy { ConfigureSharedWalletArgs.deserializeFrom(intent) }

    private val viewModel: ConfigureSharedWalletViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityConfigureSharedWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()

        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleEvent(event: ConfigureSharedWalletEvent) {
        when (event) {
            is ConfigureCompletedEvent -> openReviewSharedWalletScreen(
                totalSigns = event.totalSigns,
                requireSigns = event.requireSigns
            )
        }
    }

    private fun openReviewSharedWalletScreen(
        totalSigns: Int,
        requireSigns: Int
    ) {
        navigator.openReviewSharedWalletScreen(
            activityContext = this,
            walletName = args.walletName,
            walletType = args.walletType,
            addressType = args.addressType,
            totalSigns = totalSigns,
            requireSigns = requireSigns
        )
    }

    private fun handleState(state: ConfigureSharedWalletState) {
        val totalSigns = state.totalSigns
        val requireSigns = state.requireSigns
        bindTotalRequireSigns(totalSigns)
        binding.totalRequireSigns.bindWalletConfiguration(
            totalSigns = totalSigns,
            requireSigns = requireSigns
        )
        binding.totalSingerCounter.text = "$totalSigns"
        binding.requiredSingerCounter.text = "$requireSigns"
        binding.btnContinue.bindEnableState(state.isConfigured)
        binding.totalSignerIconMinus.isClickable = state.canDecreaseTotal
    }

    private fun bindTotalRequireSigns(totalRequireSigns: Int) {
        binding.requiredSingerCounter.text = "$totalRequireSigns"
    }

    private fun setupViews() {
        binding.totalSignerIconPlus.setOnClickListener { viewModel.handleIncreaseTotalSigners() }
        binding.iconPlus.setOnClickListener { viewModel.handleIncreaseRequiredSigners() }
        binding.totalSignerIconMinus.setOnClickListener { viewModel.handleDecreaseTotalSigners() }
        binding.iconMinus.setOnClickListener { viewModel.handleDecreaseRequiredSigners() }
        binding.btnContinue.setOnClickListener { viewModel.handleContinue() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(ConfigureSharedWalletArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }

}
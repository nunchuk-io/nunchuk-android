package com.nunchuk.android.wallet.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.*
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.add.AddWalletEvent.WalletNameRequiredEvent
import com.nunchuk.android.wallet.add.AddWalletEvent.WalletSetupDoneEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletAddBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AddWalletActivity : BaseActivity<ActivityWalletAddBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: AddWalletViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityWalletAddBinding.inflate(layoutInflater)

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

    private fun handleState(state: AddWalletState) {
        bindWalletType(state.walletType)
        bindAddressType(state.addressType)
        bindWalletCounter(state.walletName)
    }

    private fun bindWalletCounter(walletName: String) {
        val counter = "${walletName.length}/$MAX_LENGTH"
        binding.walletNameCounter.text = counter
    }

    private fun bindAddressType(addressType: AddressType) {
        when (addressType) {
            NESTED_SEGWIT -> enableNestedAddressType()
            NATIVE_SEGWIT -> enableNativeAddressType()
            LEGACY -> enableLegacyAddressType()
            else -> {
            }
        }
    }

    private fun enableLegacyAddressType() {
        binding.legacyRadio.isChecked = true
        binding.nativeSegwitRadio.isChecked = false
        binding.nestedSegwitRadio.isChecked = false
    }

    private fun enableNestedAddressType() {
        binding.legacyRadio.isChecked = false
        binding.nativeSegwitRadio.isChecked = false
        binding.nestedSegwitRadio.isChecked = true
    }

    private fun enableNativeAddressType() {
        binding.legacyRadio.isChecked = false
        binding.nativeSegwitRadio.isChecked = true
        binding.nestedSegwitRadio.isChecked = false
    }

    private fun handleEvent(event: AddWalletEvent) {
        when (event) {
            WalletNameRequiredEvent -> binding.walletName.setError(getString(R.string.nc_text_required))
            is WalletSetupDoneEvent -> openAssignSignerScreen(event.walletName, event.walletType, event.addressType)
        }
    }

    private fun openAssignSignerScreen(walletName: String, walletType: WalletType, addressType: AddressType) {
        navigator.openAssignSignerScreen(this, walletName, walletType, addressType)
    }

    private fun bindWalletType(walletType: WalletType) {
        if (walletType == WalletType.ESCROW) {
            binding.standardWalletRadio.isChecked = false
            binding.escrowWalletRadio.isChecked = true
        } else {
            binding.standardWalletRadio.isChecked = true
            binding.escrowWalletRadio.isChecked = false
        }
    }

    private fun setupViews() {
        binding.walletName.getEditTextView().filters = arrayOf(LengthFilter(MAX_LENGTH))

        binding.customizeAddressSwitch.setOnCheckedChangeListener { _, checked -> handleCustomizeCustomerChanged(checked) }

        binding.standardWalletRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setStandardWalletType() }
        binding.escrowWalletRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setEscrowWalletType() }

        binding.nestedSegwitRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setNestedAddressType() }
        binding.nativeSegwitRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setNativeAddressType() }
        binding.legacyRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setLegacyAddressType() }

        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleCustomizeCustomerChanged(checked: Boolean) {
        if (checked) {
            binding.customizeAddressContainer.isVisible = true
        } else {
            viewModel.setDefaultAddressType()
            binding.customizeAddressContainer.isVisible = false
        }
    }

    companion object {
        private const val MAX_LENGTH = 20

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AddWalletActivity::class.java))
        }
    }

}
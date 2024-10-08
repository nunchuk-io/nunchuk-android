/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.personal.components.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.LEGACY
import com.nunchuk.android.type.AddressType.NATIVE_SEGWIT
import com.nunchuk.android.type.AddressType.NESTED_SEGWIT
import com.nunchuk.android.type.AddressType.TAPROOT
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.add.AddWalletEvent.WalletNameRequiredEvent
import com.nunchuk.android.wallet.personal.components.add.AddWalletEvent.WalletSetupDoneEvent
import com.nunchuk.android.wallet.personal.databinding.ActivityWalletAddBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddWalletActivity : BaseActivity<ActivityWalletAddBinding>() {
    private val pin: String by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(DECOY_PIN).orEmpty()
    }
    private val viewModel: AddWalletViewModel by viewModels()

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
            TAPROOT -> enableTaprootAddressType()
            else -> {}
        }
    }

    private fun enableNestedAddressType() {
        binding.legacyRadio.isChecked = false
        binding.nativeSegwitRadio.isChecked = false
        binding.nestedSegwitRadio.isChecked = true
        binding.taprootRadio.isChecked = false
    }

    private fun enableNativeAddressType() {
        binding.legacyRadio.isChecked = false
        binding.nativeSegwitRadio.isChecked = true
        binding.nestedSegwitRadio.isChecked = false
        binding.taprootRadio.isChecked = false
    }

    private fun enableLegacyAddressType() {
        binding.legacyRadio.isChecked = true
        binding.nativeSegwitRadio.isChecked = false
        binding.nestedSegwitRadio.isChecked = false
        binding.taprootRadio.isChecked = false
    }

    private fun enableTaprootAddressType() {
        binding.legacyRadio.isChecked = false
        binding.nativeSegwitRadio.isChecked = false
        binding.nestedSegwitRadio.isChecked = false
        binding.taprootRadio.isChecked = true
    }

    private fun handleEvent(event: AddWalletEvent) {
        when (event) {
            WalletNameRequiredEvent -> binding.walletName.setError(getString(R.string.nc_text_required))
            is WalletSetupDoneEvent -> openAssignSignerScreen(
                event.walletName,
                event.walletType,
                event.addressType,
            )
        }
    }

    private fun openAssignSignerScreen(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType
    ) {
        if (addressType.isTaproot()) {
            navigator.openTaprootWarningScreen(this, walletName, walletType, addressType)
        } else {
            navigator.openConfigureWalletScreen(
                this,
                args = ConfigureWalletArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    decoyPin = pin
                )
            )
        }
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

        binding.customizeAddressSwitch.setOnCheckedChangeListener { _, checked ->
            handleCustomizeAddressChanged(
                checked
            )
        }
        binding.customizeWalletTypeSwitch.setOnCheckedChangeListener { _, checked ->
            handleCustomizeWalletChanged(
                checked
            )
        }

        binding.standardWalletRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setStandardWalletType() }
        binding.escrowWalletRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setEscrowWalletType() }

        binding.nestedSegwitRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setNestedAddressType() }
        binding.nativeSegwitRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setNativeAddressType() }
        binding.legacyRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setLegacyAddressType() }
        binding.taprootRadio.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setTaprootAddressType() }

        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleCustomizeAddressChanged(checked: Boolean) {
        binding.customizeAddressContainer.isVisible = checked
        if (!checked) {
            viewModel.setDefaultAddressType()
        }
    }

    private fun handleCustomizeWalletChanged(checked: Boolean) {
        binding.customizeWalletContainer.isVisible = checked
        if (!checked) {
            viewModel.setDefaultWalletType()
        }
    }

    companion object {
        private const val MAX_LENGTH = 20
        private const val DECOY_PIN = "decoy_wallet"

        fun start(activityContext: Context, decoyPin: String) {
            activityContext.startActivity(Intent(activityContext, AddWalletActivity::class.java).apply {
                putExtra(DECOY_PIN, decoyPin)
            })
        }
    }

}
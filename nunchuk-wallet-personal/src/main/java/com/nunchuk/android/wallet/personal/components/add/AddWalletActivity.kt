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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.WalletConfigType
import com.nunchuk.android.core.data.model.getWalletConfigTypeBy
import com.nunchuk.android.core.util.ADD_WALLET_RESULT
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.nav.args.AddWalletArgs
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.nav.args.MiniscriptArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddWalletActivity : BaseComposeActivity() {

    private val viewModel: AddWalletViewModel by viewModels()

    private val args: AddWalletArgs by lazy { AddWalletArgs.deserializeFrom(intent) }

    private var isAlreadyShowChangeAddressTypeDialog = false
    private var isAlreadyShowMiniscriptWarningDialog = false
    private var originalWalletConfigType: WalletConfigType? = null

    private val miniscriptLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val template = result.data?.getStringExtra("miniscript_template") ?: ""
            if (template.isNotEmpty()) {
                viewModel.setMiniscriptTemplate(template)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(args.groupWalletId)
        viewModel.initMiniscriptTemplate(args.miniscriptTemplate)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            AddWalletView(
                state = state,
                viewModel = viewModel,
                isCreateMiniscriptWallet = args.isCreateMiniscriptWallet,
                viewOnlyComposer = args.groupWalletComposer,
                isViewConfigOnly = args.groupWalletComposer != null || !state.groupSandbox?.replaceWalletId.isNullOrEmpty(),
                isEditGroupWallet = args.groupWalletId.isNotEmpty(),
                onSelectAddressType = {
                    if (args.groupWalletId.isNotEmpty()) {
                        val action = {
                            viewModel.selectAddressType(it)
                            viewModel.getFreeGroupWalletConfig(it)
                        }
                        if (viewModel.state.value.groupSandbox?.addressType != it && args.hasGroupSigner && isAlreadyShowChangeAddressTypeDialog.not()) {
                            showChangeAddressTypeDialog {
                                isAlreadyShowChangeAddressTypeDialog = true
                                action()
                            }
                        } else {
                            action()
                        }
                    } else {
                        viewModel.selectAddressType(it)
                    }
                },
                onContinue = { walletName, addressType, requiredKeys, totalKeys, walletConfigType ->
                    if (args.groupWalletComposer != null) {
                        setResult(
                            RESULT_OK,
                            Intent().apply { putExtra(ADD_WALLET_RESULT, walletName) }
                        )
                        finish()
                    } else if (args.groupWalletId.isNotEmpty()) {
                        if (walletConfigType == WalletConfigType.MINISCRIPT && state.miniscriptTemplate.isEmpty()) {
                            return@AddWalletView
                        }

                        // Check if switching to miniscript with group signers
                        val isSwitchingToMiniscript = walletConfigType == WalletConfigType.MINISCRIPT && 
                            originalWalletConfigType != WalletConfigType.MINISCRIPT &&
                            args.hasGroupSigner &&
                            !isAlreadyShowMiniscriptWarningDialog

                        val updateWalletConfig = {
                            viewModel.updateWalletConfig(
                                walletName,
                                addressType,
                                totalKeys,
                                requiredKeys,
                                miniscriptTemplate = if (walletConfigType == WalletConfigType.MINISCRIPT) state.miniscriptTemplate else null
                            )
                        }

                        if (isSwitchingToMiniscript) {
                            showMiniscriptConfigWarningDialog {
                                isAlreadyShowMiniscriptWarningDialog = true
                                updateWalletConfig()
                            }
                        } else {
                            updateWalletConfig()
                        }
                    } else if (args.isCreateMiniscriptWallet) {
                        navigator.openMiniscriptScreen(
                            this,
                            args = MiniscriptArgs(
                                walletName = walletName,
                                addressType = addressType
                            )
                        )
                    } else {
                        openAssignSignerScreen(
                            walletName = walletName,
                            addressType = addressType
                        )
                    }
                },
                onNavigateToMiniscript = { miniscriptArgs ->
                    // Use the launcher to get result back
                    val intent = Intent(this, Class.forName("com.nunchuk.android.app.miniscript.MiniscriptActivity"))
                    intent.putExtras(miniscriptArgs.buildBundle())
                    miniscriptLauncher.launch(intent)
                }
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is AddWalletEvent.UpdateGroupSandboxConfigSuccess -> {
                            finish()
                        }

                        is AddWalletEvent.Error -> {
                            showToast(event.message)
                        }

                        is AddWalletEvent.ShowError -> {
                            showToast(event.message)
                        }

                        is AddWalletEvent.OnCreateWalletSuccess -> {
                            finish()
                        }
                    }
                }
            }
        }

        // Track original wallet config type for detecting changes to miniscript
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (originalWalletConfigType == null && state.groupSandbox != null) {
                        // Initialize original wallet config type based on current configuration
                        originalWalletConfigType = if (state.miniscriptTemplate.isNotEmpty()) {
                            WalletConfigType.MINISCRIPT
                        } else {
                            getWalletConfigTypeBy(
                                n = state.groupSandbox.n ?: 3,
                                m = state.groupSandbox.m ?: 2
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openAssignSignerScreen(
        walletName: String,
        addressType: AddressType
    ) {
        if (addressType.isTaproot()) {
            navigator.openTaprootScreen(
                activityContext = this,
                walletName = walletName,
                walletType = WalletType.MULTI_SIG,
                addressType = addressType,
                decoyPin = args.decoyPin,
                quickWalletParam = args.quickWalletParam
            )
        } else {
            navigator.openConfigureWalletScreen(
                this,
                args = ConfigureWalletArgs(
                    walletName = walletName,
                    walletType = WalletType.MULTI_SIG,
                    addressType = addressType,
                    decoyPin = args.decoyPin,
                    quickWalletParam = args.quickWalletParam
                )
            )
        }
    }

    private fun showChangeAddressTypeDialog(onAction: () -> Unit) {
        NCWarningDialog(this).showDialog(
            message = getString(R.string.nc_change_address_type_group_wallet),
            btnYes = getString(R.string.nc_text_continue),
            onYesClick = {
                onAction()
            }
        )
    }

    private fun showMiniscriptConfigWarningDialog(onContinue: () -> Unit) {
        NCWarningDialog(this).showDialog(
            message = getString(R.string.nc_change_wallet_config_to_miniscript),
            btnYes = getString(R.string.nc_text_continue),
            onYesClick = {
                onContinue()
            }
        )
    }

    companion object {
        fun start(
            activityContext: Context,
            launcher: ActivityResultLauncher<Intent>? = null,
            args: AddWalletArgs
        ) {
            val intent = Intent(activityContext, AddWalletActivity::class.java).apply {
                putExtras(args.buildBundle())
            }
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                activityContext.startActivity(intent)
            }
        }
    }

}
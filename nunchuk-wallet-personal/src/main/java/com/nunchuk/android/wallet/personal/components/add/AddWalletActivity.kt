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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.WalletConfigViewOnlyDataComposer
import com.nunchuk.android.core.util.ADD_WALLET_RESULT
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddWalletActivity : BaseComposeActivity() {

    private val viewModel: AddWalletViewModel by viewModels()

    private val pin: String by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(DECOY_PIN).orEmpty()
    }

    private val groupWalletId: String by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(GROUP_WALLET_ID).orEmpty()
    }

    private val hasGroupSigner: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        intent.getBooleanExtra(HAS_GROUP_SIGNER, false)
    }

    private val viewOnlyComposer: WalletConfigViewOnlyDataComposer? by lazy(LazyThreadSafetyMode.NONE) {
        intent.parcelable(VIEW_ONLY_COMPOSER)
    }

    private var isAlreadyShowChangeAddressTypeDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            AddWalletView(
                state = state,
                viewOnlyComposer = viewOnlyComposer,
                isViewConfigOnly = viewOnlyComposer != null || !state.groupSandbox?.replaceWalletId.isNullOrEmpty(),
                isEditGroupWallet = groupWalletId.isNotEmpty(),
                onSelectAddressType = {
                    if (groupWalletId.isNotEmpty()) {
                        val action = {
                            viewModel.updateAddressTypeSelected(it)
                            viewModel.getFreeGroupWalletConfig(it)
                        }
                        if (viewModel.state.value.groupSandbox?.addressType != it && hasGroupSigner && isAlreadyShowChangeAddressTypeDialog.not()) {
                            showChangeAddressTypeDialog {
                                isAlreadyShowChangeAddressTypeDialog = true
                                action()
                            }
                        } else {
                            action()
                        }
                    } else {
                        viewModel.updateAddressTypeSelected(it)
                    }
                }, onContinue = { walletName, addressType, m, n ->
                    if (viewOnlyComposer != null) {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().apply { putExtra(ADD_WALLET_RESULT, walletName) }
                        )
                        finish()
                    } else if (groupWalletId.isNotEmpty()) {
                        viewModel.updateGroupSandboxConfig(walletName, m, n)
                    } else {
                        openAssignSignerScreen(
                            walletName = walletName,
                            addressType = addressType
                        )
                    }
                })
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
                decoyPin = pin
            )
        } else {
            navigator.openConfigureWalletScreen(
                this,
                args = ConfigureWalletArgs(
                    walletName = walletName,
                    walletType = WalletType.MULTI_SIG,
                    addressType = addressType,
                    decoyPin = pin
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

    companion object {
        private const val DECOY_PIN = "decoy_wallet"
        const val GROUP_WALLET_ID = "group_wallet_id"
        private const val HAS_GROUP_SIGNER = "has_group_signer"
        private const val VIEW_ONLY_COMPOSER = "view_only_composer"

        fun start(
            activityContext: Context,
            launcher: ActivityResultLauncher<Intent>? = null,
            decoyPin: String, groupWalletId: String,
            hasGroupSigner: Boolean, viewOnlyComposer: WalletConfigViewOnlyDataComposer? = null
        ) {
            val intent = Intent(activityContext, AddWalletActivity::class.java).apply {
                putExtra(DECOY_PIN, decoyPin)
                putExtra(GROUP_WALLET_ID, groupWalletId)
                putExtra(HAS_GROUP_SIGNER, hasGroupSigner)
                putExtra(VIEW_ONLY_COMPOSER, viewOnlyComposer)
            }
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                activityContext.startActivity(intent)
            }
        }
    }

}
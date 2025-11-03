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

package com.nunchuk.android.signer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignerIntroActivity : BaseComposeActivity() {
    private val supportedSigners: List<SupportedSigner> by lazy {
        intent.parcelableArrayList<SupportedSigner>(EXTRA_SUPPORTED_SIGNERS).orEmpty()
    }
    private val keyFlow by lazy { intent.getIntExtra(EXTRA_KEY_FLOW, KeyFlow.NONE) }
    private val onChainAddSignerParam by lazy {
        intent.parcelable<OnChainAddSignerParam>(EXTRA_ONCHAIN_ADD_SIGNER_PARAM)
    }

    private val viewModel: SignerIntroViewModel by viewModels()

    private val checkFirmwareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val filteredSigners =
                result.data?.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            if (!filteredSigners.isNullOrEmpty()) {
                val intent = Intent().apply {
                    putParcelableArrayListExtra(
                        GlobalResultKey.EXTRA_SIGNERS,
                        ArrayList(filteredSigners)
                    )
                }
                setResult(RESULT_OK, intent)
                finish()
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.init(onChainAddSignerParam)

        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                // Handle ViewModel events
                LaunchedEffect(Unit) {
                    viewModel.event.collect { event ->
                        when (event) {
                            is SignerIntroEvent.ShowFilteredTapSigners -> {
                                onFilteredTapSignersReady(event.signers)
                            }

                            SignerIntroEvent.OpenSetupTapSigner -> {
                                navigateToSetupTapSigner()
                            }
                        }
                    }
                }

                SignerIntroScreen(
                    keyFlow = keyFlow,
                    supportedSigners = supportedSigners,
                    viewModel = viewModel,
                    onChainAddSignerParam = onChainAddSignerParam,
                    onClick = { keyType: KeyType ->
                        when (keyType) {
                            KeyType.TAPSIGNER -> handleTapSignerSelection()
                            KeyType.COLDCARD -> handleColdCardSelection()
                            KeyType.JADE -> handleJadeSelection()
                            KeyType.PORTAL -> openPortalScreen()
                            KeyType.SEEDSIGNER -> handleSelectAddAirgapType(SignerTag.SEEDSIGNER)
                            KeyType.KEYSTONE -> handleSelectAddAirgapType(SignerTag.KEYSTONE)
                            KeyType.FOUNDATION -> handleSelectAddAirgapType(SignerTag.PASSPORT)
                            KeyType.SOFTWARE -> openAddSoftwareSignerScreen()
                            KeyType.GENERIC_AIRGAP -> openAddAirSignerIntroScreen()
                            else -> {}
                        }
                    }
                )
            }
        })
    }

    private fun onFilteredTapSignersReady(filteredSigners: List<SignerModel>) {
        val intent = Intent().apply {
            putParcelableArrayListExtra(GlobalResultKey.EXTRA_SIGNERS, ArrayList(filteredSigners))
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun handleTapSignerSelection() {
        if (onChainAddSignerParam != null) {
            viewModel.onTapSignerContinueClicked()
        } else {
            navigateToSetupTapSigner()
        }
    }

    private fun handleColdCardSelection() {
        if (onChainAddSignerParam == null || onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
            openSetupMk4()
            return
        }
        navigator.openCheckFirmwareActivity(
            activityContext = this,
            launcher = checkFirmwareLauncher,
            args = CheckFirmwareArgs(
                signerTag = SignerTag.COLDCARD,
                onChainAddSignerParam = onChainAddSignerParam,
                walletId = walletId,
                groupId = groupId
            )
        )
    }

    private fun handleJadeSelection() {
        if (onChainAddSignerParam == null || onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
            handleSelectAddAirgapType(SignerTag.JADE)
            return
        }
        navigator.openCheckFirmwareActivity(
            activityContext = this,
            launcher = checkFirmwareLauncher,
            args = CheckFirmwareArgs(
                signerTag = SignerTag.JADE,
                onChainAddSignerParam = onChainAddSignerParam,
                walletId = walletId,
                groupId = groupId
            )
        )
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            args = AddAirSignerArgs(
                isMembershipFlow = onChainAddSignerParam != null,
                tag = tag,
                groupId = groupId,
                walletId = walletId,
                onChainAddSignerParam = onChainAddSignerParam,
            )
        )
        finish()
    }

    private fun openSetupMk4() {
        navigator.openSetupMk4(
            activity = this,
            args = SetupMk4Args(
                fromMembershipFlow = onChainAddSignerParam != null,
                isFromAddKey = true,
                groupId = groupId,
                walletId = walletId,
                onChainAddSignerParam = onChainAddSignerParam,
            )
        )
        finish()
    }

    private fun openPortalScreen() {
        navigator.openPortalScreen(
            activity = this,
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = walletId.isNotEmpty() || onChainAddSignerParam != null,
                walletId = walletId,
                groupId = groupId,
            )
        )
        finish()
    }

    private fun openAddAirSignerIntroScreen() {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            args = AddAirSignerArgs(
                isMembershipFlow = onChainAddSignerParam != null,
                groupId = groupId,
                walletId = walletId,
                onChainAddSignerParam = onChainAddSignerParam,
            )
        )
        finish()
    }

    private fun openAddSoftwareSignerScreen() {
        val primaryKeyFlow =
            if (walletId.isNotEmpty()) KeyFlow.REPLACE_KEY_IN_FREE_WALLET else keyFlow
        navigator.openAddSoftwareSignerScreen(
            activityContext = this,
            keyFlow = primaryKeyFlow,
            groupId = groupId,
            walletId = walletId,
        )
        finish()
    }

    private fun navigateToSetupTapSigner() {
        startActivity(
            NfcSetupActivity.buildIntent(
                activity = this,
                setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                walletId = walletId,
                groupId = groupId,
            )
        )
        finish()
    }

    // replace key in free wallet
    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }

    // group sandbox id
    private val groupId by lazy { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }

    companion object {
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_SUPPORTED_SIGNERS = "supported_signers"
        private const val EXTRA_KEY_FLOW = "key_flow"
        private const val EXTRA_ONCHAIN_ADD_SIGNER_PARAM = "onchain_add_signer_param"

        fun start(
            activityContext: Context,
            walletId: String? = null,
            groupId: String? = null,
            supportedSigners: List<SupportedSigner>? = null,
            @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
            onChainAddSignerParam: OnChainAddSignerParam? = null,
        ) {
            activityContext.startActivity(
                buildIntent(
                    activityContext,
                    walletId,
                    groupId,
                    supportedSigners,
                    keyFlow,
                    onChainAddSignerParam
                )
            )
        }

        fun buildIntent(
            activityContext: Context,
            walletId: String? = null,
            groupId: String? = null,
            supportedSigners: List<SupportedSigner>? = null,
            @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
            onChainAddSignerParam: OnChainAddSignerParam? = null,
        ): Intent {
            return Intent(activityContext, SignerIntroActivity::class.java).apply {
                putExtra(EXTRA_WALLET_ID, walletId)
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_KEY_FLOW, keyFlow)
                putExtra(EXTRA_ONCHAIN_ADD_SIGNER_PARAM, onChainAddSignerParam)
                supportedSigners?.let {
                    putParcelableArrayListExtra(EXTRA_SUPPORTED_SIGNERS, ArrayList(it))
                }
            }
        }
    }
}
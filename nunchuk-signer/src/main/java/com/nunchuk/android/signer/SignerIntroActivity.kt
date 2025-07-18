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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignerIntroActivity : BaseComposeActivity() {
    private val supportedSigners: List<SupportedSigner> by lazy {
        intent.parcelableArrayList<SupportedSigner>(EXTRA_SUPPORTED_SIGNERS).orEmpty()
    }
    private val keyFlow by lazy { intent.getIntExtra(EXTRA_KEY_FLOW, KeyFlow.NONE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                SignerIntroScreen(
                    keyFlow = keyFlow,
                    supportedSigners = supportedSigners,
                    onClick = { keyType: KeyType ->
                        when (keyType) {
                            KeyType.TAPSIGNER -> navigateToSetupTapSigner()
                            KeyType.COLDCARD -> openSetupMk4()
                            KeyType.JADE -> handleSelectAddAirgapType(SignerTag.JADE)
                            KeyType.PORTAL -> openPortalScreen()
                            KeyType.SEEDSIGNER -> handleSelectAddAirgapType(SignerTag.SEEDSIGNER)
                            KeyType.KEYSTONE -> handleSelectAddAirgapType(SignerTag.KEYSTONE)
                            KeyType.FOUNDATION -> handleSelectAddAirgapType(SignerTag.PASSPORT)
                            KeyType.SOFTWARE -> openAddSoftwareSignerScreen()
                            KeyType.GENERIC_AIRGAP -> openAddAirSignerIntroScreen()
                        }
                    }
                )
            }
        })
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            isMembershipFlow = false,
            tag = tag,
            groupId = groupId,
            walletId = walletId,
        )
        finish()
    }

    private fun openSetupMk4() {
        navigator.openSetupMk4(
            activity = this,
            fromMembershipFlow = false,
            isFromAddKey = true,
            groupId = groupId,
            walletId = walletId,
        )
        finish()
    }

    private fun openPortalScreen() {
        navigator.openPortalScreen(
            activity = this,
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = walletId.isNotEmpty(),
                walletId = walletId,
                groupId = groupId,
            )
        )
        finish()
    }

    private fun openAddAirSignerIntroScreen() {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            isMembershipFlow = false,
            groupId = groupId,
            walletId = walletId
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
        private const val EXTRA_INDEX = "index"
        private const val EXTRA_SUPPORTED_SIGNERS = "supported_signers"
        private const val EXTRA_KEY_FLOW = "key_flow"

        fun start(
            activityContext: Context,
            walletId: String? = null,
            groupId: String? = null,
            index: Int = -1,
            supportedSigners: List<SupportedSigner>? = null,
            @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        ) {
            activityContext.startActivity(
                Intent(activityContext, SignerIntroActivity::class.java).apply {
                    putExtra(EXTRA_WALLET_ID, walletId)
                    putExtra(EXTRA_GROUP_ID, groupId)
                    putExtra(EXTRA_INDEX, index)
                    putExtra(EXTRA_KEY_FLOW, keyFlow)
                    supportedSigners?.let {
                        putParcelableArrayListExtra(EXTRA_SUPPORTED_SIGNERS, ArrayList(it))
                    }
                },
            )
        }
    }
}
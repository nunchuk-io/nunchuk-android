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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.tapsigner.SetUpNfcOptionSheet
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignerIntroActivity : BaseComposeActivity(),
    SetUpNfcOptionSheet.OptionClickListener {

//    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setLightStatusBar()
//        setupViews()
//    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                SignerIntroScreen(
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
            tag = tag
        )
        finish()
    }

    private fun openSetupMk4() {
        navigator.openSetupMk4(
            activity = this,
            fromMembershipFlow = false,
            walletId = walletId,
            isFromAddKey = true
        )
        finish()
    }

    override fun onOptionClickListener(option: SetUpNfcOptionSheet.SetUpNfcOption) {
        when (option) {
            SetUpNfcOptionSheet.SetUpNfcOption.ADD_NEW -> navigateToSetupTapSigner()
            SetUpNfcOptionSheet.SetUpNfcOption.RECOVER -> {
                startActivity(
                    NfcSetupActivity.buildIntent(
                        activity = this,
                        setUpAction = NfcSetupActivity.RECOVER_NFC,
                        walletId = walletId
                    )
                )
            }

            SetUpNfcOptionSheet.SetUpNfcOption.Mk4 -> {

            }

            SetUpNfcOptionSheet.SetUpNfcOption.PORTAL -> {

            }
        }
    }

//    private fun setupViews() {
//        binding.btnAddNFC.setOnClickListener {
//            SetUpNfcOptionSheet.newInstance().show(supportFragmentManager, "SetUpNfcOptionSheet")
//        }
//        binding.btnAddAirSigner.setOnClickListener { openAddAirSignerIntroScreen() }
//        binding.btnAddSSigner.setOnClickListener { openAddSoftwareSignerScreen() }
//        binding.toolbar.setNavigationOnClickListener {
//            finish()
//        }
//    }

    private fun openPortalScreen() {
        navigator.openPortalScreen(
            activity = this,
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = walletId.isNotEmpty(),
                walletId = walletId
            )
        )
        finish()
    }

    private fun openAddAirSignerIntroScreen() {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            isMembershipFlow = false,
            walletId = walletId
        )
        finish()
    }

    private fun openAddSoftwareSignerScreen() {
        val primaryKeyFlow =
            if (walletId.isNotEmpty()) KeyFlow.REPLACE_KEY_IN_FREE_WALLET else KeyFlow.NONE
        navigator.openAddSoftwareSignerScreen(
            activityContext = this,
            keyFlow = primaryKeyFlow,
            walletId = walletId
        )
        finish()
    }

    private fun navigateToSetupTapSigner() {
        startActivity(
            NfcSetupActivity.buildIntent(
                activity = this,
                setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                walletId = walletId,
            )
        )
        finish()
    }

    // replace key in free wallet
    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }

    companion object {
        private const val EXTRA_WALLET_ID = "wallet_id"

        fun start(activityContext: Context, walletId: String? = null) {
            activityContext.startActivity(
                Intent(activityContext, SignerIntroActivity::class.java).apply {
                    putExtra(EXTRA_WALLET_ID, walletId)
                },
            )
        }
    }
}
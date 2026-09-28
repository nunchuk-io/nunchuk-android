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

package com.nunchuk.android.main.membership.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.SignerIntroHostEvent
import com.nunchuk.android.signer.SignerIntroEvent
import com.nunchuk.android.signer.SignerIntroViewModel
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignerIntroActivity : BaseComposeActivity(), BottomSheetOptionListener {
    @Inject lateinit var membershipStepManager: MembershipStepManager

    private val request by lazy {
        requireNotNull(intent.parcelable<SignerIntroRequest>(EXTRA_REQUEST))
    }
    private val viewModel: SignerIntroViewModel by viewModels()
    private lateinit var deviceLauncher: SignerDeviceLauncher

    private val recoverSeedLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val mnemonic = result.data?.getStringExtra(GlobalResultKey.MNEMONIC).orEmpty()
            val passphrase = result.data?.getStringExtra(GlobalResultKey.PASSPHRASE).orEmpty()
            if (mnemonic.isNotEmpty()) {
                val signerCount = viewModel.state.value.allSigners.size + 1
                val signerName = "Inheritance key #$signerCount"
                viewModel.createSoftwareSignerFromMnemonic(mnemonic, passphrase, signerName)
            }
        }
    }

    private val xprvLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val xprv = result.data?.getStringExtra(GlobalResultKey.XPRV).orEmpty()
            if (xprv.isNotEmpty()) {
                val signerCount = viewModel.state.value.allSigners.size + 1
                val signerName = "Inheritance key #$signerCount"
                viewModel.createSoftwareSignerFromXprv(xprv, signerName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.init(request)
        deviceLauncher = SignerDeviceLauncher(
            activity = this,
            navigator = navigator,
            request = request,
            membershipStep = { membershipStepManager.currentStep },
            skipPicker = viewModel.verifyingKeyType != null,
            onSigner = ::returnSigner,
            onResult = ::relayResult,
        )
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SignerIntroRoute(
                    request = request,
                    viewModel = viewModel,
                    onEvent = ::handleEvent,
                    onSigner = ::returnSigner,
                    onMore = ::handleShowMore,
                    onRecoverSeed = ::onRecoverSeedClicked,
                    onRecoverXprv = ::onRecoverXprvClicked,
                    onRecoverTapsigner = deviceLauncher::recoverTapsigner,
                )
            }
        })
    }

    private fun handleEvent(event: SignerIntroHostEvent) {
        when (event) {
            is SignerIntroEvent.OpenDevice -> deviceLauncher.launch(event.action)
            is SignerIntroEvent.ReturnHardwareTag -> relayResult(RESULT_OK, Intent().apply {
                putExtra(GlobalResultKey.EXTRA_SIGNER_TAG, event.tag)
            })
            SignerIntroEvent.ReturnPlatformKey -> relayResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_PLATFORM_KEY_SELECTED, true)
            })
            SignerIntroEvent.RestartWizardSuccess -> {
                navigator.openMembershipActivity(
                    activityContext = this,
                    groupStep = MembershipStage.NONE,
                    isPersonalWallet = membershipStepManager.isPersonalWallet(),
                    isClearTop = true,
                    quickWalletParam = null,
                )
                relayResult(RESULT_OK, null)
            }
            is SignerIntroEvent.CreateSoftwareSignerSuccess -> returnSigner(event.signer)
            is SignerIntroEvent.Error -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun relayResult(resultCode: Int, data: Intent?) {
        setResult(resultCode, data)
        finish()
    }

    private fun returnSigner(signer: SignerModel) = relayResult(RESULT_OK, Intent().apply {
        putExtra(GlobalResultKey.EXTRA_SIGNER, signer)
    })

    private fun onRecoverSeedClicked() {
        navigator.openRecoverSeedScreen(
            launcher = recoverSeedLauncher,
            activityContext = this,
            keyFlow = KeyFlow.ADD_AND_RETURN_PASSPHRASE
        )
    }

    private fun onRecoverXprvClicked() {
        navigator.openAddSoftwareSignerScreen(
            activityContext = this,
            keyFlow = KeyFlow.ADD_AND_RETURN,
            launcher = xprvLauncher,
            masterSignerId = ""
        )
    }

    private fun handleShowMore() {
        val options = mutableListOf<SheetOption>()
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_RESTART_WIZARD,
                label = getString(R.string.nc_restart_wizard)
            )
        )
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_EXIT_WIZARD,
                label = getString(R.string.nc_exit_wizard)
            )
        )
        BottomSheetOption.newInstance(options).show(supportFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_RESTART_WIZARD) {
            NCWarningDialog(this).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_confirm_restart_wizard),
                onYesClick = {
                    viewModel.resetWizard(membershipStepManager.localMembershipPlan, request.groupId)
                }
            )
        } else if (option.type == SheetOptionType.TYPE_EXIT_WIZARD) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_resume_wizard_desc),
                onYesClick = {
                    finish()
                }
            )
        }
    }

    companion object {
        const val EXTRA_PLATFORM_KEY_SELECTED = "platform_key_selected"
        private const val EXTRA_REQUEST = "signer_intro_request"

        fun buildIntent(context: Context, request: SignerIntroRequest): Intent =
            Intent(context, SignerIntroActivity::class.java).putExtra(EXTRA_REQUEST, request)
    }
}

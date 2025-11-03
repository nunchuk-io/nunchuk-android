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

package com.nunchuk.android.signer.software.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.KeyFlow.isAddAndReturnFlow
import com.nunchuk.android.core.signer.KeyFlow.isSignInFlow
import com.nunchuk.android.core.util.bindEnableState
import com.nunchuk.android.core.util.navigateToSelectWallet
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.CanGoNextStepEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.InvalidMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.MnemonicRequiredEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.UpdateMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.ValidMnemonicEvent
import com.nunchuk.android.signer.software.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RecoverSeedActivity : BaseActivity<ActivityRecoverSeedBinding>() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    @Inject
    lateinit var assistedWalletManager: AssistedWalletManager

    private val viewModel: RecoverSeedViewModel by viewModels()

    private lateinit var adapter: RecoverSeedSuggestionAdapter

    private val isHotWalletRecovery: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_RECOVER_HOT_WALLET, false)
    }

    private val groupId: String? by lazy {
        intent.getStringExtra(EXTRA_GROUP_ID)
    }

    private val replacedXfp: String by lazy {
        intent.getStringExtra(EXTRA_REPLACED_XFP).orEmpty()
    }

    private val walletId: String by lazy {
        intent.getStringExtra(EXTRA_WALLET_ID).orEmpty()
    }

    private val quickWalletParam: QuickWalletParam? by lazy {
        intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM)
    }

    override fun initializeBinding() = ActivityRecoverSeedBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        if (replacedXfp.isNotEmpty()) {
            viewModel.getReplaceSignerName(replacedXfp)
        }
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: RecoverSeedState) {
        adapter.items = state.suggestions
        binding.recyclerView.scrollToPosition(0)
    }

    private fun handleEvent(event: RecoverSeedEvent) {
        when (event) {
            MnemonicRequiredEvent -> binding.mnemonic.setError(getString(R.string.nc_text_required))
            InvalidMnemonicEvent -> binding.mnemonic.setError(getString(R.string.nc_invalid_seed_phrase))
            is ValidMnemonicEvent -> {
                val primaryKeyFlow = intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, KeyFlow.NONE)
                when {
                    primaryKeyFlow.isSignInFlow() -> {
                        navigator.openPrimaryKeyEnterPassphraseScreen(
                            this,
                            event.mnemonic,
                            primaryKeyFlow
                        )
                    }

                    primaryKeyFlow.isAddAndReturnFlow() -> {
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(GlobalResultKey.MNEMONIC, event.mnemonic)
                            },
                        )
                        finish()
                    }

                    assistedWalletManager.isGroupAssistedWallet(groupId) || replacedXfp.isNotEmpty() -> {
                        val signerName = if (replacedXfp.isNotEmpty()) {
                            viewModel.state.value?.replaceSignerName.orEmpty()
                        } else {
                            "Key${membershipStepManager.getNextKeySuffixByType(SignerType.SOFTWARE)}"
                        }
                        navigator.openSetPassphraseScreen(
                            activityContext = this,
                            mnemonic = event.mnemonic,
                            signerName = signerName,
                            groupId = groupId,
                            replacedXfp = replacedXfp,
                            walletId = walletId,
                        )
                    }

                    else -> {
                        val passphrase = intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
                        navigator.openAddSoftwareSignerNameScreen(
                            activityContext = this,
                            mnemonic = event.mnemonic,
                            keyFlow = primaryKeyFlow,
                            passphrase = passphrase,
                            walletId = walletId,
                            groupId = groupId,
                        )
                    }
                }
            }

            is UpdateMnemonicEvent -> updateMnemonic(event.mnemonic)
            is CanGoNextStepEvent -> binding.btnContinue.bindEnableState(event.canGoNext)
            is RecoverSeedEvent.RecoverHotWalletSuccess -> {
                navigateToSelectWallet(
                    navigator = navigator,
                    quickWalletParam = quickWalletParam
                ) {
                    navigator.returnToMainScreen(this)
                    navigator.openWalletDetailsScreen(
                        activityContext = this,
                        walletId = event.walletId
                    )
                }
                NcToastManager.scheduleShowMessage(getString(R.string.nc_my_hot_wallet_has_been_recovered))
            }

            is RecoverSeedEvent.ExistingSignerEvent -> {
                NCInfoDialog(this)
                    .showDialog(
                        message = String.format(
                            getString(R.string.nc_existing_key_change_key_type),
                            event.fingerprint.uppercase(Locale.getDefault())
                        ),
                        btnYes = getString(R.string.nc_text_yes),
                        btnInfo = getString(R.string.nc_text_no),
                        onYesClick = {
                            viewModel.recoverHotWallet(true)
                        },
                        onInfoClick = {}
                    )
            }
        }
    }

    private fun updateMnemonic(mnemonic: String) {
        val withSpace = "$mnemonic "
        binding.mnemonic.getEditTextView().setText(withSpace)
        binding.mnemonic.getEditTextView().setSelection(mnemonic.length + 1)
    }

    private fun setupViews() {
        if (isHotWalletRecovery) {
            binding.toolbarTitle.text = getString(R.string.nc_recover_hot_wallet)
        }
        binding.mnemonic.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_120))
        binding.mnemonic.addTextChangedCallback(viewModel::handleInputEvent)
        adapter = RecoverSeedSuggestionAdapter(viewModel::handleSelectWord)
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent(isHotWalletRecovery)
        }
    }

    companion object {
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_RECOVER_HOT_WALLET = "EXTRA_RECOVER_HOT_WALLET"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        private const val EXTRA_REPLACED_XFP = "EXTRA_REPLACED_XFP"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun start(
            activityContext: Context,
            passphrase: String,
            primaryKeyFlow: Int,
            isRecoverHotWallet: Boolean = false,
            groupId: String? = null,
            replacedXfp: String? = null,
            walletId: String = "",
            quickWalletParam: QuickWalletParam? = null,
        ) {
            activityContext.startActivity(
                buildIntent(
                    activityContext,
                    primaryKeyFlow,
                    passphrase,
                    isRecoverHotWallet,
                    groupId,
                    replacedXfp,
                    walletId,
                    quickWalletParam
                )
            )
        }

        fun buildIntent(
            activityContext: Context,
            keyFlow: Int = 0,
            passphrase: String = "",
            isRecoverHotWallet: Boolean = false,
            groupId: String? = null,
            replacedXfp: String? = null,
            walletId: String = "",
            quickWalletParam: QuickWalletParam? = null,
        ) = Intent(
            activityContext,
            RecoverSeedActivity::class.java
        ).apply {
            putExtra(
                EXTRA_PRIMARY_KEY_FLOW,
                keyFlow
            )
            putExtra(
                EXTRA_PASSPHRASE,
                passphrase
            )
            putExtra(
                EXTRA_RECOVER_HOT_WALLET,
                isRecoverHotWallet
            )
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_REPLACED_XFP, replacedXfp)
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
        }
    }
}

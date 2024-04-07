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
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.core.util.bindEnableState
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.CanGoNextStepEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.InvalidMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.MnemonicRequiredEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.UpdateMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.ValidMnemonicEvent
import com.nunchuk.android.signer.software.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoverSeedActivity : BaseActivity<ActivityRecoverSeedBinding>() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: RecoverSeedViewModel by viewModels()

    private lateinit var adapter: RecoverSeedSuggestionAdapter

    private val isHotWalletRecovery: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_RECOVER_HOT_WALLET, false)
    }

    private val groupId: String? by lazy {
        intent.getStringExtra(EXTRA_GROUP_ID)
    }

    override fun initializeBinding() = ActivityRecoverSeedBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
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
                val primaryKeyFlow = intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
                when  {
                    primaryKeyFlow == PrimaryKeyFlow.SIGN_IN -> {
                        navigator.openPrimaryKeyEnterPassphraseScreen(
                            this,
                            event.mnemonic,
                            primaryKeyFlow
                        )
                    }

                    !groupId.isNullOrEmpty() -> {
                        navigator.openSetPassphraseScreen(
                            activityContext = this,
                            mnemonic = event.mnemonic,
                            signerName = "Key${membershipStepManager.getNextKeySuffixByType(
                                SignerType.SOFTWARE)}",
                            groupId = groupId
                        )
                    }

                    else -> {
                        val passphrase = intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
                        navigator.openAddSoftwareSignerNameScreen(
                            this,
                            mnemonic = event.mnemonic,
                            passphrase = passphrase,
                            primaryKeyFlow = primaryKeyFlow
                        )
                    }
                }
            }

            is UpdateMnemonicEvent -> updateMnemonic(event.mnemonic)
            is CanGoNextStepEvent -> binding.btnContinue.bindEnableState(event.canGoNext)
            is RecoverSeedEvent.RecoverHotWalletSuccess -> {
                navigator.returnToMainScreen()
                navigator.openWalletDetailsScreen(activityContext = this, walletId = event.walletId)
                NcToastManager.scheduleShowMessage(getString(R.string.nc_my_hot_wallet_has_been_recovered))
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
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        private const val EXTRA_RECOVER_HOT_WALLET = "EXTRA_RECOVER_HOT_WALLET"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"

        fun start(
            activityContext: Context,
            passphrase: String,
            primaryKeyFlow: Int,
            isRecoverHotWallet: Boolean = false,
            groupId: String? = null,
        ) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    RecoverSeedActivity::class.java
                ).apply {
                    putExtra(
                        EXTRA_PRIMARY_KEY_FLOW,
                        primaryKeyFlow
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
                },
            )
        }
    }

}

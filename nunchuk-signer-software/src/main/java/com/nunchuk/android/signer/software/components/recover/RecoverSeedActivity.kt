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
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.core.util.bindEnableState
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.CanGoNextStepEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.InvalidMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.MnemonicRequiredEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.UpdateMnemonicEvent
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.ValidMnemonicEvent
import com.nunchuk.android.signer.software.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecoverSeedActivity : BaseActivity<ActivityRecoverSeedBinding>() {

    private val viewModel: RecoverSeedViewModel by viewModels()

    private lateinit var adapter: RecoverSeedSuggestionAdapter

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
            InvalidMnemonicEvent -> binding.mnemonic.setError(getString(R.string.nc_error_invalid_signer_spec))
            is ValidMnemonicEvent -> {
                when (val primaryKeyFlow =
                    intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)) {
                    PrimaryKeyFlow.SIGN_IN -> {
                        navigator.openPrimaryKeyEnterPassphraseScreen(
                            this,
                            event.mnemonic,
                            primaryKeyFlow
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
        }
    }

    private fun updateMnemonic(mnemonic: String) {
        val withSpace = "$mnemonic "
        binding.mnemonic.getEditTextView().setText(withSpace)
        binding.mnemonic.getEditTextView().setSelection(mnemonic.length + 1)
    }

    private fun setupViews() {
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
            viewModel.handleContinueEvent()
        }
    }

    companion object {
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"

        fun start(activityContext: Context, passphrase: String, primaryKeyFlow: Int) {
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
                })
        }
    }

}

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

package com.nunchuk.android.signer.software.components.name

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.KeyFlow.isReplaceFlow
import com.nunchuk.android.core.signer.KeyFlow.isSignInFlow
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameInputCompletedEvent
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameRequiredEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateSoftwareSignerCompletedEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseViewModel
import com.nunchuk.android.signer.software.databinding.ActivityAddNameBinding
import com.nunchuk.android.signer.software.handleCreateSoftwareSignerEvent
import com.nunchuk.android.signer.software.onCreateSignerCompleted
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddSoftwareSignerNameActivity : BaseActivity<ActivityAddNameBinding>() {

    @Inject
    internal lateinit var vmFactory: AddSoftwareSignerNameViewModel.Factory

    private val setPassphraseViewModel by viewModels<SetPassphraseViewModel>()

    private val args: AddSoftwareSignerNameArgs by lazy {
        AddSoftwareSignerNameArgs.deserializeFrom(
            intent
        )
    }
    private val viewModel: AddSoftwareSignerNameViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    override fun initializeBinding() = ActivityAddNameBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
        setPassphraseViewModel.event.observe(this) { event ->
            if (handleCreateSoftwareSignerEvent(event)) return@observe
            if (event is CreateSoftwareSignerCompletedEvent) {
                onCreateSignerCompleted(
                    navigator = navigator,
                    masterSigner = event.masterSigner,
                    skipPassphrase = event.skipPassphrase,
                    keyFlow = args.primaryKeyFlow,
                    replacedXfp = "",
                    groupId = args.groupId.orEmpty(),
                    passphrase = "",
                    mnemonic = args.mnemonic,
                    signerName = viewModel.getSignerName(),
                )
            }
        }
    }

    private fun handleState(state: AddSoftwareSignerNameState) {
        val signerName = state.signerName
        val counter = "${signerName.length}/$MAX_LENGTH"
        binding.nameCounter.text = counter
    }

    private fun handleEvent(event: AddSoftwareSignerNameEvent) {
        when (event) {
            is SignerNameInputCompletedEvent -> {
                if (!args.xprv.isNullOrEmpty()) {
                    setPassphraseViewModel.createSoftwareSigner(
                        isReplaceKey = true,
                        signerName = event.signerName,
                        mnemonic = args.mnemonic,
                        passphrase = "",
                        primaryKeyFlow = args.primaryKeyFlow,
                        groupId = args.groupId.orEmpty(),
                        replacedXfp = "",
                        walletId = args.walletId.orEmpty(),
                        isQuickWallet = false,
                        skipPassphrase = true,
                        xprv = args.xprv.orEmpty(),
                    )
                } else if (args.primaryKeyFlow.isSignInFlow()) {
                    viewModel.getTurnOnNotification()
                } else if (args.primaryKeyFlow.isReplaceFlow()) {
                    openSetPassphraseScreen(event.signerName, args.passphrase)
                } else {
                    openSetPassphraseScreen(event.signerName, "")
                }
            }

            is SignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
            is AddSoftwareSignerNameEvent.ImportPrimaryKeyErrorEvent -> NCToastMessage(this).showError(
                message = event.message
            )

            is AddSoftwareSignerNameEvent.LoadingEvent -> showOrHideLoading(event.loading)
            is AddSoftwareSignerNameEvent.InitFailure -> {
                NCToastMessage(this).showError(message = event.message)
                finish()
            }

            is AddSoftwareSignerNameEvent.GetTurnOnNotificationSuccess -> openNextScreen(event.isTurnOn)
        }
    }

    private fun openNextScreen(turnOn: Boolean) {
        val isEnabledNotification = NotificationUtils.areNotificationsEnabled(this)
        val messages = ArrayList<String>()
        messages.add(String.format(getString(R.string.nc_text_signed_in_with_data), args.username))
        messages.add(
            String.format(
                getString(R.string.nc_text_key_has_been_added_data),
                viewModel.getSignerName()
            )
        )
        if (turnOn && isEnabledNotification) {
            navigator.openTurnNotificationScreen(
                this,
                messages = messages
            )
        } else {
            navigator.openMainScreen(
                this,
                messages = messages,
                isClearTask = true
            )
        }
        viewModel.updateTurnOnNotification()
        finish()
    }

    private fun openSetPassphraseScreen(signerName: String, passphrase: String) {
        navigator.openSetPassphraseScreen(
            activityContext = this,
            mnemonic = args.mnemonic,
            signerName = signerName,
            passphrase = passphrase,
            keyFlow = args.primaryKeyFlow,
            walletId = args.walletId.orEmpty(),
            groupId = args.groupId.orEmpty(),
        )
    }

    private fun setupViews() {
        binding.signerName.setMaxLength(MAX_LENGTH)
        binding.signerName.addTextChangedCallback(viewModel::updateSignerName)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinue() }
    }

    companion object {
        private const val MAX_LENGTH = 20

        fun start(
            activityContext: Context,
            mnemonic: String,
            primaryKeyFlow: Int,
            username: String?,
            passphrase: String,
            address: String?,
            walletId: String?,
            groupId: String?,
            xprv: String?
        ) {
            activityContext.startActivity(
                AddSoftwareSignerNameArgs(
                    mnemonic = mnemonic,
                    primaryKeyFlow = primaryKeyFlow,
                    username = username,
                    passphrase = passphrase,
                    address = address,
                    walletId = walletId,
                    xprv = xprv,
                    groupId = groupId
                ).buildIntent(
                    activityContext
                )
            )
        }
    }

}
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

package com.nunchuk.android.signer.software.components.primarykey.manuallysignature

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeyManuallySignatureBinding
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.getTrimmedText
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeyManuallySignatureActivity : BaseActivity<ActivityPkeyManuallySignatureBinding>() {

    @Inject
    internal lateinit var vmFactory: PKeyManuallySignatureViewModel.Factory

    private val args: PKeyManuallySignatureArgs by lazy {
        PKeyManuallySignatureArgs.deserializeFrom(
            intent
        )
    }

    private val viewModel: PKeyManuallySignatureViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    override fun initializeBinding() =
        ActivityPkeyManuallySignatureBinding.inflate(layoutInflater)

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

    private fun handleState(state: PKeyManuallySignatureState) {
        binding.challengeInput.setText(state.challengeMessage)
    }

    private fun handleEvent(event: PKeyManuallySignatureEvent) {
        when (event) {
            is PKeyManuallySignatureEvent.LoadingEvent -> showOrHideLoading(loading = event.loading)
            is PKeyManuallySignatureEvent.ProcessFailure -> {
                hideLoading()
                NCToastMessage(this).showError(event.message)
            }
            is PKeyManuallySignatureEvent.SignInSuccess -> {
                hideLoading()
                viewModel.getTurnOnNotification()
            }
            is PKeyManuallySignatureEvent.GetTurnOnNotificationSuccess -> openNextScreen(event.isTurnOn)
        }
    }

    private fun openNextScreen(turnOn: Boolean) {
        val isEnabledNotification = NotificationUtils.areNotificationsEnabled(this@PKeyManuallySignatureActivity)
        val messages = ArrayList<String>()
        messages.add(String.format(getString(R.string.nc_text_signed_in_with_data), args.username))
        if (turnOn && isEnabledNotification) {
            navigator.openTurnNotificationScreen(
                this@PKeyManuallySignatureActivity,
                messages = messages
            )
        } else {
            navigator.openMainScreen(
                this@PKeyManuallySignatureActivity,
                messages = messages,
                isClearTask = true
            )
        }
        viewModel.updateTurnOnNotification()
        finish()
    }

    private fun setupViews() {
        binding.yourSignatureInput.addTextChangedCallback {
            viewModel.updateSignature(it)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnSignIn.setOnClickListener { viewModel.handleSignIn() }
        binding.yourSignatureInput.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_140))
        binding.staySignIn.setOnCheckedChangeListener { _, checked ->
            viewModel.updateStaySignedIn(
                checked
            )
        }
        binding.copyImage.setOnDebounceClickListener {
            copyChallengeMessageText(binding.challengeInput.getTrimmedText())
        }
        binding.reloadImage.setOnDebounceClickListener {
            viewModel.getChallengeMessage()
        }
    }

    private fun copyChallengeMessageText(text: String) {
        this.copyToClipboard(label = "Nunchuk", text = text)
        NCToastMessage(this).showMessage(getString(R.string.nc_primary_key_signin_manually_copy_clipboard))
    }

    companion object {

        fun start(
            activityContext: Context,
            username: String
        ) {
            activityContext.startActivity(
                PKeyManuallySignatureArgs(
                    username = username,
                ).buildIntent(
                    activityContext
                )
            )
        }
    }
}
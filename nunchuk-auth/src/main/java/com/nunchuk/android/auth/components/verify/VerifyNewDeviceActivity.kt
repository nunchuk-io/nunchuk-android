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

package com.nunchuk.android.auth.components.verify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.ProcessingEvent
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.SignInErrorEvent
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.SignInSuccessEvent
import com.nunchuk.android.auth.databinding.ActivityVerifyNewDeviceBinding
import com.nunchuk.android.auth.util.getTextTrimmed
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyNewDeviceActivity : BaseActivity<ActivityVerifyNewDeviceBinding>() {

    private val viewModel: VerifyNewDeviceViewModel by viewModels()

    private val email
        get() = intent.getStringExtra(EXTRAS_EMAIL)
    private val loginHalfToken
        get() = intent.getStringExtra(EXTRAS_LOGIN_HALF_TOKEN)
    private val deviceId
        get() = intent.getStringExtra(EXTRAS_DEVICE_ID)
    private val staySignedIn
        get() = intent.getBooleanExtra(EXTRAS_STAY_SIGNED_IN, false)

    override fun initializeBinding() = ActivityVerifyNewDeviceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)
        setupViews()
        observeEvent()
    }

    private fun showToolbarBackButton() {
        setSupportActionBar(binding.toolbarVerifyScreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is SignInErrorEvent -> onSignInError(it.message)
                is SignInSuccessEvent -> {
                    openMainScreen(it.token, it.encryptedDeviceId)
                }
                is ProcessingEvent -> showLoading()
            }
        }
    }

    private fun onSignInError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun openMainScreen(token: String, deviceId: String) {
        hideLoading()
        setResult(RESULT_OK)
        finish()
        if (NotificationUtils.areNotificationsEnabled(this).not()) {
            navigator.openTurnNotificationScreen(this)
        } else {
            navigator.openMainScreen(
                activityContext = this,
                loginHalfToken = token,
                deviceId = deviceId,
                isClearTask = true
            )
        }
    }

    private fun setupViews() {
        binding.tvConfirmInstruction.text = getString(R.string.nc_text_verify_instruction, email)
        binding.btnContinue.setOnClickListener { onVerifyNewDeviceClick() }
        showToolbarBackButton()
    }

    private fun onVerifyNewDeviceClick() {
        viewModel.handleVerifyNewDevice(
            email = email.orEmpty(),
            loginHalfToken = loginHalfToken.orEmpty(),
            pin = binding.edtConfirmCode.getTextTrimmed(),
            deviceId = deviceId.orEmpty(),
            staySignedIn = staySignedIn
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // FIXME extract/wrap serialize/deserialize logic with ActivityArgs
    companion object {
        const val EXTRAS_EMAIL = "EXTRAS_EMAIL"
        const val EXTRAS_LOGIN_HALF_TOKEN = "EXTRAS_LOGIN_HALF_TOKEN"
        const val EXTRAS_DEVICE_ID = "EXTRAS_DEVICE_ID"
        const val EXTRAS_STAY_SIGNED_IN = "EXTRAS_STAY_SIGNED_IN"

        fun buildIntent(
            activityContext: Context,
            email: String,
            loginHalfToken: String,
            deviceId: String,
            staySignedIn: Boolean
        ): Intent {
            return Intent(activityContext, VerifyNewDeviceActivity::class.java).apply {
                putExtra(EXTRAS_EMAIL, email)
                putExtra(EXTRAS_LOGIN_HALF_TOKEN, loginHalfToken)
                putExtra(EXTRAS_DEVICE_ID, deviceId)
                putExtra(EXTRAS_STAY_SIGNED_IN, staySignedIn)
            }
        }
    }

}
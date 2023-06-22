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

package com.nunchuk.android.auth.components.forgot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.forgot.ForgotPasswordEvent.*
import com.nunchuk.android.auth.databinding.ActivityForgotPasswordBinding
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding>() {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun initializeBinding() = ActivityForgotPasswordBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        setupViews()

        observeEvent()
    }

    private fun setupViews() {
        binding.forgotPassword.setOnClickListener { onForgotPasswordClicked() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun onForgotPasswordClicked() {
        viewModel.handleForgotPassword(binding.email.getEditText())
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                EmailInvalidEvent -> showEmailError(R.string.nc_text_email_invalid)
                EmailRequiredEvent -> showEmailError(R.string.nc_text_required)
                EmailValidEvent -> hideEmailError()
                LoadingEvent -> showLoading()
                is ForgotPasswordSuccessEvent -> openRecoverPasswordScreen(it.email)
                is ForgotPasswordErrorEvent -> forgotPasswordError(it)
            }
        }
    }

    private fun forgotPasswordError(event: ForgotPasswordErrorEvent) {
        hideLoading()
        showToast(event.errorMessage.orUnknownError())
    }

    private fun openRecoverPasswordScreen(email: String) {
        hideLoading()
        navigator.openRecoverPasswordScreen(this, email)
    }

    private fun hideEmailError() {
        binding.email.hideError()
    }

    private fun showEmailError(errorMessageId: Int) {
        binding.email.setError(getString(errorMessageId))
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, ForgotPasswordActivity::class.java))
        }
    }

}
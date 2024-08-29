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

package com.nunchuk.android.auth.components.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.signup.SignUpEvent.AccountExistedEvent
import com.nunchuk.android.auth.components.signup.SignUpEvent.EmailInvalidEvent
import com.nunchuk.android.auth.components.signup.SignUpEvent.EmailRequiredEvent
import com.nunchuk.android.auth.components.signup.SignUpEvent.EmailValidEvent
import com.nunchuk.android.auth.components.signup.SignUpEvent.LoadingEvent
import com.nunchuk.android.auth.components.signup.SignUpEvent.SignUpErrorEvent
import com.nunchuk.android.auth.components.signup.SignUpEvent.SignUpSuccessEvent
import com.nunchuk.android.auth.databinding.ActivitySignupBinding
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showKeyboard
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpActivity : BaseActivity<ActivitySignupBinding>() {

    private val viewModel: SignUpViewModel by viewModels()

    override fun initializeBinding() = ActivitySignupBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)
        setupViews()
        observeEvent()
    }

    private fun setupViews() {
        binding.signUp.setOnDebounceClickListener { onRegisterClicked() }
        binding.signUpPrimaryKey.setOnDebounceClickListener { openSignUpPrimaryKeyScreen() }
        binding.ivBack.setOnDebounceClickListener { finish() }

        binding.tvTermAndPolicy.linkify(getString(R.string.nc_hyperlink_text_term), TERM_URL)
        binding.tvTermAndPolicy.linkify(getString(R.string.nc_hyperlink_text_policy), PRIVACY_URL)
        binding.email.getEditTextView().showKeyboard()
    }

    private fun onRegisterClicked() {
        viewModel.handleRegister(binding.email.getEditText().trim(), binding.email.getEditText().trim())
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: SignUpEvent) {
        when (event) {
            EmailInvalidEvent -> showEmailError(R.string.nc_text_email_invalid)
            EmailRequiredEvent -> showEmailError(R.string.nc_text_required)
            EmailValidEvent -> hideEmailError()
            LoadingEvent -> showLoading()
            is SignUpSuccessEvent -> openChangePasswordScreen()
            is SignUpErrorEvent -> openSignUpError(event.errorMessage.orUnknownError())
            is AccountExistedEvent -> switchLoginPage(event.errorMessage.orUnknownError())
        }
    }

    private fun openSignUpError(message: String) {
        hideLoading()
        showToast(message)
    }

    private fun switchLoginPage(message: String) {
        hideLoading()
        showToast(message)
        openLoginScreen()
    }

    private fun hideEmailError() {
        binding.email.hideError()
    }

    private fun showEmailError(errorMessageId: Int) {
        binding.email.setError(getString(errorMessageId))
    }

    private fun openLoginScreen() {
        finish()
        navigator.openSignInScreen(this)
    }

    private fun openSignUpPrimaryKeyScreen() {
        finish()
        navigator.openPrimaryKeyIntroScreen(this)
    }

    private fun openChangePasswordScreen() {
        hideLoading()
        finish()
        navigator.openChangePasswordScreen(this, true)
    }

    companion object {
        private const val PRIVACY_URL = "https://www.nunchuk.io/privacy"
        private const val TERM_URL = "https://www.nunchuk.io/terms"
        fun start(activityContext: Context) {
            val intent = Intent(activityContext, SignUpActivity::class.java)
            activityContext.startActivity(intent)
        }
    }
}


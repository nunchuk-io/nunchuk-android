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

package com.nunchuk.android.auth.components.changepass

import android.content.Context
import android.content.Intent
import android.graphics.Typeface.BOLD
import android.os.Bundle
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ChangePasswordSuccessError
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ChangePasswordSuccessEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ConfirmPasswordNotMatchedEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ConfirmPasswordRequiredEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ConfirmPasswordValidEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.LoadingEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.NewPasswordRequiredEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.NewPasswordValidEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.OldPasswordRequiredEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.OldPasswordValidEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ShowEmailSentEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordViewModel.Companion.SPECIAL_CHARACTERS
import com.nunchuk.android.auth.databinding.ActivityChangePasswordBinding
import com.nunchuk.android.core.account.SignInType
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class ChangePasswordActivity : BaseActivity<ActivityChangePasswordBinding>() {

    private val viewModel: ChangePasswordViewModel by viewModels()

    private val isNewAccount: Boolean by lazy {
        intent.getBooleanExtra(IS_NEW_ACCOUNT, false)
    }

    override fun initializeBinding() = ActivityChangePasswordBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setupViews()

        observeEvent()
    }

    private fun showToolbarBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is OldPasswordRequiredEvent -> binding.oldPassword.setError(getString(R.string.nc_text_required))
                is OldPasswordValidEvent -> binding.oldPassword.hideError()
                is NewPasswordRequiredEvent -> binding.newPassword.setError(getString(R.string.nc_text_required))
                is NewPasswordValidEvent -> binding.newPassword.hideError()
                is ConfirmPasswordRequiredEvent -> binding.confirmPassword.setError(getString(R.string.nc_text_required))
                is ConfirmPasswordValidEvent -> binding.confirmPassword.hideError()
                is ConfirmPasswordNotMatchedEvent -> binding.confirmPassword.setError(getString(R.string.nc_text_password_does_not_match))
                is ChangePasswordSuccessError -> showChangePasswordError(it.errorMessage.orUnknownError())
                is ChangePasswordSuccessEvent -> openLoginPage()
                is ShowEmailSentEvent -> showEmailConfirmation(it.email)
                LoadingEvent -> showLoading()
                is ChangePasswordEvent.ResendPasswordSuccessEvent -> {
                    NCToastMessage(this).showMessage(getString(R.string.nc_resend_request_submitted))
                    showCountdownTimer(it.email)
                }
            }
        }
    }

    private fun openLoginPage() {
        NcToastManager.scheduleShowMessage(getString(R.string.nc_your_password_changed))
        navigator.openSignInScreen(this, type = SignInType.PASSWORD)
        finish()
    }

    private fun showEmailConfirmation(email: String) {
        binding.oldPassword.getTextView().text = getString(R.string.nc_text_temporary_password)
        binding.emailSentDescription.isVisible = true
        binding.emailSentDescription.movementMethod = LinkMovementMethod.getInstance()
        showResendOption(email)
    }

    private fun showCountdownTimer(email: String) {
        val start = System.currentTimeMillis()
        lifecycleScope.launch {
            while ((System.currentTimeMillis() - start).milliseconds < 30.seconds) {
                val seconds = (System.currentTimeMillis() - start).milliseconds.inWholeSeconds
                binding.emailSentDescription.text = buildSpannedString {
                    if (isNewAccount) {
                        append(getString(R.string.nc_text_create_account_and_email_sent, email))
                    } else {
                        append(getString(R.string.nc_text_email_sent, email))
                    }
                    append(" ")
                    append(
                        "(${getString(R.string.nc_try_again_in)} ${
                            30 - seconds
                        }s)"
                    )
                    val startIndex = indexOf(email)
                    setSpan(
                        StyleSpan(BOLD),
                        startIndex,
                        startIndex + email.length,
                        SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        ForegroundColorSpan(getColor(R.color.nc_text_primary)),
                        startIndex,
                        startIndex + email.length,
                        SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
                delay(500.milliseconds)
            }
            showResendOption(email)
        }
    }

    private fun showResendOption(email: String) {
        val resendPasswordClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                widget.invalidate()
                viewModel.resendPassword()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColor(R.color.nc_text_primary)
                ds.isUnderlineText = true
            }
        }
        binding.emailSentDescription.text = buildSpannedString {
            if (isNewAccount) {
                append(getString(R.string.nc_text_create_account_and_email_sent, email))
            } else {
                append(getString(R.string.nc_text_email_sent, email))
            }
            inSpans(resendPasswordClickableSpan) {
                append(" ${getString(R.string.nc_text_resend_password)}")
            }
            append(" ")
            val startIndex = indexOf(email)
            setSpan(
                StyleSpan(BOLD),
                startIndex,
                startIndex + email.length,
                SPAN_INCLUSIVE_EXCLUSIVE
            )
            setSpan(
                ForegroundColorSpan(getColor(R.color.nc_text_primary)),
                startIndex,
                startIndex + email.length,
                SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun setupViews() {
        showToolbarBackButton()
        binding.oldPassword.makeMaskedInput()
        binding.newPassword.makeMaskedInput()
        binding.confirmPassword.makeMaskedInput()
        binding.changePassword.setOnClickListener { onChangePasswordClicked() }
        binding.newPassword.addTextChangedCallback {
            validateNewPassword(it)
        }
        binding.newPassword.getEditTextView().setOnFocusChangeListener { v, hasFocus ->
            binding.passwordErrorGroup.isVisible =
                hasFocus || binding.newPassword.getEditText().isNotEmpty()
        }
    }

    private fun showChangePasswordError(errorMessage: String) {
        hideLoading()
        NCToastMessage(this).showError(errorMessage)
    }

    private fun onChangePasswordClicked() {
        viewModel.handleChangePassword(
            oldPassword = binding.oldPassword.getEditText(),
            newPassword = binding.newPassword.getEditText(),
            confirmPassword = binding.confirmPassword.getEditText()
        )
    }

    private fun validateNewPassword(newPassword: String) {
        if (newPassword.isEmpty()) {
            listOf(
                binding.tvAtLeastOneNumber,
                binding.tvAtLeastOneUpperCase,
                binding.tvAtLeastOneSpecialChar,
                binding.tvAtLeast8Characters
            ).forEach {
                it.setTextColor(ContextCompat.getColor(this, R.color.nc_text_primary))
                it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_dot,
                    0,
                    0,
                    0
                )
            }
        } else {
            applyPasswordAttribute(
                binding.tvAtLeastOneNumber,
                newPassword.any { it.isDigit() }
            )
            applyPasswordAttribute(
                binding.tvAtLeastOneUpperCase,
                newPassword.any { it.isUpperCase() }
            )
            applyPasswordAttribute(
                binding.tvAtLeastOneSpecialChar,
                newPassword.any { it in SPECIAL_CHARACTERS }
            )
            applyPasswordAttribute(
                binding.tvAtLeast8Characters,
                newPassword.length >= 8
            )
        }
    }

    private fun applyPasswordAttribute(tv: TextView, isPass: Boolean) {
        if (isPass) {
            tv.setTextColor(ContextCompat.getColor(this, R.color.nc_slime_dark))
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_check_green,
                0,
                0,
                0
            )
        } else {
            tv.setTextColor(ContextCompat.getColor(this, R.color.nc_orange_color))
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_info_red,
                0,
                0,
                0
            )
        }
    }

    companion object {

        private const val IS_NEW_ACCOUNT = "is_new_account"

        fun start(activityContext: Context, isNewAccount: Boolean) {
            activityContext.startActivity(Intent(
                activityContext,
                ChangePasswordActivity::class.java
            )
                .apply {
                    putExtra(IS_NEW_ACCOUNT, isNewAccount)
                })
        }
    }
}


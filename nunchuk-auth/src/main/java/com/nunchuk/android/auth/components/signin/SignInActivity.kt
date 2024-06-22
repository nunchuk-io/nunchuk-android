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

package com.nunchuk.android.auth.components.signin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.enterxpub.EnterXPUBActivity
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.databinding.ActivitySigninBinding
import com.nunchuk.android.auth.util.getTextTrimmed
import com.nunchuk.android.auth.util.setUnderlineText
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.network.ApiErrorCode.NEW_DEVICE
import com.nunchuk.android.core.network.ErrorDetail
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInActivity : BaseActivity<ActivitySigninBinding>(), BottomSheetOptionListener {

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                finish()
            }
        }

    private val viewModel: SignInViewModel by viewModels()

    override fun initializeBinding() = ActivitySigninBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        setupViews()

        observeEvent()

        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_IS_DELETED, false)) {
            NCToastMessage(this).showMessage(getString(R.string.nc_account_deleted_message))
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is EmailRequiredEvent -> binding.email.setError(getString(R.string.nc_text_required))
                is EmailInvalidEvent -> binding.email.setError(getString(R.string.nc_text_email_invalid))
                is EmailValidEvent -> binding.email.hideError()
                is PasswordRequiredEvent -> binding.password.setError(getString(R.string.nc_text_required))
                is PasswordValidEvent -> binding.password.hideError()
                is SignInErrorEvent -> onSignInError(it.code, it.message.orEmpty(), it.errorDetail)
                is SignInSuccessEvent -> openMainScreen(it.token, it.deviceId)
                is ProcessingEvent -> showLoading(false)
                is CheckPrimaryKeyAccountEvent -> {
                    if (it.accounts.isNotEmpty()) {
                        navigator.openPrimaryKeyAccountScreen(this, it.accounts)
                    } else {
                        navigator.openPrimaryKeySignInIntroScreen(this)
                    }
                }
            }
        }
    }

    private fun onSignInError(code: Int?, message: String, errorDetail: ErrorDetail?) {
        hideLoading()
        when (code) {
            NEW_DEVICE -> {
                navigator.openVerifyNewDeviceScreen(
                    launcher = signInLauncher,
                    activityContext = this,
                    email = binding.email.getTextTrimmed(),
                    deviceId = errorDetail?.deviceID.orEmpty(),
                    loginHalfToken = errorDetail?.halfToken.orEmpty(),
                    staySignedIn = binding.staySignIn.isChecked
                )
            }
            else -> NCToastMessage(this).showError(message)
        }
    }

    private fun openMainScreen(token: String, deviceId: String) {
        hideLoading()
        if (NotificationUtils.areNotificationsEnabled(this).not()) {
            navigator.openTurnNotificationScreen(this)
        } else {
            navigator.openMainScreen(this, token, deviceId)
        }
        finish()
    }

    private fun setupViews() {
        binding.forgotPassword.setUnderlineText(getString(R.string.nc_text_forgot_password))

        binding.password.makeMaskedInput()

        binding.staySignIn.setOnCheckedChangeListener { _, checked ->
            viewModel.storeStaySignedIn(
                checked
            )
        }
        binding.signUp.setOnClickListener { onSignUpClick() }
        binding.signIn.setOnClickListener { onSignInClick() }
        binding.signInDigitalSignature.setOnClickListener { showSignInViaDigitalSignatureSheet() }
        binding.forgotPassword.setOnClickListener { onForgotPasswordClick() }
        binding.guestMode.setOnClickListener { onGuestModeClick() }
        binding.tvTermAndPolicy.linkify(
            getString(R.string.nc_hyperlink_text_term),
            TERM_URL
        )
        binding.tvTermAndPolicy.linkify(
            getString(R.string.nc_hyperlink_text_policy),
            PRIVACY_URL
        )
        clearInputFields()
    }

    private fun showSignInViaDigitalSignatureSheet() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_your_account_type),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_SIGN_IN_PAID_SUBSCRIPTION,
                    label = getString(R.string.nc_have_a_paid_subscription),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_SIGN_IN_PRIMARY_KEY,
                    label = getString(R.string.nc_have_a_primary_key_account),
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    override fun onDestroy() {
        super.onDestroy()
        clearInputFields()
    }

    private fun clearInputFields() {
        clearInputField(binding.email.getEditTextView())
        clearInputField(binding.password.getEditTextView())
    }

    private fun clearInputField(edittext: EditText) {
        edittext.clearComposingText()
        edittext.setText("")
    }

    private fun onSignUpClick() {
        navigator.openSignUpScreen(this)
    }

    private fun onForgotPasswordClick() {
        navigator.openForgotPasswordScreen(this)
    }

    private fun onSignInClick() {
        viewModel.handleSignIn(
            email = binding.email.getEditText().trim(),
            password = binding.password.getEditText(),
        )
    }

    private fun onGuestModeClick() {
        navigator.openGuestModeIntroScreen(this)
        finish()
    }

    companion object {
        private const val PRIVACY_URL = "https://www.nunchuk.io/privacy.html"
        private const val TERM_URL = "https://www.nunchuk.io/terms.html"
        private const val EXTRA_IS_DELETED = "EXTRA_IS_DELETED"
        fun start(activityContext: Context, isNeedNewTask: Boolean, isAccountDeleted: Boolean) {
            val intent = Intent(activityContext, SignInActivity::class.java).apply {
                if (isNeedNewTask) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                putExtra(EXTRA_IS_DELETED, isAccountDeleted)
            }
            activityContext.startActivity(intent)
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_SIGN_IN_PAID_SUBSCRIPTION -> EnterXPUBActivity.start(this)
            SheetOptionType.TYPE_SIGN_IN_PRIMARY_KEY -> navigator.openPrimaryKeySignInIntroScreen(this)
        }
    }

}
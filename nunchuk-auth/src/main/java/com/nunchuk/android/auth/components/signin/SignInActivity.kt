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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.enterxpub.EnterXPUBActivity
import com.nunchuk.android.auth.components.signin.SignInEvent.EmailInvalidEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.EmailRequiredEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.EmailValidEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.PasswordRequiredEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.PasswordValidEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.ProcessingEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.SignInErrorEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.SignInSuccessEvent
import com.nunchuk.android.auth.databinding.ActivitySigninBinding
import com.nunchuk.android.auth.util.getTextTrimmed
import com.nunchuk.android.auth.util.setUnderlineText
import com.nunchuk.android.core.account.SignInType
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.biometric.BiometricPromptManager
import com.nunchuk.android.core.network.ApiErrorCode.NEW_DEVICE
import com.nunchuk.android.core.network.ErrorDetail
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.core.util.showKeyboard
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInActivity : BaseActivity<ActivitySigninBinding>() {

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                finish()
            }
        }

    private val biometricPromptManager by lazy {
        BiometricPromptManager(activity = this)
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
        flowObserver(viewModel.event) {
            when (it) {
                is EmailRequiredEvent -> binding.email.setError(getString(R.string.nc_text_required))
                is EmailInvalidEvent -> binding.email.setError(getString(R.string.nc_text_email_invalid))
                is EmailValidEvent -> binding.email.hideError()
                is PasswordRequiredEvent -> binding.password.setError(getString(R.string.nc_text_required))
                is PasswordValidEvent -> binding.password.hideError()
                is SignInErrorEvent -> onSignInError(it.code, it.message.orEmpty(), it.errorDetail)
                is SignInSuccessEvent -> viewModel.checkClearBiometric()

                is ProcessingEvent -> showOrHideLoading(it.isLoading)

                is SignInEvent.RequireChangePassword -> navigator.openChangePasswordScreen(
                    activityContext = this,
                    isNewAccount = it.isNew
                )

                SignInEvent.NameRequiredEvent -> binding.name.setError(getString(R.string.nc_text_required))
                SignInEvent.NameValidEvent -> binding.name.hideError()
                SignInEvent.OpenMainScreen -> {
                    openMainScreen()
                    if (viewModel.walletPinEnable.value) {
                        navigator.openUnlockPinScreen(this)
                    }
                }
            }
        }
        flowObserver(viewModel.state) {
            if (it.email.isNotEmpty()) {
                binding.email.getEditTextView().setText(it.email)
            }
            binding.signInPrimary.isVisible =
                it.type == SignInType.GUEST || it.accounts.isNotEmpty()
            initUiWithStage(it.type, it.isSubscriberUser)

            when (it.type) {
                SignInType.EMAIL, SignInType.GUEST -> {
                    binding.email.getEditTextView().showKeyboard()
                }

                SignInType.PASSWORD -> {
                    binding.password.getEditTextView().showKeyboard()
                }

                SignInType.NAME -> {
                    binding.name.getEditTextView().showKeyboard()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                biometricPromptManager.promptResults.collect {
                    when (it) {
                        is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> viewModel.onBiometricSignIn()
                        is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                            NCInfoDialog(
                                activity = this@SignInActivity
                            ).showDialog(
                                message = getString(R.string.nc_biometric_is_not_enable_this_device),
                                btnYes = getString(R.string.nc_try_again),
                                btnInfo = getString(R.string.nc_sign_in_using_password),
                            )
                        }

                        is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {

                        }

                        is BiometricPromptManager.BiometricResult.AuthenticationError -> {

                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun initUiWithStage(stage: SignInType, isSubscriberUser: Boolean) {
        binding.password.isVisible = stage == SignInType.PASSWORD
        binding.forgotPassword.isVisible = stage == SignInType.PASSWORD
        binding.staySignIn.isVisible = stage == SignInType.PASSWORD
        binding.signInDigitalSignature.isVisible = isSubscriberUser && stage == SignInType.PASSWORD
        binding.email.isEnabled = stage == SignInType.EMAIL || stage == SignInType.GUEST

        binding.name.isVisible = stage == SignInType.NAME

        binding.guestMode.isVisible = stage == SignInType.EMAIL
        binding.tvTermAndPolicy.isVisible = stage == SignInType.EMAIL

        if (stage == SignInType.PASSWORD) {
            binding.signIn.setText(R.string.nc_text_sign_in)
        } else {
            binding.signIn.setText(R.string.nc_text_continue)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(stage != SignInType.EMAIL)
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

    private fun openMainScreen() {
        hideLoading()
        if (NotificationUtils.areNotificationsEnabled(this).not()) {
            navigator.openTurnNotificationScreen(this)
        } else {
            navigator.openMainScreen(
                activityContext = this,
                isClearTask = true,
            )
        }
        finish()
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""
        binding.forgotPassword.setUnderlineText(getString(R.string.nc_text_forgot_password))

        binding.password.makeMaskedInput()

        binding.staySignIn.setOnCheckedChangeListener { _, checked ->
            viewModel.storeStaySignedIn(
                checked
            )
        }
        binding.signIn.setOnClickListener { onSignInClick() }
        binding.signInDigitalSignature.setOnClickListener { EnterXPUBActivity.start(this) }
        binding.forgotPassword.setOnClickListener { onForgotPasswordClick() }
        binding.guestMode.setOnClickListener { onGuestModeClick() }
        binding.signInPrimary.setOnClickListener {
            val accounts = viewModel.state.value.accounts
            if (accounts.isNotEmpty()) {
                navigator.openPrimaryKeyAccountScreen(this, ArrayList(accounts))
            } else {
                navigator.openPrimaryKeySignInIntroScreen(this)
            }
        }
        binding.tvTermAndPolicy.linkify(
            getString(R.string.nc_hyperlink_text_term),
            TERM_URL
        )
        binding.tvTermAndPolicy.linkify(
            getString(R.string.nc_hyperlink_text_policy),
            PRIVACY_URL
        )
        if (viewModel.type == SignInType.GUEST) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_close)
        binding.toolbar.setNavigationOnClickListener {
            if (viewModel.state.value.type == viewModel.type) {
                finish()
            } else {
                viewModel.setType(
                    intent.serializable<SignInType>(SignInViewModel.EXTRA_TYPE) ?: SignInType.EMAIL
                )
                clearInputFields()
            }
        }
        binding.fingerprint.setOnClickListener {
            if (viewModel.biometricConfig.value.enabled) {
                biometricPromptManager.showBiometricPrompt()
            } else {
                NCInfoDialog(this).showDialog(
                    message = getString(R.string.nc_biometric_login_not_setup),
                )
            }
        }
        clearInputFields()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearInputFields()
    }

    private fun clearInputFields() {
        clearInputField(binding.email.getEditTextView())
        clearInputField(binding.password.getEditTextView())
    }

    @SuppressLint("SetTextI18n")
    private fun clearInputField(edittext: EditText) {
        edittext.clearComposingText()
        edittext.setText("")
    }

    private fun onForgotPasswordClick() {
        navigator.openForgotPasswordScreen(this)
    }

    private fun onSignInClick() {
        viewModel.onContinueClicked(
            email = binding.email.getEditText().trim(),
            password = binding.password.getEditText(),
            name = binding.name.getEditText().trim()
        )
    }

    private fun onGuestModeClick() {
        viewModel.initGuestModeNunchuk()
    }

    companion object {
        private const val PRIVACY_URL = "https://www.nunchuk.io/privacy.html"
        private const val TERM_URL = "https://www.nunchuk.io/terms.html"
        private const val EXTRA_IS_DELETED = "EXTRA_IS_DELETED"
        fun start(
            activityContext: Context,
            isNeedNewTask: Boolean,
            isAccountDeleted: Boolean,
            type: SignInType
        ) {
            val intent = Intent(activityContext, SignInActivity::class.java).apply {
                putExtra(SignInViewModel.EXTRA_SIGN_OUT, isNeedNewTask)
                if (isNeedNewTask) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                putExtra(EXTRA_IS_DELETED, isAccountDeleted)
                putExtra(SignInViewModel.EXTRA_TYPE, type)
            }
            activityContext.startActivity(intent)
        }
    }
}
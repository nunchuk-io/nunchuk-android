package com.nunchuk.android.auth.components.signin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.databinding.ActivitySigninBinding
import com.nunchuk.android.auth.util.getTextTrimmed
import com.nunchuk.android.auth.util.setUnderlineText
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.network.ApiErrorCode.NEW_DEVICE
import com.nunchuk.android.core.network.ErrorDetail
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInActivity : BaseActivity<ActivitySigninBinding>() {
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                is SignInSuccessEvent -> {
                    SignInModeHolder.currentMode = SignInMode.NORMAL
                    openMainScreen(it.token, it.deviceId)
                }
                is ProcessingEvent -> showLoading(false)
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
        finish()
        navigator.openMainScreen(this, token, deviceId)
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

}
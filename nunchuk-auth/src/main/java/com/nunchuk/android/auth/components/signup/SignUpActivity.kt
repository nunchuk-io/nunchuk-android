package com.nunchuk.android.auth.components.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.signup.SignUpEvent.*
import com.nunchuk.android.auth.databinding.ActivitySignupBinding
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.linkify
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showToast
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
        binding.signUp.setOnClickListener { onRegisterClicked() }
        binding.signIn.setOnClickListener { openLoginScreen() }

        binding.tvTermAndPolicy.linkify(getString(R.string.nc_hyperlink_text_term), TERM_URL)
        binding.tvTermAndPolicy.linkify(getString(R.string.nc_hyperlink_text_policy), PRIVACY_URL)
    }

    private fun onRegisterClicked() {
        viewModel.handleRegister(binding.name.getEditText().trim(), binding.email.getEditText().trim())
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: SignUpEvent) {
        when (event) {
            NameRequiredEvent -> showNameError(R.string.nc_text_required)
            NameInvalidEvent -> showNameError(R.string.nc_text_name_invalid)
            EmailInvalidEvent -> showEmailError(R.string.nc_text_email_invalid)
            EmailRequiredEvent -> showEmailError(R.string.nc_text_required)
            NameValidEvent -> hideNameError()
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

    private fun hideNameError() {
        binding.name.hideError()
    }

    private fun hideEmailError() {
        binding.email.hideError()
    }

    private fun showNameError(errorMessageId: Int) {
        binding.name.setError(getString(errorMessageId))
    }

    private fun showEmailError(errorMessageId: Int) {
        binding.email.setError(getString(errorMessageId))
    }

    private fun openLoginScreen() {
        finish()
        navigator.openSignInScreen(this)
    }

    private fun openChangePasswordScreen() {
        hideLoading()
        finish()
        navigator.openChangePasswordScreen(this)
    }

    companion object {
        private const val PRIVACY_URL = "https://www.nunchuk.io/privacy.html"
        private const val TERM_URL = "https://www.nunchuk.io/terms.html"
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignUpActivity::class.java))
        }
    }
}


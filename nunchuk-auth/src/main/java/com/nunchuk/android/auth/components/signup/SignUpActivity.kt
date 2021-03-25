package com.nunchuk.android.auth.components.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.changepass.ChangePasswordActivity
import com.nunchuk.android.auth.components.signin.SignInActivity
import com.nunchuk.android.auth.components.signup.SignUpEvent.*
import com.nunchuk.android.auth.databinding.ActivitySignupBinding
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.auth.util.showToast
import com.nunchuk.android.widget.util.SimpleTextWatcher
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

class SignUpActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: SignUpViewModel by lazy {
        ViewModelProviders.of(this, factory).get(SignUpViewModel::class.java)
    }

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()
    }

    private fun setupViews() {
        binding.email.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.validateEmail("$s")
            }
        })
        binding.name.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.validateName("$s")
            }
        })
        binding.signUp.setOnClickListener { onRegisterClicked() }
        binding.signIn.setOnClickListener { openLoginScreen() }
    }

    private fun onRegisterClicked() {
        viewModel.handleRegister(binding.name.getEditText(), binding.email.getEditText())
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                NameRequiredEvent -> showNameError(R.string.nc_text_required)
                NameInvalidEvent -> showNameError(R.string.nc_text_name_invalid)
                EmailInvalidEvent -> showEmailError(R.string.nc_text_email_invalid)
                EmailRequiredEvent -> showEmailError(R.string.nc_text_required)
                NameValidEvent -> hideNameError()
                EmailValidEvent -> hideEmailError()
                LoadingEvent -> showLoading()
                is SignUpSuccessEvent -> openChangePasswordScreen()
                is SignUpErrorEvent -> showToast(it.errorMessage.orUnknownError())
            }
        }
    }

    private fun showLoading() {
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
        SignInActivity.start(this)
    }

    private fun openChangePasswordScreen() {
        finish()
        ChangePasswordActivity.start(this)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignUpActivity::class.java))
        }
    }
}


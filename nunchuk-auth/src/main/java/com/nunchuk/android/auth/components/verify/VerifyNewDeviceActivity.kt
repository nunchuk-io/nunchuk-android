package com.nunchuk.android.auth.components.verify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.*
import com.nunchuk.android.auth.databinding.ActivityVerifyNewDeviceBinding
import com.nunchuk.android.auth.util.getTextTrimmed
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
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
                    SignInModeHolder.currentMode = SignInMode.NORMAL
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
        finish()
        navigator.openMainScreen(this, token, deviceId)
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

        fun start(activityContext: Context, email: String, loginHalfToken: String, deviceId: String, staySignedIn: Boolean) {
            val intent = Intent(activityContext, VerifyNewDeviceActivity::class.java).apply {
                putExtra(EXTRAS_EMAIL, email)
                putExtra(EXTRAS_LOGIN_HALF_TOKEN, loginHalfToken)
                putExtra(EXTRAS_DEVICE_ID, deviceId)
                putExtra(EXTRAS_STAY_SIGNED_IN, staySignedIn)
            }
            activityContext.startActivity(intent)
        }
    }

}
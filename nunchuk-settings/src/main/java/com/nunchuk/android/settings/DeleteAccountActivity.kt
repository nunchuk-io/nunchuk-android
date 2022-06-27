package com.nunchuk.android.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.getHtmlText
import com.nunchuk.android.settings.DeleteAccountEvent.*
import com.nunchuk.android.settings.databinding.ActivityDeleteAccountBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteAccountActivity : BaseActivity<ActivityDeleteAccountBinding>() {

    private val viewModel: DeleteAccountViewModel by viewModels()

    override fun initializeBinding() = ActivityDeleteAccountBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: DeleteAccountState) {
        val email = state.email
        if (email.isNotEmpty()) {
            binding.information.text = getHtmlText(R.string.nc_account_delete_account_information, email)
        }
    }

    private fun handleEvent(event: DeleteAccountEvent) {
        when (event) {
            Loading -> showLoading()
            is ConfirmDeleteError -> showConfirmError(event.message)
            ConfirmDeleteSuccess -> handleConfirmSuccess()
        }
    }

    private fun handleConfirmSuccess() {
        hideLoading()
        NCToastMessage(this).showMessage(getString(R.string.nc_account_deleted_message))
        binding.root.postDelayed(::gotoLogin, DELAY)

    }

    private fun gotoLogin() {
        navigator.openSignInScreen(this)
    }

    private fun showConfirmError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.confirmationCode.addTextChangedCallback {
            if (it.isNotEmpty()) {
                binding.confirmationCode.hideError()
            }
        }
        binding.confirmButton.setOnClickListener {
            val confirmationCode = binding.confirmationCode.getEditText()
            if (confirmationCode.isEmpty()) {
                binding.confirmationCode.setError(getString(R.string.nc_text_required))
            } else {
                viewModel.sendConfirmDeleteAccount(confirmationCode)
            }
        }
    }

    companion object {
        private const val DELAY = 3000L
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, DeleteAccountActivity::class.java))
        }
    }
}

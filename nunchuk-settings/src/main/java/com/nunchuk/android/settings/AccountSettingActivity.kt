package com.nunchuk.android.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.settings.AccountSettingEvent.*
import com.nunchuk.android.settings.databinding.ActivityAccountSettingBinding
import com.nunchuk.android.widget.NCDeleteConfirmationDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AccountSettingActivity : BaseActivity<ActivityAccountSettingBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: AccountSettingViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityAccountSettingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: AccountSettingEvent) {
        when (event) {
            Loading -> showLoading()
            is RequestDeleteError -> showRequestError(event.message)
            RequestDeleteSuccess -> handleRequestSuccess()
        }
    }

    private fun handleRequestSuccess() {
        hideLoading()
        DeleteAccountActivity.start(this)
    }

    private fun showRequestError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.devices.setOnClickListener { showComingSoonText() }
        binding.password.setOnClickListener { navigator.openChangePasswordScreen(this) }
        binding.delete.setOnClickListener { showDeleteAccountConfirmation() }
    }

    private fun showDeleteAccountConfirmation() {
        NCDeleteConfirmationDialog(this).showDialog(onConfirmed = {
            if (it == CONFIRMATION_TEXT) {
                viewModel.sendRequestDeleteAccount()
            }
        })
    }

    companion object {

        private const val CONFIRMATION_TEXT = "DELETE"

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AccountSettingActivity::class.java))
        }

    }
}
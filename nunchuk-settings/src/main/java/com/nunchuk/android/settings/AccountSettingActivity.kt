package com.nunchuk.android.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isPrimaryKey
import com.nunchuk.android.settings.AccountSettingEvent.*
import com.nunchuk.android.settings.databinding.ActivityAccountSettingBinding
import com.nunchuk.android.widget.NCDeleteConfirmationDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountSettingActivity : BaseActivity<ActivityAccountSettingBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    @Inject
    lateinit var primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder

    private val viewModel: AccountSettingViewModel by viewModels()

    override fun initializeBinding() = ActivityAccountSettingBinding.inflate(layoutInflater)

    private val isSignInPrimaryKey by lazy { signInModeHolder.getCurrentMode().isPrimaryKey() }

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
            DeletePrimaryKeySuccess -> {
                hideLoading()
                navigator.openSignInScreen(this, isAccountDeleted = true)
            }
            is CheckNeedPassphraseSent -> {
                hideLoading()
                showEnterPassphraseDialog(event.isNeeded)
            }
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
        binding.password.isVisible = isSignInPrimaryKey.not()
        binding.replacePrimaryKey.isVisible = isSignInPrimaryKey

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.devices.setOnClickListener { navigator.openUserDevicesScreen(this) }
        binding.enableSync.setOnClickListener {
            navigator.openSyncSettingScreen(this)
        }
        binding.password.setOnClickListener { navigator.openChangePasswordScreen(this) }
        binding.delete.setOnClickListener {
            if (isSignInPrimaryKey) {
                if (accountManager.getPrimaryKeyInfo()?.xfp.isNullOrEmpty()) return@setOnClickListener
                showDeletePrimaryKeyConfirmation()
            } else {
                showDeleteAccountConfirmation()
            }
        }
        binding.replacePrimaryKey.setOnClickListener {
            if (accountManager.getPrimaryKeyInfo()?.xfp.isNullOrEmpty()) return@setOnClickListener
            navigator.openPrimaryKeyReplaceIntroScreen(
                this
            )
        }
    }

    private fun showDeletePrimaryKeyConfirmation() {
        NCDeleteConfirmationDialog(this).showDialog(
            message = getString(R.string.nc_delete_primary_key_desc),
            onConfirmed = {
                if (it.trim() == CONFIRMATION_TEXT) {
                    viewModel.checkNeedPassphraseSent()
                }
            })
    }

    private fun showDeleteAccountConfirmation() {
        NCDeleteConfirmationDialog(this).showDialog(onConfirmed = {
            if (it.trim() == CONFIRMATION_TEXT) {
                viewModel.sendRequestDeleteAccount()
            }
        })
    }

    private fun showEnterPassphraseDialog(isNeeded: Boolean) {
        if (isNeeded) {
            NCInputDialog(this).showDialog(
                title = getString(R.string.nc_transaction_enter_passphrase),
                onConfirmed = {
                    viewModel.deletePrimaryKey(it)
                }
            )
        } else {
            viewModel.deletePrimaryKey("")
        }
    }

    companion object {

        private const val CONFIRMATION_TEXT = "DELETE"

        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    AccountSettingActivity::class.java
                )
            )
        }

    }
}
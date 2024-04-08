package com.nunchuk.android.core.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.viewmodel.WalletSecuritySettingsEvent
import com.nunchuk.android.core.viewmodel.WalletSecuritySettingsViewModel
import com.nunchuk.android.widget.NCInputDialog

abstract class BaseAuthenticationFragment<out Binding : ViewBinding> : BaseFragment<Binding>() {

    private val walletSecuritySettingsViewModel by viewModels<WalletSecuritySettingsViewModel>()

    abstract fun openWalletDetailsScreen(walletId: String)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(walletSecuritySettingsViewModel.event) {
            when (it) {
                is WalletSecuritySettingsEvent.InvalidPin -> showError(requireContext().getString(R.string.nc_incorrect_current_pin))
                is WalletSecuritySettingsEvent.ShowError -> showError(it.throwable?.message.orUnknownError())
                is WalletSecuritySettingsEvent.OpenWalletDetailsScreen -> openWalletDetailsScreen(it.walletId)
                is WalletSecuritySettingsEvent.DoNextAction -> actionAfterCheckingPasswordOrPassphrase(it.walletId)
            }
        }
    }

    fun checkWalletSecurity(walletId: String) {
        if (walletSecuritySettingsViewModel.isWalletPasswordEnabled()) {
            enterPasswordDialog(walletId)
        } else if (walletSecuritySettingsViewModel.isWalletPassphraseEnabled()) {
            enterPassphraseDialog(walletId)
        } else if (walletSecuritySettingsViewModel.isWalletPinEnabled()) {
            showInputPinDialog(walletId)
        } else {
            openWalletDetailsScreen(walletId)
        }
    }

    private fun showInputPinDialog(walletId: String) {
        NCInputDialog(requireContext()).showDialog(
            title = requireContext().getString(R.string.nc_enter_your_pin),
            onConfirmed = {
                walletSecuritySettingsViewModel.checkWalletPin(it, walletId)
            }
        )
    }

    private fun actionAfterCheckingPasswordOrPassphrase(walletId: String) {
        if (walletSecuritySettingsViewModel.isWalletPinEnabled()) {
            showInputPinDialog(walletId)
        } else {
            openWalletDetailsScreen(walletId)
        }
    }

    private fun enterPasswordDialog(walletId: String) {
        NCInputDialog(requireContext()).showDialog(
            title = requireContext().getString(R.string.nc_re_enter_your_password),
            descMessage = requireContext().getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                walletSecuritySettingsViewModel.confirmPassword(it, walletId)
            }
        )
    }

    private fun enterPassphraseDialog(walletId: String) {
        NCInputDialog(requireContext()).showDialog(
            title = requireContext().getString(R.string.nc_re_enter_your_passphrase),
            descMessage = requireContext().getString(R.string.nc_re_enter_your_passphrase_dialog_desc),
            onConfirmed = {
                walletSecuritySettingsViewModel.confirmPassphrase(it, walletId)
            }
        )
    }
}
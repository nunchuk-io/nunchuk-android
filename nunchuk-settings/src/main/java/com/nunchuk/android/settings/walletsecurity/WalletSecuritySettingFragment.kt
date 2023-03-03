package com.nunchuk.android.settings.walletsecurity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.FragmentWalletSecuritySettingBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletSecuritySettingFragment : BaseFragment<FragmentWalletSecuritySettingBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    private val viewModel: WalletSecuritySettingViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWalletSecuritySettingBinding {
        return FragmentWalletSecuritySettingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: WalletSecuritySettingState) {
        binding.hideWalletDetailOption.setOptionChecked(state.walletSecuritySetting.hideWalletDetail)
        binding.passwordOption.setOptionChecked(state.walletSecuritySetting.protectWalletPassword)
        binding.pinOption.setOptionChecked(state.walletSecuritySetting.protectWalletPin)
        binding.passwordOption.isVisible = signInModeHolder.getCurrentMode() != SignInMode.GUEST_MODE
        binding.pinOptionCreateButton.isVisible =
            state.walletPin.isBlank() && state.walletSecuritySetting.protectWalletPin
        binding.pinOptionChangeButton.isVisible =
            state.walletPin.isBlank().not() && state.walletSecuritySetting.protectWalletPin
    }

    private fun handleEvent(event: WalletSecuritySettingEvent) {
        when (event) {
            is WalletSecuritySettingEvent.Error -> NCToastMessage(requireActivity()).showError(
                message = event.message
            )
            is WalletSecuritySettingEvent.Loading -> showOrHideLoading(loading = event.loading)
            WalletSecuritySettingEvent.UpdateConfigSuccess -> {

            }
            is WalletSecuritySettingEvent.CheckWalletPin -> {
                if (event.match.not()) {
                    NCToastMessage(requireActivity()).showError(
                        message = getString(R.string.nc_incorrect_pin)
                    )
                }
            }
            WalletSecuritySettingEvent.None -> {}
        }
        viewModel.clearEvent()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
        binding.hideWalletDetailOption.setOptionChangeListener {
            viewModel.updateHideWalletDetail(it)
        }
        binding.passwordOption.setOptionChangeListener {
            if (it.not()) {
                enterPasswordDialog()
            } else {
                viewModel.updateProtectWalletPassword(it)
            }
        }
        binding.pinOption.setOptionChangeListener {
            if (it.not() && viewModel.getWalletPin().isNotBlank()) {
                showInputPinDialog()
            } else {
                viewModel.updateProtectWalletPin(it)
            }
        }
        binding.pinOptionCreateButton.setOnDebounceClickListener {
            findNavController().navigate(
                WalletSecuritySettingFragmentDirections.actionWalletSecuritySettingFragmentToWalletSecurityCreatePinFragment(
                    currentPin = viewModel.getWalletPin()
                )
            )
        }
        binding.pinOptionChangeButton.setOnDebounceClickListener {
            findNavController().navigate(
                WalletSecuritySettingFragmentDirections.actionWalletSecuritySettingFragmentToWalletSecurityCreatePinFragment(
                    currentPin = viewModel.getWalletPin()
                )
            )
        }
    }

    private fun showInputPinDialog() {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_pin),
            onCanceled = {
                viewModel.updateProtectWalletPin(true)
            },
            onConfirmed = {
                viewModel.checkWalletPin(it)
            }
        )
    }

    private fun enterPasswordDialog() {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onCanceled = {
                viewModel.updateProtectWalletPassword(true)
            },
            onConfirmed = {
                viewModel.confirmPassword(it)
            }
        )
    }
}
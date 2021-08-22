package com.nunchuk.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.settings.AccountEvent.SignOutEvent
import com.nunchuk.android.settings.databinding.FragmentAccountBinding

internal class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private val viewModel: AccountViewModel by activityViewModels { factory }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAccountBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: AccountState) {
        binding.appVersion.text = state.appVersion
        binding.avatarHolder.text = state.account.name.shorten()
        binding.name.text = state.account.name
        binding.email.text = state.account.email
        binding.edit.setOnClickListener { editName() }
        binding.takePicture.setOnClickListener { changeAvatar() }
        binding.unit.setOnClickListener { changeUnitSetting() }
        binding.network.setOnClickListener { changeNetworkSetting() }
        binding.password.setOnClickListener { openChangePasswordScreen() }
        binding.devices.setOnClickListener { openLoggedInDevicesScreen() }
        binding.about.setOnClickListener { openAboutScreen() }
    }

    private fun openAboutScreen() {
        showComingSoonText()
    }

    private fun openLoggedInDevicesScreen() {
        showComingSoonText()
    }

    private fun openChangePasswordScreen() {
        navigator.openChangePasswordScreen(requireActivity())
    }

    private fun changeNetworkSetting() {
        showComingSoonText()
    }

    private fun changeUnitSetting() {
        showComingSoonText()
    }

    private fun changeAvatar() {
        showComingSoonText()
    }

    private fun editName() {
        showComingSoonText()
    }

    private fun handleEvent(event: AccountEvent) {
        if (event == SignOutEvent) {
            val activity = requireActivity()
            navigator.openSignInScreen(activity)
            activity.finish()
        }
    }

    private fun showComingSoonText() {
        requireActivity().showToast("Coming soon")
    }

    private fun setupViews() {
        binding.btnSignOut.setOnClickListener { viewModel.handleSignOutEvent() }
    }

}
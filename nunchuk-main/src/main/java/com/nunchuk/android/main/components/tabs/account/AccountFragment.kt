package com.nunchuk.android.main.components.tabs.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.main.databinding.FragmentAccountBinding

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
        binding.email.text = state.email
    }

    private fun handleEvent(event: AccountEvent) {
        if (event == AccountEvent.SignOutEvent) {
            val activity = requireActivity()
            navigator.openSignInScreen(activity)
            activity.finish()
        }
    }

    private fun setupViews() {
        binding.btnSignOut.setOnClickListener { viewModel.handleSignOutEvent() }
    }

}
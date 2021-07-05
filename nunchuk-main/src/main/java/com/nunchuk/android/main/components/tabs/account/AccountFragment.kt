package com.nunchuk.android.main.components.tabs.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.main.databinding.FragmentAccountBinding
import javax.inject.Inject

internal class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

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
    }

    private fun handleEvent(event: AccountEvent) {
        if (event == AccountEvent.SignOutEvent) {
            val activity = requireActivity()
            navigator.openSignInScreen(activity)
            activity.finish()
            SessionHolder.currentSession?.close()
        }
    }

    private fun setupViews() {
        binding.btnSignOut.setOnClickListener { viewModel.handleSignOutEvent() }
    }

}
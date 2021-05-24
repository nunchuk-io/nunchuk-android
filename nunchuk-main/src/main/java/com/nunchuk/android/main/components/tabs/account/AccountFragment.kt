package com.nunchuk.android.main.components.tabs.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.main.databinding.FragmentAccountBinding
import javax.inject.Inject

internal class AccountFragment : BaseFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AccountViewModel::class.java)
    }

    private var _binding: FragmentAccountBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

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
        }
    }

    private fun setupViews() {
        binding.btnSignOut.setOnClickListener { viewModel.handleSignOutEvent() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
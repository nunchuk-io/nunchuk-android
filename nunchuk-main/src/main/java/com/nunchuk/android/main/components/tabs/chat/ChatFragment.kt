package com.nunchuk.android.main.components.tabs.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.main.databinding.FragmentChatBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ChatFragment : BaseFragment<FragmentChatBinding>() {

    private lateinit var pagerAdapter: ChatFragmentPagerAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentChatBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        registerEvents()
    }

    private fun registerEvents() {
        binding.signIn.setOnClickListener {
            navigator.openSignInScreen(requireActivity(), isNeedNewTask = false)
        }
        binding.signUp.setOnClickListener {
            navigator.openSignUpScreen(requireActivity())
        }
    }

    private fun setupViews() {
        binding.containerNotSignIn.isVisible = SignInModeHolder.currentMode.isGuestMode()
        binding.pagers.isVisible = binding.containerNotSignIn.isGone
        val tabs = binding.tabs

        if (binding.pagers.isGone) {
            ChatFragmentTab.values().forEach { tab ->
                tabs.addTab(tabs.newTab().setText(getString(tab.labelId)))
            }
        } else {
            val pagers = binding.pagers

            pagerAdapter = ChatFragmentPagerAdapter(requireContext(), fragmentManager = childFragmentManager)
            binding.pagers.offscreenPageLimit = ChatFragmentTab.values().size
            val position = pagers.currentItem
            pagers.adapter = pagerAdapter
            tabs.setupWithViewPager(pagers)
            pagers.currentItem = position
        }
    }

}
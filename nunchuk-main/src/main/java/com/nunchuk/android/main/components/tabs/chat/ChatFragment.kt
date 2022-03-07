package com.nunchuk.android.main.components.tabs.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.main.databinding.FragmentChatBinding

internal class ChatFragment : BaseFragment<FragmentChatBinding>() {

    private lateinit var pagerAdapter: ChatFragmentPagerAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentChatBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!SignInModeHolder.currentMode.isGuestMode()) {
            setupViews()
        } else {
            navigator.openGuestModeMessageIntroScreen(requireActivity())
        }
    }

    private fun setupViews() {
        val pagers = binding.pagers
        val tabs = binding.tabs

        pagerAdapter = ChatFragmentPagerAdapter(requireContext(), fragmentManager = parentFragmentManager)
        binding.pagers.offscreenPageLimit = ChatFragmentTab.values().size
        ChatFragmentTab.values().forEach {
            tabs.addTab(tabs.newTab().setText(it.name))
        }
        val position = pagers.currentItem
        pagers.adapter = pagerAdapter
        tabs.setupWithViewPager(pagers)
        pagers.currentItem = position
    }

}
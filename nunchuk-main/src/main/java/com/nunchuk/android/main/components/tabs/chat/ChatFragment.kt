/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatFragment : BaseFragment<FragmentChatBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

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
        binding.containerNotSignIn.isVisible = signInModeHolder.getCurrentMode().isGuestMode()
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
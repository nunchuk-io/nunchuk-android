/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.main.databinding.FragmentChatBinding
import com.nunchuk.android.messages.components.list.RoomMessage
import com.nunchuk.android.messages.components.list.RoomsState
import com.nunchuk.android.messages.components.list.RoomsViewModel
import com.nunchuk.android.messages.components.list.shouldShow
import com.nunchuk.android.utils.consumeEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatFragment : BaseFragment<FragmentChatBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    private val viewModel: RoomsViewModel by activityViewModels()

    private lateinit var pagerAdapter: ChatFragmentPagerAdapter

    private var isHasRoomChat = false

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

        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: RoomsState) {
        val visibleRooms = state.rooms.filter {
            it is RoomMessage.GroupWalletRoom ||
                    ((it as? RoomMessage.MatrixRoom)?.data?.shouldShow() == true)
        }
        isHasRoomChat = visibleRooms.isNotEmpty()
        binding.containerNotSignIn.isVisible = if (binding.pagers.currentItem == ChatFragmentTab.MESSAGES.position) {
            isHasRoomChat.not()
        } else {
            signInModeHolder.getCurrentMode().isGuestMode()
        }
    }

    private fun setupViews() {
        binding.toolbar.consumeEdgeToEdge()
        binding.pagers.isVisible = binding.containerNotSignIn.isGone
        val tabs = binding.tabs

        if (binding.pagers.isGone) {
            ChatFragmentTab.entries.forEach { tab ->
                tabs.addTab(tabs.newTab().setText(getString(tab.labelId)))
            }
        } else {
            val pagers = binding.pagers

            pagerAdapter = ChatFragmentPagerAdapter(requireContext(), fragmentManager = childFragmentManager)
            binding.pagers.offscreenPageLimit = ChatFragmentTab.entries.size
            binding.pagers.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    Timber.tag("group-wallet-chat").e("onPageSelected - position: $position")
                    when (position) {
                        ChatFragmentTab.MESSAGES.position -> {
                            binding.containerNotSignIn.isVisible = isHasRoomChat.not()
                        }
                        ChatFragmentTab.CONTACTS.position -> {
                            binding.containerNotSignIn.isVisible = signInModeHolder.getCurrentMode().isGuestMode()
                        }
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                }
            })
            val position = pagers.currentItem
            pagers.adapter = pagerAdapter
            tabs.setupWithViewPager(pagers)
            pagers.currentItem = position
        }
    }

}
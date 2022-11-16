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

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.nunchuk.android.contact.components.contacts.ContactsFragment
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.chat.ChatFragmentTab.CONTACTS
import com.nunchuk.android.main.components.tabs.chat.ChatFragmentTab.MESSAGES
import com.nunchuk.android.messages.components.list.RoomsFragment

class ChatFragmentPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            MESSAGES.position -> RoomsFragment.newInstance()
            CONTACTS.position -> ContactsFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun getCount() = ChatFragmentTab.values().size

    override fun getPageTitle(position: Int) = when (position) {
        MESSAGES.position -> context.getString(R.string.nc_title_message)
        CONTACTS.position -> context.getString(R.string.nc_title_contacts)
        else -> ""
    }

}
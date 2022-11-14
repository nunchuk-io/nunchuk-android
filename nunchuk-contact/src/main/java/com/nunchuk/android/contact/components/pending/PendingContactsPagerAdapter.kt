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

package com.nunchuk.android.contact.components.pending

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.components.pending.PendingContactTab.RECEIVED
import com.nunchuk.android.contact.components.pending.PendingContactTab.SENT
import com.nunchuk.android.contact.components.pending.receive.ReceivedFragment
import com.nunchuk.android.contact.components.pending.sent.SentFragment

class PendingContactsPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            RECEIVED.position -> ReceivedFragment.newInstance()
            SENT.position -> SentFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun getCount() = PendingContactTab.values().size

    override fun getPageTitle(position: Int) = when (position) {
        RECEIVED.position -> context.getString(R.string.nc_contact_received)
        SENT.position -> context.getString(R.string.nc_contact_sent)
        else -> ""
    }

}
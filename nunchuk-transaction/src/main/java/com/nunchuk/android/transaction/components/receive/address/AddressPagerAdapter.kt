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

package com.nunchuk.android.transaction.components.receive.address

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.receive.address.AddressTab.UNUSED
import com.nunchuk.android.transaction.components.receive.address.AddressTab.USED

@Suppress("DEPRECATION")
class AddressPagerAdapter(
    private val context: Context,
    private val fragmentFactory: AddressFragmentFactory,
    fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            UNUSED.position -> fragmentFactory.createUnusedAddressFragment()
            USED.position -> fragmentFactory.createUsedAddressFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun getCount() = AddressTab.values().size

    override fun getPageTitle(position: Int) = when (position) {
        UNUSED.position -> context.getString(R.string.nc_transaction_unused)
        USED.position -> context.getString(R.string.nc_transaction_used)
        else -> ""
    }

}
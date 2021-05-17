package com.nunchuk.android.transaction.receive.address

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.receive.address.AddressTab.*

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
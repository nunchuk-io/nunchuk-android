package com.nunchuk.android.transaction.receive.address

import androidx.fragment.app.Fragment
import com.nunchuk.android.transaction.receive.address.unused.UnusedAddressFragment
import com.nunchuk.android.transaction.receive.address.used.UsedAddressFragment

class AddressFragmentFactory(private val walletId: String) {

    fun createUnusedAddressFragment(): Fragment = UnusedAddressFragment.newInstance(walletId)

    fun createUsedAddressFragment(): Fragment = UsedAddressFragment.newInstance(walletId)
}
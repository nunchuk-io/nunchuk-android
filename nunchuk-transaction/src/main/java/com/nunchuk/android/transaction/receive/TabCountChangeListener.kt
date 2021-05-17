package com.nunchuk.android.transaction.receive

import com.nunchuk.android.transaction.receive.address.AddressTab

interface TabCountChangeListener {
    fun onChange(tab: AddressTab, count: Int)
}
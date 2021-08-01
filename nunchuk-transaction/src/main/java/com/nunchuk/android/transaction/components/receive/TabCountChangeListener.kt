package com.nunchuk.android.transaction.components.receive

import com.nunchuk.android.transaction.components.receive.address.AddressTab

interface TabCountChangeListener {
    fun onChange(tab: AddressTab, count: Int)
}
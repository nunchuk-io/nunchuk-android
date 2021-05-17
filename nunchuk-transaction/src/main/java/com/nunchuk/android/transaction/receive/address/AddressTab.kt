package com.nunchuk.android.transaction.receive.address

private const val POSITION_UNUSED = 0
private const val POSITION_USED = 1

enum class AddressTab(val position: Int) {
    UNUSED(POSITION_UNUSED),
    USED(POSITION_USED),
}
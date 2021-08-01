package com.nunchuk.android.transaction.components.receive.address.used

import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel

sealed class UsedAddressEvent {
    data class GetUsedAddressErrorEvent(val message: String) : UsedAddressEvent()
}

data class UsedAddressState(val addresses: List<UsedAddressModel> = emptyList())
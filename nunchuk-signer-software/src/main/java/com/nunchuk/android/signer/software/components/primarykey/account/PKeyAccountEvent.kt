package com.nunchuk.android.signer.software.components.primarykey.account

import com.nunchuk.android.model.PrimaryKey

sealed class PKeyAccountEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyAccountEvent()
    data class ProcessErrorEvent(val message: String) : PKeyAccountEvent()
}

data class PKeyAccountState(
    val primaryKeys: List<PrimaryKey> = mutableListOf()
)
package com.nunchuk.android.core.nfc

import android.nfc.tech.NfcA
import xyz.twenty_two.CardStatus

data class PortalDeviceUiState(
    val isLoading: Boolean = false,
    val tag: NfcA? = null,
    val status: CardStatus? = null,
    val isConnected: Boolean = false,
    val event: PortalDeviceEvent? = null,
    val message: String = ""
)
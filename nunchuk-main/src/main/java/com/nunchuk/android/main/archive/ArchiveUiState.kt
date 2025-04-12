package com.nunchuk.android.main.archive

import com.nunchuk.android.model.WalletExtended

internal data class ArchiveUiState(
    val wallets: List<WalletExtended> = emptyList()
)
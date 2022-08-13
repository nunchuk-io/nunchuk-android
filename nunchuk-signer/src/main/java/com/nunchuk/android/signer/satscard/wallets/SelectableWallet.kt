package com.nunchuk.android.signer.satscard.wallets

import com.nunchuk.android.model.Wallet

data class SelectableWallet(val wallet: Wallet, val isShared: Boolean, val isSelected: Boolean = false)
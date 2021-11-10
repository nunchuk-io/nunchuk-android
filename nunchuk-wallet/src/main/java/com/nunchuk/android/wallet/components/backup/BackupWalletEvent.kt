package com.nunchuk.android.wallet.components.backup

sealed class BackupWalletEvent {
    data class Success(val filePath: String) : BackupWalletEvent()
    data class Failure(val message: String) : BackupWalletEvent()
}
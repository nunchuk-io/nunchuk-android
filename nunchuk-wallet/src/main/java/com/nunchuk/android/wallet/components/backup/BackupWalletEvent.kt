package com.nunchuk.android.wallet.components.backup

sealed class BackupWalletEvent {
    data class SetLoadingEvent(val showLoading: Boolean) : BackupWalletEvent()
    data class BackupDescriptorEvent(val walletId: String, val descriptor: String) : BackupWalletEvent()
    data class SkipBackupWalletEvent(val walletId: String) : BackupWalletEvent()
}
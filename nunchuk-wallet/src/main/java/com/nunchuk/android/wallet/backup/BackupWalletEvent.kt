package com.nunchuk.android.wallet.backup

sealed class BackupWalletEvent {
    data class SetLoadingEvent(val showLoading: Boolean) : BackupWalletEvent()
    data class BackupDescriptorEvent(val descriptor: String) : BackupWalletEvent()
    object SkipBackupWalletEvent : BackupWalletEvent()
}
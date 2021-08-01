package com.nunchuk.android.wallet.components.backup

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.BackupDescriptorEvent
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.SkipBackupWalletEvent
import javax.inject.Inject

internal class BackupWalletViewModel @Inject constructor() : NunchukViewModel<Unit, BackupWalletEvent>() {

    override val initialState = Unit

    private lateinit var walletId: String

    private lateinit var descriptor: String

    fun init(walletId: String, descriptor: String) {
        this.walletId = walletId
        this.descriptor = descriptor
    }

    fun handleBackupDescriptorEvent() {
        event(BackupDescriptorEvent(walletId, descriptor))
    }

    fun handleSkipBackupEvent() {
        event(SkipBackupWalletEvent(walletId))
    }

}
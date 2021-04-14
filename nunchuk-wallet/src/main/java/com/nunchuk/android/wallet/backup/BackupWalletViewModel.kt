package com.nunchuk.android.wallet.backup

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.wallet.backup.BackupWalletEvent.BackupDescriptorEvent
import com.nunchuk.android.wallet.backup.BackupWalletEvent.SkipBackupWalletEvent
import javax.inject.Inject

internal class BackupWalletViewModel @Inject constructor(

) : NunchukViewModel<Unit, BackupWalletEvent>() {

    override val initialState = Unit

    private lateinit var descriptor: String

    fun init(descriptor: String) {
        this.descriptor = descriptor
    }

    fun handleBackupDescriptorEvent() {
        event(BackupDescriptorEvent(descriptor))
    }

    fun handleSkipBackupEvent() {
        event(SkipBackupWalletEvent)
    }

}
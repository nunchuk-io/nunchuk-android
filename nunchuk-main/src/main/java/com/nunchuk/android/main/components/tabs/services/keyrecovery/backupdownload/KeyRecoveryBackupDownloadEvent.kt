package com.nunchuk.android.main.components.tabs.services.keyrecovery.backupdownload

import com.nunchuk.android.core.signer.SignerModel

sealed class BackupDownloadEvent {
    data class Loading(val isLoading: Boolean) : BackupDownloadEvent()
    data class ProcessFailure(val message: String) : BackupDownloadEvent()
    data class ImportTapsignerSuccess(val signer: SignerModel) : BackupDownloadEvent()
}

data class BackupDownloadState(
//    val password: String = "5246D9226D4D876B88FFCB94BEE10B3F", // demo
    val keyName: String = "",
    val error: String = "",
    val password: String = ""
)
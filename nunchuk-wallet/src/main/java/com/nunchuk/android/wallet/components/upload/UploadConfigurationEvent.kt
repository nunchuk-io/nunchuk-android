package com.nunchuk.android.wallet.components.upload

sealed class UploadConfigurationEvent {
    data class OpenDynamicQRScreen(val values: List<String>) : UploadConfigurationEvent()
    data class ExportColdcardSuccess(val filePath: String) : UploadConfigurationEvent()
    data class ExportColdcardFailure(val message: String) : UploadConfigurationEvent()
}

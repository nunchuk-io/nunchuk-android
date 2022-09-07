package com.nunchuk.android.wallet.components.upload

sealed class UploadConfigurationEvent {
    data class NfcLoading(val isLoading: Boolean) : UploadConfigurationEvent()
    data class OpenDynamicQRScreen(val values: List<String>) : UploadConfigurationEvent()
    data class ExportColdcardSuccess(val filePath: String? = null) : UploadConfigurationEvent()
    data class ShowError(val message: String) : UploadConfigurationEvent()
}

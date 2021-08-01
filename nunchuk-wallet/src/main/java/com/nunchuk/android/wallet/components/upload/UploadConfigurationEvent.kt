package com.nunchuk.android.wallet.components.upload

sealed class UploadConfigurationEvent {
    data class SetLoadingEvent(val showLoading: Boolean) : UploadConfigurationEvent()
    data class OpenDynamicQRScreen(val values: List<String>) : UploadConfigurationEvent()
    data class ExportWalletSuccessEvent(val filePath: String) : UploadConfigurationEvent()
    data class UploadConfigurationError(val message: String) : UploadConfigurationEvent()
}

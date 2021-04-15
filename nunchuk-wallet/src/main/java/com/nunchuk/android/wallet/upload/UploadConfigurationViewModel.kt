package com.nunchuk.android.wallet.upload

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.CreateWalletFilePathUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.wallet.upload.UploadConfigurationEvent.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class UploadConfigurationViewModel @Inject constructor(
    private val createWalletFilePathUseCase: CreateWalletFilePathUseCase,
    private val exportWalletUseCase: ExportWalletUseCase
) : NunchukViewModel<Unit, UploadConfigurationEvent>() {

    private lateinit var walletId: String

    override val initialState = Unit

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun handleUploadEvent() {
        event(SetLoadingEvent(true))
        viewModelScope.launch {
            when (val event = createWalletFilePathUseCase.execute(walletId)) {
                is Result.Success -> exportWallet(walletId, event.data)
                is Result.Error -> {
                    event(UploadConfigurationError(event.exception.message.orUnknownError()))
                    event(SetLoadingEvent(false))
                }
            }
        }
    }

    private fun exportWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath)) {
                is Result.Success -> {
                    event(ExportWalletSuccessEvent(filePath))
                    event(SetLoadingEvent(false))
                }
                is Result.Error -> {
                    event(UploadConfigurationError(event.exception.message.orUnknownError()))
                    event(SetLoadingEvent(false))
                }
            }
        }
    }

}
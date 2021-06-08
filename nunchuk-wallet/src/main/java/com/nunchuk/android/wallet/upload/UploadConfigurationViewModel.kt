package com.nunchuk.android.wallet.upload

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportCoboWalletUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.wallet.upload.UploadConfigurationEvent.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class UploadConfigurationViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportCoboWalletUseCase: ExportCoboWalletUseCase
) : NunchukViewModel<Unit, UploadConfigurationEvent>() {

    private lateinit var walletId: String

    override val initialState = Unit

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun handleUploadEvent() {
        event(SetLoadingEvent(true))
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute(walletId)) {
                is Success -> exportWallet(walletId, event.data)
                is Error -> {
                    event(UploadConfigurationError(event.exception.message.orUnknownError()))
                    event(SetLoadingEvent(false))
                }
            }
        }
    }

    fun handleShowQREvent() {
        event(SetLoadingEvent(true))
        viewModelScope.launch {
            when (val event = exportCoboWalletUseCase.execute(walletId)) {
                is Success -> {
                    event(OpenDynamicQRScreen(event.data))
                }
                is Error -> {
                    event(UploadConfigurationError(event.exception.message.orUnknownError()))
                    event(SetLoadingEvent(false))
                }
            }
        }
    }

    private fun exportWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath)) {
                is Success -> {
                    event(ExportWalletSuccessEvent(filePath))
                    event(SetLoadingEvent(false))
                }
                is Error -> {
                    event(UploadConfigurationError(event.exception.message.orUnknownError()))
                    event(SetLoadingEvent(false))
                }
            }
        }
    }

}
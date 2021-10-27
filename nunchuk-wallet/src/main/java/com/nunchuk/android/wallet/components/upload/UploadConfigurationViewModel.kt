package com.nunchuk.android.wallet.components.upload

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportKeystoneWalletUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class UploadConfigurationViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase
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
        viewModelScope.launch {
            viewModelScope.launch {
                exportKeystoneWalletUseCase.execute(walletId)
                    .onStart { event(SetLoadingEvent(true)) }
                    .flowOn(Dispatchers.IO)
                    .catch { event(UploadConfigurationError(it.message.orUnknownError())) }
                    .flowOn(Dispatchers.Main)
                    .collect { event(OpenDynamicQRScreen(it)) }
            }
        }
    }

    private fun exportWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.BSMS)) {
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
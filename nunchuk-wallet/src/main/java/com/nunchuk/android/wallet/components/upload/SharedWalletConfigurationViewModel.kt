package com.nunchuk.android.wallet.components.upload

import android.nfc.tech.Ndef
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.ExportWalletToMk4UseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportKeystoneWalletUseCase
import com.nunchuk.android.usecase.ExportPassportWalletUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedWalletConfigurationViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportWalletToMk4UseCase: ExportWalletToMk4UseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase,
    private val exportPassportWalletUseCase: ExportPassportWalletUseCase,
) : NunchukViewModel<Unit, UploadConfigurationEvent>() {

    private lateinit var walletId: String

    override val initialState = Unit

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun handleColdcardExportNfc(ndef: Ndef) {
        viewModelScope.launch {
            setEvent(NfcLoading(true))
            val result = exportWalletToMk4UseCase(ExportWalletToMk4UseCase.Data(walletId, ndef))
            setEvent(NfcLoading(false))
            if (result.isSuccess) {
                setEvent(ExportColdcardSuccess())
            } else {
                event(ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleColdcardExportToFile() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute(walletId + "_coldcard_export.txt")) {
                is Success -> exportWallet(walletId, event.data)
                is Error -> {
                    event(ShowError(event.exception.message.orUnknownError()))
                }
            }
        }
    }

    fun handleShowQREvent() {
        viewModelScope.launch {
            exportKeystoneWalletUseCase.execute(walletId)
                .flowOn(IO)
                .onException { event(ShowError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { event(OpenDynamicQRScreen(it)) }
        }
    }

    fun handleExportWalletQR() {
        viewModelScope.launch {
            exportKeystoneWalletUseCase.execute(walletId)
                .flowOn(IO)
                .onException { showError(it) }
                .flowOn(Main)
                .collect { event(OpenDynamicQRScreen(it)) }
        }
    }

    fun handleExportPassport() {
        viewModelScope.launch {
            exportPassportWalletUseCase.execute(walletId)
                .flowOn(IO)
                .onException { showError(it) }
                .flowOn(Main)
                .collect { event(OpenDynamicQRScreen(it)) }
        }
    }

    private fun exportWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.COLDCARD)) {
                is Success -> {
                    event(ExportColdcardSuccess(filePath))
                }
                is Error -> showError(event.exception)
            }
        }
    }

    private fun showError(t: Throwable?) {
        event(ShowError(t?.message.orUnknownError()))
    }
}
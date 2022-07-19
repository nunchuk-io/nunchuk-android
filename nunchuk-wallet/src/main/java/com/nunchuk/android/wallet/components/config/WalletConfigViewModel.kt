package com.nunchuk.android.wallet.components.config

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class WalletConfigViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase,
    private val exportPassportWalletUseCase: ExportPassportWalletUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
) : NunchukViewModel<WalletExtended, WalletConfigEvent>() {

    override val initialState = WalletExtended()

    lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { updateState { it } }
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        viewModelScope.launch {
            updateWalletUseCase.execute(getState().wallet.copy(name = walletName))
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState { copy(wallet = wallet.copy(name = walletName)) }
                    event(UpdateNameSuccessEvent)
                }
        }
    }


    fun handleExportWalletQR() {
        viewModelScope.launch {
            exportKeystoneWalletUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .onException { showError(it) }
                .flowOn(Dispatchers.Main)
                .collect { event(WalletConfigEvent.OpenDynamicQRScreen(it)) }
        }
    }

    fun handleExportPassport() {
        viewModelScope.launch {
            exportPassportWalletUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .onException { showError(it) }
                .flowOn(Dispatchers.Main)
                .collect { event(WalletConfigEvent.OpenDynamicQRScreen(it)) }
        }
    }

    private fun showError(t: Throwable) {
        event(WalletConfigEvent.WalletDetailsError(t.message.orUnknownError()))
    }

    fun handleDeleteWallet() {
        viewModelScope.launch {
            when (val event = deleteWalletUseCase.execute(walletId)) {
                is Result.Success -> event(WalletConfigEvent.DeleteWalletSuccess)
                is Result.Error -> showError(event)
            }
        }
    }

    fun handleExportColdcard() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute(walletId + "_coldcard_export.txt")) {
                is Result.Success -> exportWalletToFile(walletId, event.data, ExportFormat.COLDCARD)
                is Result.Error -> showError(event)
            }
        }
    }

    private fun exportWalletToFile(walletId: String, filePath: String, format: ExportFormat) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, format)) {
                is Result.Success -> event(WalletConfigEvent.UploadWalletConfigEvent(filePath))
                is Result.Error -> showError(event)
            }
        }
    }

    private fun showError(event: Result.Error) {
        WalletDetailsEvent.WalletDetailsError(event.exception.messageOrUnknownError())
    }
}
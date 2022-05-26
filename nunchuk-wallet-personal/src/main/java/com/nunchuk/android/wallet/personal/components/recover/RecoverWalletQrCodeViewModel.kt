package com.nunchuk.android.wallet.personal.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.usecase.ImportKeystoneWalletUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class RecoverWalletQrCodeViewModel @Inject constructor(
    private val importKeystoneWalletUseCase: ImportKeystoneWalletUseCase
) : NunchukViewModel<Unit, RecoverWalletQrCodeEvent>() {

    private var isProcessing = false

    private val qrDataList = HashSet<String>()

    override val initialState = Unit

    fun updateQRCode(qrData: String, description: String) {
        qrDataList.add(qrData)
        if (!isProcessing) {
            viewModelScope.launch {
                importKeystoneWalletUseCase.execute(description = description, qrData = qrDataList.toList())
                    .onStart { isProcessing = true }
                    .flowOn(IO)
                    .onException { }
                    .flowOn(Main)
                    .onCompletion { isProcessing = false }
                    .collect { event(RecoverWalletQrCodeEvent.ImportQRCodeSuccess(it.id)) }
            }
        }
    }

}
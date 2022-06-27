package com.nunchuk.android.wallet.components.backup

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Failure
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class BackupWalletViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
) : NunchukViewModel<Unit, BackupWalletEvent>() {

    override val initialState = Unit

    private lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun handleBackupDescriptorEvent() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute("$walletId.bsms")) {
                is Result.Success -> exportWallet(walletId, event.data)
                is Result.Error -> {
                    event(Failure(event.exception.message.orUnknownError()))
                }
            }
        }
    }

    private fun exportWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.BSMS)) {
                is Result.Success -> {
                    event(Success(filePath))
                }
                is Result.Error -> {
                    event(Failure(event.exception.message.orUnknownError()))
                }
            }
        }
    }

}
package com.nunchuk.android.signer.mk4.inheritance.backup.myself

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.utils.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ColdCardVerifyRecoveredKeyViewModel @Inject constructor(
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _event = MutableSharedFlow<ColdCardVerifyRecoveredKeyEvent>()
    val event = _event.asSharedFlow()

    fun setKeyVerified(groupId: String, masterSignerId: String) {
        viewModelScope.launch {
            val result =
                setKeyVerifiedUseCase(
                    SetKeyVerifiedUseCase.Param(
                        groupId,
                        masterSignerId,
                        false
                    )
                )
            if (result.isSuccess) {
                _event.emit(ColdCardVerifyRecoveredKeyEvent.OnExitSelfCheck)
            } else {
                _event.emit(ColdCardVerifyRecoveredKeyEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun setReplaceKeyVerified(
        keyId: String, filePath: String,
        groupId: String, walletId: String
    ) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = getChecksum(filePath),
                    isAppVerified = false,
                    groupId = groupId,
                    walletId = walletId
                )
            ).onSuccess {
                _event.emit(ColdCardVerifyRecoveredKeyEvent.OnExitSelfCheck)
            }.onFailure {
                _event.emit(ColdCardVerifyRecoveredKeyEvent.ShowError(it))
            }
        }
    }

    private suspend fun getChecksum(filePath: String): String = withContext(ioDispatcher) {
        ChecksumUtil.getChecksum(File(filePath).readBytes())
    }
}

sealed class ColdCardVerifyRecoveredKeyEvent {
    data object OnExitSelfCheck : ColdCardVerifyRecoveredKeyEvent()
    data class ShowError(val throwable: Throwable?) : ColdCardVerifyRecoveredKeyEvent()
}
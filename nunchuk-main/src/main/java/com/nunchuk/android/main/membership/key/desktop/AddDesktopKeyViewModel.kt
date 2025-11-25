package com.nunchuk.android.main.membership.key.desktop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.membership.DeletePendingRequestsByMagicUseCase
import com.nunchuk.android.usecase.membership.RequestAddDesktopKeyUseCase
import com.nunchuk.android.usecase.membership.RequestAddKeyForInheritanceUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDesktopKeyViewModel @Inject constructor(
    private val requestAddDesktopKeyUseCase: RequestAddDesktopKeyUseCase,
    private val requestAddKeyForInheritanceUseCase: RequestAddKeyForInheritanceUseCase,
    private val deletePendingRequestsByMagicUseCase: DeletePendingRequestsByMagicUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = AddDesktopKeyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<AddDesktopKeyEvent>()
    val event = _event.asSharedFlow()

    private var requestJob: Job? = null

    fun deleteClaimingRequests() {
        viewModelScope.launch {
            if (args.magic.isNotEmpty()) {
                deletePendingRequestsByMagicUseCase(
                    DeletePendingRequestsByMagicUseCase.Param(
                        args.magic,
                    )
                )
            }
        }
    }

    fun requestAddDesktopKey() {
        if (requestJob?.isActive != true) {
            if (args.magic.isNotEmpty()) {
                requestAddDestopClaimKey()
            } else {
                requestAddDestopKey()
            }
        }
    }

    private fun requestAddDestopClaimKey() {
        requestJob = viewModelScope.launch {
            requestAddKeyForInheritanceUseCase(
                RequestAddKeyForInheritanceUseCase.Param(
                    args.magic,
                )
            ).onSuccess {
                _event.emit(AddDesktopKeyEvent.RequestAddKeySuccess(it))
            }.onFailure {
                _event.emit(AddDesktopKeyEvent.RequestAddKeyFailed(it.message.orUnknownError()))
            }
        }
    }

    private fun requestAddDestopKey() {
        requestJob = viewModelScope.launch {
            val walletType = syncDraftWalletUseCase(args.groupId.orEmpty()).getOrNull()?.walletType
                ?: WalletType.MULTI_SIG
            requestAddDesktopKeyUseCase(
                RequestAddDesktopKeyUseCase.Param(
                    args.step,
                    args.groupId.orEmpty(),
                    if (args.isAddInheritanceKey) listOf(
                        SignerTag.INHERITANCE,
                        args.signerTag
                    ) else listOf(args.signerTag),
                    walletType
                )
            ).onSuccess {
                _event.emit(AddDesktopKeyEvent.RequestAddKeySuccess(it))
            }.onFailure {
                _event.emit(AddDesktopKeyEvent.RequestAddKeyFailed(it.message.orUnknownError()))
            }
        }
    }
}

sealed class AddDesktopKeyEvent {
    data class RequestAddKeySuccess(val requestId: String,) : AddDesktopKeyEvent()
    data class RequestAddKeyFailed(val message: String) : AddDesktopKeyEvent()
}
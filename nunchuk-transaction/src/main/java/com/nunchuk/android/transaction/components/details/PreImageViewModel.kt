package com.nunchuk.android.transaction.components.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.usecase.RevealPreimageUseCase
import com.nunchuk.android.utils.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreImageViewModel @Inject constructor(
    private val revealPreimageUseCase: RevealPreimageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PreImageState())
    val state = _state.asStateFlow()

    fun setInvalidPreimageLengthError() {
        _state.update {
            it.copy(
                error = "Invalid preimage length"
            )
        }
    }

    fun validateAndRevealPreimage(
        walletId: String,
        txId: String,
        node: ScriptNode,
        preimage: String
    ) {
        viewModelScope.launch {
            revealPreimageUseCase(
                RevealPreimageUseCase.Params(
                    walletId = walletId,
                    txId = txId,
                    hash = node.data,
                    preimage = ChecksumUtil.decodeHex(preimage.toCharArray())
                )
            ).onSuccess {
                _state.update { it.copy(isSuccess = true) }
            }.onFailure {
                _state.update {
                    it.copy(
                        error = "Invalid preimage"
                    )
                }
            }
        }
    }

    fun reset() {
        _state.update {
            PreImageState(
                isSuccess = false,
                error = null
            )
        }
    }
}

data class PreImageState(
    val isSuccess: Boolean = false,
    val error: String? = null
) 
package com.nunchuk.android.main.membership.replacekey

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.signer.SignerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReplaceKeysViewModel @Inject constructor(

) : ViewModel() {
    private val _uiState = MutableStateFlow(ReplaceKeysUiState())
    val uiState = _uiState.asStateFlow()


}

data class ReplaceKeysUiState(
    val signers: List<SignerModel> = emptyList(),
)
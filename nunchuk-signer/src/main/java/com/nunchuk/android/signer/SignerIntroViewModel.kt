package com.nunchuk.android.signer

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerStatusUseCase
import com.nunchuk.android.model.TapSignerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignerIntroViewModel @Inject constructor(
    private val getTapSignerStatusUseCase: GetTapSignerStatusUseCase
) : ViewModel() {
    private val _tapSignerStatus = MutableStateFlow<TapSignerStatus?>(null)
    val tapSignerStatus = _tapSignerStatus.filterIsInstance<TapSignerStatus>()

    fun getTapSignerStatus(isoDep: IsoDep) {
        viewModelScope.launch {
            val result = getTapSignerStatusUseCase(isoDep)
            if (result.isSuccess) {
                _tapSignerStatus.value = result.getOrNull()
            }

        }
    }

    fun clearTapSignerStatus() {
        _tapSignerStatus.value = null
    }
}
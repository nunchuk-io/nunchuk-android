package com.nunchuk.android.signer.tapsigner.backup.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.verify.model.TsBackUpOption
import com.nunchuk.android.signer.tapsigner.backup.verify.model.TsBackUpOptionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerVerifyBackUpOptionViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerVerifyBackUpOptionEvent>()
    val event = _event.asSharedFlow()

    private val _options = MutableStateFlow(
        listOf(
            TsBackUpOption(
                type = TsBackUpOptionType.BY_APP,
                labelId = R.string.nc_verify_backup_via_the_app
            ),
            TsBackUpOption(
                type = TsBackUpOptionType.BY_MYSELF,
                labelId = R.string.nc_verify_backup_myself
            ),
        )
    )
    val options = _options.asStateFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(OnContinueClicked)
        }
    }

    fun onOptionClicked(option: TsBackUpOption) {
        val options =
            _options.value.toList().map { it.copy(isSelected = it.labelId == option.labelId) }
        _options.value = options
    }

    val selectedOptionType: TsBackUpOptionType
        get() = _options.value.first { it.isSelected }.type
}

sealed class TapSignerVerifyBackUpOptionEvent

object OnContinueClicked : TapSignerVerifyBackUpOptionEvent()

package com.nunchuk.android.app.onboard.advisor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Country
import com.nunchuk.android.usecase.GetListCountryUseCase
import com.nunchuk.android.usecase.SendOnboardNoAdvisorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardAdvisorInputViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getListCountryUseCase: GetListCountryUseCase,
    private val sendOnboardNoAdvisorUseCase: SendOnboardNoAdvisorUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardAdvisorInputUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getListCountryUseCase(Unit).onSuccess { countries ->
                _state.update {  it.copy(countries = countries) }
            }
        }
    }

    fun onCountrySelected(country: Country) {
        _state.update { it.copy(selectedCountry = country) }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun onNoteChanged(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun onSendQuery() {
        val state = state.value
        viewModelScope.launch {
            sendOnboardNoAdvisorUseCase(
                SendOnboardNoAdvisorUseCase.Params(
                    email = state.email,
                    countryCode = state.selectedCountry?.code.orEmpty(),
                    note = state.note
                )
            ).onSuccess {
                _state.update { it.copy(sendQuerySuccess = Any()) }
            }.onFailure {
                _state.update { it.copy(errorMessage = it.errorMessage.orUnknownError()) }
            }
        }
    }

    fun onErrorMessageEventConsumed() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    fun onSendQuerySuccessEventConsumed() {
        _state.update { state -> state.copy(sendQuerySuccess = null) }
    }
}

data class OnboardAdvisorInputUiState(
    val country: String = "",
    val email: String = "",
    val note: String = "",
    val countries: List<Country> = emptyList(),
    val selectedCountry: Country? = null,
    val errorMessage: String? = null,
    val sendQuerySuccess: Any? = null
)
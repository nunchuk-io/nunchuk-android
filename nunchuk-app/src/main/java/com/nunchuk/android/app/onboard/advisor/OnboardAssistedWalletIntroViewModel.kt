package com.nunchuk.android.app.onboard.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.profile.SetOnBoardUseCase
import com.nunchuk.android.model.banner.BannerPage
import com.nunchuk.android.usecase.banner.GetAssistedWalletPageContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class OnboardAssistedWalletIntroViewModel @Inject constructor(
    private val getAssistedWalletPageContentUseCase: GetAssistedWalletPageContentUseCase,
    private val setOnBoardUseCase: SetOnBoardUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardAssistedWalletIntroUiState())
    val state = _state.asStateFlow()

    init {
        getBannerPage()
    }

    private fun getBannerPage() = viewModelScope.launch {
        getAssistedWalletPageContentUseCase("")
            .onSuccess { banner ->
                _state.update { it.copy(bannerPage = banner) }
            }
    }

    fun markOnboardDone() = viewModelScope.launch {
        runCatching {
            setOnBoardUseCase(false)
        }.onSuccess {
            _state.update { it.copy(openMainScreen = true) }
        }.onFailure {
            Timber.e(it)
        }
    }

    fun handledOpenMainScreen() {
        _state.update { it.copy(openMainScreen = false) }
    }
}

data class OnboardAssistedWalletIntroUiState(
    val bannerPage: BannerPage? = null,
    val openMainScreen: Boolean = false,
)
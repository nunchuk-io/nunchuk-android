package com.nunchuk.android.settings.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.settings.GetCustomExplorerUrlFlowUseCase
import com.nunchuk.android.usecase.settings.SetCustomExplorerUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CustomExplorerUiState(
    val defaultUrl: String = "https://mempool.space/tx/<tx_id>",
    val customUrl: String = "",
    val isCustomSelected: Boolean = false
)

@HiltViewModel
class CustomExplorerViewModel @Inject constructor(
    getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    getCustomExplorerUrlFlowUseCase: GetCustomExplorerUrlFlowUseCase,
    private val setCustomExplorerUrlUseCase: SetCustomExplorerUrlUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomExplorerUiState())
    val uiState: StateFlow<CustomExplorerUiState> = _uiState.asStateFlow()

    init {
        combine(
            getChainSettingFlowUseCase(Unit).map { it.getOrThrow() },
            getCustomExplorerUrlFlowUseCase(Unit).map { it.getOrThrow() }
        ) { chain, customUrl ->
            val defaultUrl = when (chain) {
                Chain.MAIN -> "https://mempool.space/tx/<tx_id>"
                Chain.TESTNET -> "https://mempool.space/testnet4/tx/<tx_id>"
                Chain.SIGNET -> "https://mempool.space/signet/tx/<tx_id>"
                else -> "https://mempool.space/tx/<tx_id>"
            }
            CustomExplorerUiState(
                defaultUrl = defaultUrl,
                customUrl = customUrl,
                isCustomSelected = customUrl.isNotBlank()
            )
        }.onEach {
            _uiState.value = it
        }.launchIn(viewModelScope)
    }

    fun saveCustomExplorer(url: String, custom: Boolean) {
        viewModelScope.launch {
            if (custom && url.isNotBlank()) {
                setCustomExplorerUrlUseCase(url)
            } else {
                setCustomExplorerUrlUseCase("")
            }.onSuccess {
                Timber.d("Custom explorer URL saved successfully: $url")
            }
        }
    }
} 
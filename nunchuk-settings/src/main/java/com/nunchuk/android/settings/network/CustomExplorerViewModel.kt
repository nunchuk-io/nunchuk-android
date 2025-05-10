package com.nunchuk.android.settings.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.settings.GetCustomExplorerUrlFlowUseCase
import com.nunchuk.android.core.domain.settings.SetCustomExplorerUrlUseCase
import com.nunchuk.android.type.Chain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CustomExplorerUiState(
    val defaultUrl: String = "https://mempool.space/tx/<tx_id>",
    val customUrl: String = "",
    val isCustomSelected: Boolean = false,
    val chain: Chain = Chain.MAIN
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
        getChainSettingFlowUseCase(Unit)
            .map { it.getOrThrow() }
            .onEach { chain ->
                val defaultUrl = when (chain) {
                    Chain.MAIN -> "https://mempool.space/tx/<tx_id>"
                    Chain.TESTNET -> "https://mempool.space/testnet4/tx/<tx_id>"
                    Chain.SIGNET -> "https://mempool.space/signet/tx/<tx_id>"
                    else -> "https://mempool.space/tx/<tx_id>"
                }
                val customUrl = getCustomExplorerUrlFlowUseCase(chain).getOrThrow()
                _uiState.update {
                    it.copy(
                        chain = chain,
                        defaultUrl = defaultUrl,
                        customUrl = customUrl,
                        isCustomSelected = customUrl.isNotBlank()
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun saveCustomExplorer(url: String, custom: Boolean) {
        viewModelScope.launch {
            if (custom && url.isNotBlank()) {
                setCustomExplorerUrlUseCase(
                    SetCustomExplorerUrlUseCase.Params(
                        chain = uiState.value.chain,
                        url = url
                    )
                ).onFailure {
                    Timber.e("Failed to save custom explorer URL: $url")
                }
            } else {
                setCustomExplorerUrlUseCase(
                    SetCustomExplorerUrlUseCase.Params(
                        chain = uiState.value.chain,
                        url = ""
                    )
                ).onFailure {
                    Timber.e("Failed to save custom explorer URL: $url")
                }
            }
        }
    }
} 
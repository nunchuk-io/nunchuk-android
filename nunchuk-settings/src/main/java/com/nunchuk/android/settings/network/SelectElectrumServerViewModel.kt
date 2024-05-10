package com.nunchuk.android.settings.network

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.AddLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.GetElectrumServersUseCase
import com.nunchuk.android.core.domain.GetLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.RemoveLocalElectrumServersUseCase
import com.nunchuk.android.model.ElectrumServer
import com.nunchuk.android.model.RemoteElectrumServer
import com.nunchuk.android.type.Chain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectElectrumServerViewModel @Inject constructor(
    private val getElectrumServersUseCase: GetElectrumServersUseCase,
    private val getLocalElectrumServersUseCase: GetLocalElectrumServersUseCase,
    private val addLocalElectrumServersUseCase: AddLocalElectrumServersUseCase,
    private val removeLocalElectrumServersUseCase: RemoveLocalElectrumServersUseCase,
    saveStateHandle: SavedStateHandle
) : ViewModel() {
    private val chain = saveStateHandle.get<Chain>(SelectElectrumServerActivity.EXTRA_CHAIN) ?: Chain.MAIN
    private val server = saveStateHandle.get<String>(SelectElectrumServerActivity.EXTRA_SERVER).orEmpty()
    private val _uiState = MutableStateFlow(SelectElectrumServerUiState(chain = chain, server = server))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getElectrumServersUseCase(Unit).onSuccess { electrumServers ->
                val servers = when (chain) {
                    Chain.MAIN -> electrumServers.mainnet
                    Chain.TESTNET -> electrumServers.testnet
                    else -> electrumServers.signet
                }
                _uiState.update { it.copy(remoteServers = servers) }
            }
        }
        viewModelScope.launch {
            getLocalElectrumServersUseCase(Unit)
                .map { it.getOrThrow() }
                .collect { localServers ->
                    _uiState.update { it.copy(localElectrumServers = localServers) }
                }
        }
    }

    fun onAddNewServer(server: String) {
        viewModelScope.launch {
            addLocalElectrumServersUseCase(ElectrumServer(url = server, chain = chain))
        }
    }

    fun onRemove(id: Long) {
        _uiState.update { it.copy(pendingRemoveIds = it.pendingRemoveIds + id) }
    }

    fun onSave() {
        viewModelScope.launch {
            removeLocalElectrumServersUseCase(_uiState.value.pendingRemoveIds.toList())
            _uiState.update { it.copy(pendingRemoveIds = emptySet()) }
        }
    }
}

data class SelectElectrumServerUiState(
    val server: String = "",
    val remoteServers: List<RemoteElectrumServer> = emptyList(),
    val localElectrumServers: List<ElectrumServer> = emptyList(),
    val chain: Chain = Chain.MAIN,
    val pendingRemoveIds: Set<Long> = emptySet()
)
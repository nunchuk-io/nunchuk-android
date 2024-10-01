package com.nunchuk.android.settings.network

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.AddLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetElectrumServersUseCase
import com.nunchuk.android.core.domain.GetLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.RemoveLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.model.ElectrumServer
import com.nunchuk.android.model.RemoteElectrumServer
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.type.Chain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val appScope: CoroutineScope,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val sendSignOutUseCase: SendSignOutUseCase,
    saveStateHandle: SavedStateHandle
) : ViewModel() {
    private val chain =
        saveStateHandle.get<Chain>(SelectElectrumServerActivity.EXTRA_CHAIN) ?: Chain.MAIN
    private val server =
        saveStateHandle.get<String>(SelectElectrumServerActivity.EXTRA_SERVER).orEmpty()
    private val _uiState =
        MutableStateFlow(SelectElectrumServerUiState(chain = chain, server = server))
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
            val isLocalExist = _uiState.value.localElectrumServers.any { it.url == server }
            val isRemoteExist = _uiState.value.remoteServers.any { it.url == server }
            if (!isLocalExist && !isRemoteExist) {
                addLocalElectrumServersUseCase(ElectrumServer(url = server, chain = chain))
                val currentSettings = getAppSettingUseCase(Unit).getOrThrow()
                updateAppSettingUseCase(currentSettings.copy(mainnetServers = listOf(server), chain = Chain.MAIN))
                _uiState.update { it.copy(addSuccessEvent = StateEvent.String(server)) }
            }
        }
    }

    fun onRemove(id: Long) {
        _uiState.update { it.copy(pendingRemoveIds = it.pendingRemoveIds + id) }
    }

    fun onSave() {
        viewModelScope.launch {
            val selectedId =
                _uiState.value.localElectrumServers.firstOrNull { it.url == _uiState.value.server }?.id
            val removeIds = _uiState.value.pendingRemoveIds
            removeLocalElectrumServersUseCase(removeIds.toList())
            if (selectedId in removeIds) {
                _uiState.update {
                    it.copy(
                        autoSelectServer = StateEvent.Unit,
                        server = _uiState.value.remoteServers.firstOrNull()?.url.orEmpty()
                    )
                }
            }
            _uiState.update { it.copy(pendingRemoveIds = emptySet()) }
        }
    }

    fun onHandleAddSuccessEvent() {
        _uiState.update { it.copy(addSuccessEvent = StateEvent.None) }
    }

    fun onHandleAutoSelectServer() {
        _uiState.update { it.copy(autoSelectServer = StateEvent.None) }
    }

    fun signOut() {
        appScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            clearInfoSessionUseCase(Unit)
            sendSignOutUseCase(Unit)
            _uiState.update { it.copy(logoutEvent = StateEvent.Unit, isLoading = false) }
        }
    }

    fun onHandleLogoutEvent() {
        _uiState.update { it.copy(logoutEvent = StateEvent.None) }
    }
}

data class SelectElectrumServerUiState(
    val isLoading: Boolean = false,
    val server: String = "",
    val remoteServers: List<RemoteElectrumServer> = emptyList(),
    val localElectrumServers: List<ElectrumServer> = emptyList(),
    val chain: Chain = Chain.MAIN,
    val pendingRemoveIds: Set<Long> = emptySet(),
    val addSuccessEvent: StateEvent = StateEvent.None,
    val autoSelectServer: StateEvent = StateEvent.None,
    val logoutEvent: StateEvent = StateEvent.None,
)
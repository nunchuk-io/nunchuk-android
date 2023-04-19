package com.nunchuk.android.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.usecase.coin.SyncCoinControlData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncInfoViewModel @Inject constructor(
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val syncCoinControlData: SyncCoinControlData,
) : ViewModel() {
    private val assistedWallets = getAssistedWalletsFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                delay(300_000L)
                syncCoin()
            }
        }
    }

    fun init() {
        viewModelScope.launch {
            delay(1000L)
            syncCoin()
        }
    }

    private fun syncCoin() {
        viewModelScope.launch {
            assistedWallets.value.forEach {
                syncCoinControlData(it.localId)
            }
        }
    }
}
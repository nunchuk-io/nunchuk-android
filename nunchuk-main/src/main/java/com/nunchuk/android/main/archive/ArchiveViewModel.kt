package com.nunchuk.android.main.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.GetAllWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ArchiveViewModel @Inject constructor(
    private val getAllWalletsUseCase: GetAllWalletsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ArchiveUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllWalletsUseCase(Unit)
                .onSuccess { wallets ->
                    _state.update {
                        it.copy(
                            wallets = wallets.filter { it.wallet.archived }
                        )
                    }
                }
        }
    }
}
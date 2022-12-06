package com.nunchuk.android.main.membership.authentication.dummytx

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class DummyTransactionDetailsViewModel @Inject constructor(
) : ViewModel() {
    private val _state = MutableStateFlow(DummyTransactionState())
    val state = _state.asStateFlow()

    fun handleViewMoreEvent() {
        _state.update { it.copy(viewMore = it.viewMore.not()) }
    }
}
package com.nunchuk.android.core.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.darkmode.GetDarkModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    getDarkModeUseCase: GetDarkModeUseCase
) : ViewModel() {
    val mode = getDarkModeUseCase(Unit)
        .map { it.getOrThrow() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
}
package com.nunchuk.android.utils

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

inline fun <VM : ViewModel> viewModelProviderFactoryOf(
    crossinline f: (handle: SavedStateHandle) -> VM
): AbstractSavedStateViewModelFactory = object : AbstractSavedStateViewModelFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = f(handle) as T
}
